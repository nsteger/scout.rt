import jscodeshift from 'jscodeshift';
import {hasDefaultImportSpecifier, hasImportDeclaration} from '@codeshift/utils';
import {validateAnyAliasOptions} from 'ts-migrate-plugins/build/src/utils/validateOptions.js';
import {isDiagnosticWithLinePosition} from 'ts-migrate-plugins/build/src/utils/type-guards.js';

/**
 * Based on https://github.com/airbnb/ts-migrate/blob/master/packages/ts-migrate-plugins/src/plugins/declare-missing-class-properties.ts
 * See readme.md for details.
 */

const j = jscodeshift.withParser('ts');
let root;
let referencedTypes = new Set();

/**
 * @type import('ts-migrate-server').Plugin<{ anyAlias?: string }>
 */
const declareMissingClassPropertiesPlugin = {
  name: 'declare-missing-class-properties',

  async run({text, fileName, getLanguageService, options}) {
    const diagnostics = getLanguageService()
      .getSemanticDiagnostics(fileName)
      .filter(isDiagnosticWithLinePosition)
      .filter(diagnostic => diagnostic.code === 2339 || diagnostic.code === 2551);

    root = j(text);

    const toAdd = [];

    diagnostics.forEach(diagnostic => {
      root
        .find(j.Identifier)
        .filter(
          path =>
            (path.node).start === diagnostic.start &&
            (path.node).end === diagnostic.start + diagnostic.length &&
            path.parentPath.node.type === 'MemberExpression' &&
            path.parentPath.node.object.type === 'ThisExpression' &&
            path.parentPath.parentPath.node.type === 'AssignmentExpression' && path.parentPath.parentPath.node.operator === '=' &&
            inConstructor(path)
        )
        .forEach(path => {
          const classBody = findParentClassBody(path);
          if (classBody) {
            let item = toAdd.find(cur => cur.classBody === classBody);
            if (!item) {
              item = {classBody, propertyNames: new Map()};
              toAdd.push(item);
            }

            let assignment = findParentPath(path, parentPath => parentPath.node.type === 'AssignmentExpression');
            let type = getTypeFor(path.node.name, assignment.node.right);
            if (type && type.type === 'TSTypeReference') {
              referencedTypes.add(type);
            }
            item.propertyNames.set(path.node.name, type);
          }
        });
    });

    toAdd.forEach(({classBody, propertyNames: propertyNameMap}) => {
      const propertyNames = Array.from(propertyNameMap.keys())
        .filter(propertyName => {
          const existingProperty = classBody.node.body.find(
            n =>
              n.type === 'ClassProperty' &&
              n.key.type === 'Identifier' &&
              n.key.name === propertyName
          );
          return existingProperty == null;
        })
        .sort(); // TODO ts change first public, than $, than protected

      let index = -1;
      for (let i = 0; i < classBody.node.body.length; i += 1) {
        const node = classBody.node.body[i];
        if (node.type === 'ClassProperty' && node.static) {
          index = i;
        }
      }

      classBody.node.body.splice(
        index + 1,
        0,
        ...propertyNames.map(propertyName =>
          j.classProperty(
            j.identifier(propertyName),
            null,
            j.tsTypeAnnotation(propertyNameMap.get(propertyName))
          )
        )
      );
    });

    for (const type of referencedTypes) {
      // if (type.typeName.name === 'JQuery' && !hasDefaultImportSpecifier(j, root, 'jquery')) {
      //   addDefaultImport('$', 'jquery');
      // }
      // TODO ts
    }

    return root.toSource();
  },

  validate: validateAnyAliasOptions
};

export default declareMissingClassPropertiesPlugin;

function inConstructor(path) {
  return !!findParentPath(path, parentPath => parentPath.node.type === 'ClassMethod' && parentPath.node.kind === 'constructor');
}

function findParentClassBody(path) {
  return findParentPath(path, parentPath => parentPath.node.type === 'ClassBody');
}

function findParentPath(path, predicate) {
  let cur = path;
  while (cur.node.type !== 'Program') {
    if (predicate(cur)) {
      return cur;
    }
    cur = cur.parentPath;
  }
  return undefined;
}

function getTypeFor(name, value) {
  if (name && (name.startsWith('$') || name.startsWith('_$'))) {
    return j.tsTypeReference(j.identifier('JQuery'));
  }
  switch (value.type) {
  case 'StringLiteral':
    return j.tsStringKeyword();
  case 'BooleanLiteral':
    return j.tsBooleanKeyword();
  case 'NumberLiteral':
    return j.tsNumberKeyword();
  case 'DateLi':
    return j.tsNumberKeyword();
  case 'ArrayExpression':
    let elementType = j.tsAnyKeyword();

    if (value.elements.length > 0) {
      elementType = getTypeFor(null, value.elements[0]);
    }
    return j.tsArrayType(elementType);
  default:
    console.log(`unknown type ${value.type} for name ${name}`);
    return j.tsAnyKeyword();
  }
}

function addDefaultImport(importName, moduleName) {
  const newImport = j.importDeclaration(
    [j.importDefaultSpecifier(j.identifier(importName))],
    j.stringLiteral(moduleName)
  );

  // Insert it at the top of the file
  root.get().node.program.body.unshift(newImport);
}
