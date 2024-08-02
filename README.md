# Pondermatic
Functional effect system-driven rules engine for Clojure/ClojureScript & Javascript

[![Clojars Project](https://img.shields.io/clojars/v/tech.totalperspective/pondermatic.svg)](https://clojars.org/tech.totalperspective/pondermatic) [![npm version](https://badge.fury.io/js/@totalperspective%2Fpondermatic.svg)](https://badge.fury.io/js/@totalperspective%2Fpondermatic)

[![JVM](https://github.com/totalperspective/pondermatic/actions/workflows/tests_clj.yml/badge.svg)](https://github.com/totalperspective/pondermatic/actions/workflows/tests_clj.yml) [![CLLS Browser](https://github.com/totalperspective/pondermatic/actions/workflows/tests_browser.yml/badge.svg)](https://github.com/totalperspective/pondermatic/actions/workflows/tests_browser.yml) [![CLJS NodeJS](https://github.com/totalperspective/pondermatic/actions/workflows/tests_node.yml/badge.svg)](https://github.com/totalperspective/pondermatic/actions/workflows/tests_node.yml) [![NodeJS](https://github.com/totalperspective/pondermatic/actions/workflows/tests_js.yml/badge.svg)](https://github.com/totalperspective/pondermatic/actions/workflows/tests_js.yml)

## Overview

Pondermatic is a functional effect system-driven rules engine designed for use with Clojure, ClojureScript, and JavaScript. It provides a robust framework for defining and executing rules in a declarative manner. Pondermatic helps manage side effects in a predictable and composable way, making it easier to integrate with various data sources and build complex rule-based systems.

## Features

- Functional effect system for managing side effects in a predictable and composable way
- Seamless integration with Clojure, ClojureScript, and JavaScript projects
- Declarative rule definitions for clear and concise rule management
- Integration with various data sources for flexible data handling

## Architecture
Pondermatic's architecture consists of the following key components:
- Ruleset: Defines the rules to be executed.
- Dataset: Contains the data to be processed by the rules.
- Engine: Executes the rules on the dataset and manages the state.

## Documentation

- [Introduction](docs/introduction.md)
- [Getting Started](docs/getting_started.md)
- [Installation](docs/installation.md)
- [Examples](docs/examples.md)
- [API Reference](docs/api_reference.md)
- [API Reference (JavaScript)](docs/api_reference_js.md)
- [Features](docs/features.md)
- [Rules](docs/rules/overview.md)
  - [Syntax](docs/rules/syntax.md)

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

// Create a new engine instance
const engine = pondermatic.createEngine('example', true);

// Define a ruleset
const rules = pondermatic.ruleset([
  {
    id: 'example-rule',
    'rule/when': { 'data/key': '?value' },
    'rule/then': { 'data/new-key': '?value' }
  }
]);

// Define a dataset
const data = pondermatic.dataset([{ key: 'value' }]);

// Load the ruleset into the engine
pondermatic.sh(engine, { '->db': rules });

// Load the dataset into the engine
pondermatic.sh(engine, { '->db': data });

// Stop the engine
pondermatic.stop(engine);
```

## Community
- [GitHub Issues](https://github.com/totalperspective/pondermatic/issues)
- [Discussion Forums](https://github.com/totalperspective/pondermatic/discussions)
- [Slack Channel](https://join.slack.com/t/pondermatic/shared_invite/...)

## Roadmap
- Improved performance optimizations
- Enhanced data source integrations
- Additional rule definition capabilities
- Expanded documentation and examples

## Contributing
Contributions are welcome! Please read the [Contributing Guidelines](CONTRIBUTING.md) for more information.

## Code of Conduct
This project is governed by the [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.