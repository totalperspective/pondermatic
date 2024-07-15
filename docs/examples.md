# Examples

This document provides examples of how to use various features of the project. Below are some code samples and their explanations.

## Example 1: Basic Usage

Here is a basic example of how to use the `pondermatic` library to create an engine and add data to it.

```clojure
(ns example.basic
  (:require [pondermatic.core :as p]))

(def engine (p/create-engine "example" true))

(def rules (p/ruleset
  [{:id "example-rule"
    :rule/when {:data/key "?value"}
    :rule/then {:data/new-key "?value"}}]))

(def data (p/dataset [{:key "value"}]))

(p/sh engine {:->db rules})
(p/sh engine {:->db data})
(p/stop engine)
```

## Example 2: Advanced Query

This example demonstrates how to perform a more advanced query using the `pondermatic` library.

```clojure
(ns example.advanced
  (:require [pondermatic.core :as p]))

(def engine (p/create-engine "advanced-example" true))

(def rules (p/ruleset
  [{:id "advanced-rule"
    :rule/when {:data/deep {:data/nested "?value"}}
    :rule/then {:data/new-key "?value"}}]))

(def data (p/dataset [{:deep {:nested "value"}}]))

(p/sh engine {:->db rules})
(p/sh engine {:->db data})

(def query (p/q engine "[:find ?v . :where [?id :data/new-key ?v]]" []))

(query (fn [result]
         (println "Query result:" result)))

(p/stop engine)
```

## Example 3: Using with JavaScript

Here is an example of how to use the `pondermatic` library in a JavaScript environment.

```javascript
import pondermatic from 'pondermatic';

const engine = pondermatic.createEngine('js-example', true);

const rules = pondermatic.ruleset([
  {
    id: 'js-rule',
    'rule/when': { 'data/key': '?value' },
    'rule/then': { 'data/new-key': '?value' }
  }
]);

const data = pondermatic.dataset([{ key: 'value' }]);

pondermatic.sh(engine, { '->db': rules });
pondermatic.sh(engine, { '->db': data });

pondermatic.q(engine, '[:find ?v . :where [?id :data/new-key ?v]]', [], (result) => {
  console.log('Query result:', result);
});

pondermatic.stop(engine);
```

## Example 4: Handling Errors

This example shows how to handle errors gracefully in your code.

```clojure
(ns example.error-handling
  (:require [pondermatic.core :as p]))

(def engine (p/create-engine "error-example" true))

(def rules (p/ruleset
  [{:id "error-rule"
    :rule/when {:data/key "?value"}
    :rule/then {:data/new-key "?value"}}]))

(def data (p/dataset [{:key "value"}]))

(try
  (p/sh engine {:->db rules})
  (p/sh engine {:->db data})
  (println "Data added successfully")
  (catch Exception e
    (println "An error occurred:" (.getMessage e))))

(p/stop engine)
```

## Example 5: Integration with Other Libraries

This example demonstrates how to integrate `pondermatic` with other libraries.

```clojure
(ns example.integration
  (:require [pondermatic.core :as p]
            [clojure.pprint :as pprint]))

(def engine (p/create-engine "integration-example" true))

(def rules (p/ruleset
  [{:id "integration-rule"
    :rule/when {:data/key "?value"}
    :rule/then {:data/new-key "?value"}}]))

(def data (p/dataset [{:key "value"}]))

(p/sh engine {:->db rules})
(p/sh engine {:->db data})

(def query (p/q engine "[:find ?v . :where [?id :data/new-key ?v]]" []))

(query (fn [result]
         (pprint/pprint result)))

(p/stop engine)
```

These examples should help you get started with using the `pondermatic` library in various scenarios. For more detailed information, refer to the [API Reference](api_reference_js.md).
