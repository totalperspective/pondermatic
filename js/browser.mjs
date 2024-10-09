import pFn from '../dist/browser/index.js'

const p = typeof pFn === 'function' ? pFn() : pFn

export default p
