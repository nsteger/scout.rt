export function lfToCrlf(text) {
  return text.replace(/(?!\r)\n/gm, '\r\n');
}

export function crlfToLf(text) {
  return text.replace(/\r\n/gm, '\n');
}
