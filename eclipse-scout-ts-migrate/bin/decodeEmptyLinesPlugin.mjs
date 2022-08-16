import {emptyLineMarker} from "./encodeEmptyLinesPlugin.mjs";

/**
 * @type import('ts-migrate-server').Plugin<unknown>
 */
const decodeEmptyLinesPlugin = {
  name: 'decode-empty-lines',

  run({ text }) {
    // text = text.replaceAll(emptyLineMarker, '')

    // revert line ending style to lf
    return text.replace(/\r\n/gm, '\n');
  },
};

export default decodeEmptyLinesPlugin;
