const pondermatic = require('../dist/cjs/index')

// pondermatic.portal("vs-code")

let engine
beforeEach(() => {
  engine = pondermatic.createEngine('tests', true)
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


test('production rule data', done => {
  const rules = pondermatic.ruleset([
    {
      "id": "test/set-value",
      "rule/when": {
        "data/deep": {
          "data/nested": "?value"
        }
      },
      "rule/then": {
        ":db/ident": "id",
        "data/new-key": "?value"
      }
    }
  ])
  const q = pondermatic.q(
    engine,
    "[:find ?v . :where [?id :db/ident \"id\"] [?id :data/new-key ?v]]",
    [],
    r => {
      if (!r) {
        return
      }
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
  const tx = pondermatic.dataset([{
    "deep": {
      "nested": "value"
    }
  }])
  pondermatic.sh(engine, { "->db": rules })
  pondermatic.sh(engine, { "->db": tx })
});
