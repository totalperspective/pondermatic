const pondermatic = require('../dist/cjs/index')

// pondermatic.portal("vs-code")

let engine
beforeEach(() => {
  engine = pondermatic.createEngine('tests')
});

test('adding data', done => {
  const q = pondermatic.q(
    engine,
    "[:find ?v . :where [?id :data/key ?v]]",
    [],
    r => {
      try {
        expect(r).toBe("value")
        done()
      } catch (e) {
        done(e)
      } finally {
        pondermatic.dispose(q)
      }
    }
  )
  const tx = pondermatic.dataset([{ "key": "value" }])
  pondermatic.sh(engine, { "->db": tx })
});
