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

> **Best Practice**: When defining rules, ensure that the conditions and actions are clearly specified to avoid unexpected behavior.

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

> **Performance Consideration**: When working with large datasets, consider optimizing your rules and data structures to improve performance.

## Declarative Rule Definitions

Define rules in a declarative manner, making them easy to read and understand. This approach reduces the complexity of rule management and enhances maintainability.

```clojure
(def rules
  (p/ruleset
   [{:id :example-rule
     :rule/when {:data/key ?value}
     :rule/then {:data/new-key ?value}}]))
```

> **Common Pitfall**: Avoid defining overly complex rules that are difficult to debug and maintain. Break down complex logic into smaller, manageable rules.

## Integration with Various Data Sources

Pondermatic can integrate with a variety of data sources, allowing you to create rules that react to changes in your data environment.

```clojure
(def data
  (p/dataset
   [{:key "value"}]))
```

> **Best Practice**: Regularly update and validate your data sources to ensure that your rules are working with accurate and up-to-date information.

## Comprehensive Logging and Debugging

Pondermatic provides robust logging and debugging tools to help you monitor and troubleshoot your rules.

```clojure
(p/log-level :debug)
```

> **Common Pitfall**: Avoid excessive logging in production environments, as it can impact performance. Use appropriate log levels to balance visibility and performance.

## Extensible and Customizable

Pondermatic is designed to be extensible and customizable, allowing you to tailor it to your specific needs.

```clojure
(def custom-rule
  {:id :custom-rule
   :rule/when {:data/key ?value}
   :rule/then {:data/custom-key ?value}})
```

> **Best Practice**: Leverage Pondermatic's extensibility to create custom rules and actions that align with your application's requirements.

These features make Pondermatic a powerful and flexible choice for managing rules in your applications. For more detailed examples and usage instructions, please refer to the [Examples](docs/examples.md) and [API Reference](docs/api_reference.md) sections.