# Features

Pondermatic is a powerful rules engine designed to work seamlessly with Clojure, ClojureScript, and JavaScript. Here are some of its key features:

## Functional Effect System

Pondermatic leverages a functional effect system to manage side effects in a predictable and composable manner. This ensures that your rules are both easy to reason about and maintain.

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

## Cross-Platform Support

Pondermatic supports Clojure, ClojureScript, and JavaScript, making it a versatile choice for projects that span multiple platforms.

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

## Declarative Rule Definitions

Define rules in a declarative manner, making them easy to read and understand. This approach reduces the complexity of rule management and enhances maintainability.

```clojure
(def rules
  (p/ruleset
   [{:id :example-rule
     :rule/when {:data/key ?value}
     :rule/then {:data/new-key ?value}}]))
```

## Integration with Various Data Sources

Pondermatic can integrate with a variety of data sources, allowing you to create rules that react to changes in your data environment.

```clojure
(def data
  (p/dataset
   [{:key "value"}]))
```

## Comprehensive Logging and Debugging

Pondermatic provides robust logging and debugging tools to help you monitor and troubleshoot your rules.

```clojure
(p/log-level :debug)
```

## Extensible and Customizable

Pondermatic is designed to be extensible and customizable, allowing you to tailor it to your specific needs.

```clojure
(defn custom-rule [data]
  ;; Custom rule logic here
  )
```

These features make Pondermatic a powerful and flexible choice for managing rules in your applications. For more detailed examples and usage instructions, please refer to the [Examples](docs/examples.md) and [API Reference](docs/api_reference_js.md) sections.
