import pFn from '../dist/browser/index.js'

const p = typeof pFn === 'function' ? pFn() : pFn

if (globalThis.window) {
  globalThis.window.pondermatic = p
  window.devtoolsFormatters = window.devtoolsFormatters || []
  window.devtoolsFormatters.push(p.devtoolsFormatter)
}

export default p
