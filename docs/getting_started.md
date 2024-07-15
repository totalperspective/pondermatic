# Getting Started

Welcome to the Pondermatic documentation! This guide will help you get started with Pondermatic, a functional effect system-driven rules engine designed for use with Clojure, ClojureScript, and JavaScript.

## Prerequisites

Before you begin, ensure you have the following installed:

- [Java 11 or higher](https://adoptopenjdk.net/)
- [Node.js 16 or higher](https://nodejs.org/)
- [Clojure CLI tools](https://clojure.org/guides/getting_started)

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

Create a new namespace and require Pondermatic:

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

Import Pondermatic and create an engine:

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

## Examples

For more detailed examples and usage, refer to the [Examples](examples.md) and [API Reference](api_reference.md) documentation.

## Contributing

Contributions are welcome! Please read the [contributing guidelines](CONTRIBUTING.md) for more information.

## License

This project is licensed under the Eclipse Public License - v 2.0. See the [LICENSE](LICENSE) file for details.