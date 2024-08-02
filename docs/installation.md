# Installation

## Prerequisites

Before you begin, ensure you have the following installed:

- Java 11 or higher
- Node.js 16 or higher
- Clojure CLI tools

## Clojure/ClojureScript

Add the following dependency to your `deps.edn` file:

```clojure
{:deps {tech.totalperspective/pondermatic {:mvn/version "1.11.10"}}}
```

## JavaScript

Install via npm:

```sh
npm install @totalperspective/pondermatic
```

## Building the Project

To build the project, run the following commands:

```sh
clojure -T:build clean
clojure -T:build jar
clojure -T:build install
```

## Running Tests

To run the tests, use the following commands:

```sh
./ci/run_tests_all.sh
```

## Usage

### Clojure/ClojureScript

```clojure
(ns example.core
  (:require [pondermatic.core :as p]))

(def rules
  (p/ruleset
   [{:id :example-rule
     :rule/when {:data/key ?value}
     :rule/then {:data/new-key ?value}}]))

(def data
  (p/dataset
   [{:key "value"}]))

(def engine (p/->engine "example" :reset-db? true))

(-> engine
    (p/|> {:->db rules})
    (p/|> {:->db data})
    p/stop)
```

### JavaScript

```javascript
import pondermatic from '@totalperspective/pondermatic';

const engine = pondermatic.createEngine('example', true);

const rules = pondermatic.ruleset([
  {
    id: 'example-rule',
    'rule/when': { 'data/key': '?value' },
    'rule/then': { 'data/new-key': '?value' }
  }
]);

const data = pondermatic.dataset([{ key: 'value' }]);

pondermatic.sh(engine, { '->db': rules });
pondermatic.sh(engine, { '->db': data });
pondermatic.stop(engine);
```

