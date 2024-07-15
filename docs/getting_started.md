# Getting Started

Welcome to the Pondermatic documentation! This guide will help you get started with Pondermatic, a functional effect system-driven rules engine designed for use with Clojure, ClojureScript, and JavaScript.

## Prerequisites
Before you begin, ensure you have the following installed:

- [Java 11 or higher](https://adoptopenjdk.net/)
- [Node.js 16 or higher](https://nodejs.org/)
- [Clojure CLI tools](https://clojure.org/guides/getting_started)

## Getting Started
### Creating a New Project
1. Create a new directory for your project:
   ```sh
   mkdir my-pondermatic-project
   cd my-pondermatic-project
   ```
2. Initialize a new Clojure project:
   ```sh
   clj -A:new app my-pondermatic-project
   cd my-pondermatic-project
   ```

### Installing Pondermatic
#### Clojure/ClojureScript
Add the following dependency to your `deps.edn` file:
```clojure
{:deps {tech.totalperspective/pondermatic {:mvn/version "1.11.10"}}}
```

#### JavaScript
Install via npm:
```sh
npm install @totalperspective/pondermatic
```

### Running a "Hello World" Example
#### Clojure/ClojureScript
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

#### JavaScript
Create a new file and import Pondermatic:
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

## Troubleshooting
If you encounter any issues during setup, try the following:
- Ensure you have the correct versions of Java, Node.js, and Clojure CLI tools installed.
- Check for typos or syntax errors in your code.
- Refer to the [GitHub Issues](https://github.com/totalperspective/pondermatic/issues) page for known issues and solutions.
- Ask for help in the [Discussion Forums](https://github.com/totalperspective/pondermatic/discussions) or [Slack Channel](https://join.slack.com/t/pondermatic/shared_invite/...).

## Examples
For more detailed examples and usage, refer to the [Examples](examples.md) and [API Reference](api_reference.md) documentation.

## Contributing
Contributions are welcome! Please read the [contributing guidelines](CONTRIBUTING.md) for more information.

## License
This project is licensed under the Eclipse Public License - v 2.0. See the [LICENSE](LICENSE) file for details.