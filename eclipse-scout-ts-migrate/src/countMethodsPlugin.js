import jscodeshift from 'jscodeshift';
import {methodFilter} from './common.js';

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
      .filter(path => methodFilter(j, path))
      .forEach(expression => {
        if (expression.node.key && expression.node.key.name.startsWith('set')) {
          console.log(expression.node.key.name);
        }
        count++;
      }).toSource();
    total += count;
    console.log(fileName + ': ' + count);
    console.log('Total: ' + total);
    return result;
  }
};

export default countMethods;
