# Introduction

Welcome to the Pondermatic documentation! This guide will help you understand the core concepts, features, and usage of Pondermatic, a functional effect system-driven rules engine designed for use with Clojure, ClojureScript, and JavaScript.

## What is Pondermatic?

Pondermatic is a robust framework for defining and executing rules in a declarative manner. It supports integration with various data sources and provides a functional effect system to manage side effects in a predictable way. By using Pondermatic, developers can build complex rule-based systems with ease, ensuring that side effects are handled in a consistent and composable manner.

## Key Features

- **Functional Effect System**: Manage side effects in a predictable and composable manner.
- **Multi-language Support**: Works seamlessly with Clojure, ClojureScript, and JavaScript.
- **Declarative Rule Definitions**: Define rules in a clear and concise way.
- **Data Integration**: Easily integrate with various data sources.

## When to use Pondermatic
- **Business Rule Management**: Use Pondermatic to define and manage business rules that need to be applied consistently across different parts of your application.
- **Data Transformation**: Leverage Pondermatic to transform and process data from various sources, ensuring that the transformations are applied in a predictable manner.
- **Event-driven Systems**: Implement event-driven systems where rules are triggered based on specific events or conditions, allowing for flexible and dynamic behavior.

![Sample Rule Definition](images/sample_rule_definition.png)

## Getting Started

To get started with Pondermatic, follow the installation instructions and explore the examples provided in the documentation.

## Example Usage

Here is a simple example of how to define and use rules in Pondermatic:

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

For more detailed examples and advanced usage, refer to the [Examples](examples.md) and [API Reference](api_reference_js.md) sections.

## Contributing

Contributions are welcome! Please read the [contributing guidelines](CONTRIBUTING.md) for more information.

## License

This project is licensed under the Eclipse Public License - v 2.0. See the [LICENSE](LICENSE) file for details.