import pondermatic from '../dist/cjs/index.js'

// pondermatic.logLevel("info")
// pondermatic.portal()
pondermatic.addTap()

let engine
beforeEach(() => {
  engine = pondermatic.createEngine('tests', true)
})

afterEach(done => {
  pondermatic.stop(engine);
  setTimeout(done, 10);
});

afterAll(() => {
  pondermatic.stop(engine)
})

test('adding data', done => {
  const q = pondermatic.q(
    engine,
    "[:find ?v . :where [?id :data/key ?v]]",
    [],
    r => {
      if (!r || !r.length) {
        return
      }
      try {
        expect(r).toBe("value")
        done()
      } catch (e) {
        done(e)
      } finally {
        // pondermatic.dispose(q)
      }
    }
  )
  const tx = pondermatic.dataset([{ "key": "value" }])
  pondermatic.sh(engine, { "->db": tx })
});

test('production rule json', done => {
  const rules = pondermatic.ruleset([
    {
      "id": "test/set-value",
      "rule/when": {
        "data/deep": {
          "data/nested": "?value"
        }
      },
      "rule/then": {
        "data/new-key": "?value"
      }
    }
  ])
  const q = pondermatic.q(
    engine,
    "[:find ?v . :where [?id :data/new-key ?v]]",
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

test('production rule edn', done => {
  const rules = pondermatic.ruleset([
    {
      "id": "test/set-value",
      "rule/when": "{data/deep {data/nested ?value}}",
      "rule/then": {
        "data/new-key": "?value"
      }
    }
  ])
  const q = pondermatic.q(
    engine,
    "[:find ?v . :where [?id :data/new-key ?v]]",
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
