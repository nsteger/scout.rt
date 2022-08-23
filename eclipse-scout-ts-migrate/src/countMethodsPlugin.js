import jscodeshift from 'jscodeshift';
import {findParentClassBody} from './common.js';

const j = jscodeshift.withParser('ts');
let total = 0;
/**
 * @type import('ts-migrate-server').Plugin<unknown>
 */
const countMethods = {
  name: 'count-methods-plugin',

  async run({text, fileName}) {
    const root = j(text);
    let count = 0;
    let result = root.find(j.Declaration)
      .filter(path =>  path.node.type === j.ClassMethod.name ||
        (!findParentClassBody(path) && path.node.type === 'ExportNamedDeclaration' && path.node?.declaration?.type === 'FunctionDeclaration'))
      .forEach(expression => {
        count++;
      }).toSource();
    total+=count;
    console.log(fileName + ': ' + count);
    console.log('Total: '+ total);
    return result;
  },
};

export default countMethods;
