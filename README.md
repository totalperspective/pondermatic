# Pondermatic
Functional effect system-driven rules engine for Clojure/ClojureScript & Javascript

[![Clojars Project](https://img.shields.io/clojars/v/tech.totalperspective/pondermatic.svg)](https://clojars.org/tech.totalperspective/pondermatic) [![npm version](https://badge.fury.io/js/@totalperspective%2Fpondermatic.svg)](https://badge.fury.io/js/@totalperspective%2Fpondermatic)

[![JVM](https://github.com/totalperspective/pondermatic/actions/workflows/tests_clj.yml/badge.svg)](https://github.com/totalperspective/pondermatic/actions/workflows/tests_clj.yml) [![CLLS Browser](https://github.com/totalperspective/pondermatic/actions/workflows/tests_browser.yml/badge.svg)](https://github.com/totalperspective/pondermatic/actions/workflows/tests_browser.yml) [![CLJS NodeJS](https://github.com/totalperspective/pondermatic/actions/workflows/tests_node.yml/badge.svg)](https://github.com/totalperspective/pondermatic/actions/workflows/tests_node.yml) [![NodeJS](https://github.com/totalperspective/pondermatic/actions/workflows/tests_js.yml/badge.svg)](https://github.com/totalperspective/pondermatic/actions/workflows/tests_js.yml)

## Overview

Pondermatic is a functional effect system-driven rules engine designed for use with Clojure, ClojureScript, and JavaScript. It provides a robust framework for defining and executing rules in a declarative manner.

## Features

- Functional effect system
- Supports Clojure, ClojureScript, and JavaScript
- Declarative rule definitions
- Integration with various data sources

## Installation

### Clojure/ClojureScript

Add the following dependency to your `deps.edn` file:

```clojure
{:deps {tech.totalperspective/pondermatic {:mvn/version "1.11.10"}}}
```

### JavaScript

Install via npm:

```sh
npm install @totalperspective/pondermatic
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

## Contributing

Contributions are welcome! Please read the [contributing guidelines](CONTRIBUTING.md) for more information.

## License

This project is licensed under the Eclipse Public License - v 2.0. See the [LICENSE](LICENSE) file for details.