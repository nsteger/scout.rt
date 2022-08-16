const emptyLineMarker = '/* -- ts-migrate empty line -- */';

/**
 * @type import('ts-migrate-server').Plugin<unknown>
 */
const encodeEmptyLinesPlugin = {
  name: 'encode-empty-lines',

  run({ text }) {

    // declareMissingClassPropertiesPlugin needs \r\n to work -> ensure crlf line breaks. The next regex also depends on that.
    text = text.replace(/(?!\r)\n/gm, '\r\n');

    // TypeScript AST will remove empty lines but not comments -> mark empty lines with a comment to preserve them
    // text = text.replace(/^\r\n/gm, emptyLineMarker + '\r\n')
    return text;
  },
};

export {emptyLineMarker};
export default encodeEmptyLinesPlugin;
