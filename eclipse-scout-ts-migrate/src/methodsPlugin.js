import jscodeshift from 'jscodeshift';
import {defaultParamTypeMap, defaultReturnTypeMap, createType, methodFilter} from './common.js';

const j = jscodeshift.withParser('ts');


/**
 * @type import('ts-migrate-server').Plugin<{paramTypeMap?: object, returnTypeMap?: object}>
 */
const methodsPlugin = {
  name: 'methods-plugin',

  async run({text, options}) {
    const root = j(text);
    const paramTypeMap = {...defaultParamTypeMap, ...options.paramTypeMap};
    const returnTypeMap = {...defaultReturnTypeMap, ...options.returnTypeMap};
    return root.find(j.Declaration)
      .filter(path => methodFilter(j, path))
      .forEach(expression => {
        let node = expression.node;
        if (node.params) {
          for (let param of node.params) {
            processParamType(param, Object.values(paramTypeMap));
          }
        }
        processReturnType(node, Object.values(returnTypeMap));

      }).toSource();
  }
};

function processReturnType(func, typeMaps) {
  let name = func.key ? func.key.name : func.id.name;
  if (func.returnType) {
    return;
  }
  let type = createType(typeMaps, name);
  if (type) {
    func.returnType = type;
  }
}

function processParamType(param, typeMaps) {
  let name = param.name;
  if (param.typeAnnotation) {
    return;
  }
  let type = createType(j, typeMaps, name);
  if (type) {
    param.typeAnnotation = type;
  }
}

export default methodsPlugin;
