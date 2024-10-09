import pFn from '../dist/browser/index.js'

const p = typeof pFn === 'function' ? pFn() : pFn
p.devtoolsFormatter.id = '__pondermatic__'

if (globalThis.window) {
  globalThis.window.pondermatic = p
  window.devtoolsFormatters = window.devtoolsFormatters || []
  if (!window.devtoolsFormatters.some(f => f.id === p.devtoolsFormatter.id)) {
    window.devtoolsFormatters.push(p.devtoolsFormatter)
  }
}

export default p
