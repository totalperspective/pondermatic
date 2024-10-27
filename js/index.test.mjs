import pondermatic from './import.mjs'

// pondermatic.logLevel("info")
// pondermatic.portal()
pondermatic.addTap()

let engine
let engineIndex = 0

beforeEach(() => {
  ++engineIndex
  engine = pondermatic.createEngine('tests', true)
  pondermatic.basisT(engine, r => {
    console.log(`[${engineIndex}] basisT`, r)
  })
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
  const r = pondermatic.q$(engine, "[:find ?v . :where [?id :data/new-key ?v]]")
  expect(r).toBe("value")
});

test('callback', done => {
  const data = pondermatic.dataset([{ "id": "test", "key": "value" }])
  pondermatic.sh(engine, {
    "->db": data,
    "cb": r => {
      console.log('callback', r)
      expect(r.tempids).toHaveProperty("test")
      done()
    }
  })
})