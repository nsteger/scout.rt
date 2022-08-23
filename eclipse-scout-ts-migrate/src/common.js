export function lfToCrlf(text) {
  return text.replace(/(?!\r)\n/gm, '\r\n');
}

export function crlfToLf(text) {
  return text.replace(/\r\n/gm, '\n');
}

export function inConstructor(path) {
  return !!findParentPath(path, parentPath => parentPath.node.type === 'ClassMethod' && parentPath.node.kind === 'constructor');
}

export function findParentClassBody(path) {
  return findParentPath(path, parentPath => parentPath.node.type === 'ClassBody');
}

export function findParentPath(path, predicate) {
  let cur = path;
  while (cur.node.type !== 'Program') {
    if (predicate(cur)) {
      return cur;
    }
    cur = cur.parentPath;
  }
  return undefined;
}

export function getTypeFor(name, value) {
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

export function addDefaultImport(root, importName, moduleName) {
  const newImport = j.importDeclaration(
    [j.importDefaultSpecifier(j.identifier(importName))],
    j.stringLiteral(moduleName)
  );

  // Insert it at the top of the file
  root.get().node.program.body.unshift(newImport);
}
