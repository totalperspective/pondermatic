# Rules

## Overview
### Production Rules & O'doyle Rules
Production rules in the system are defined using the `pondermatic.rules` namespace. These rules are used to manage the state and behavior of the system by specifying conditions and actions. For example, a rule might specify that when a certain condition is met, a specific action should be taken.

\```
(ns example.production-rules
  (:require [pondermatic.core :as p]
            [pondermatic.shell :refer [|>]]))
\```

O'Doyle rules can be integrated into the system to enhance rule management. O'Doyle is a rules engine for Clojure and ClojureScript that allows for the creation of complex rule-based systems. For more details, refer to the [O'Doyle Rules documentation](https://github.com/oakes/odoyle-rules/issues/3).

### Datasets & Asami Datastore
Datasets in the system are managed using the `pondermatic.core` namespace. The data is stored in an Asami datastore, which is a graph database that allows for flexible and efficient querying of data.

\```
(def data
  (p/dataset
   [{:id "red-apple"
     :fruit "Apple"
     :size "Large"}]))
\```

Asami is a graph database that supports RDF and SPARQL, making it suitable for complex data relationships and queries. For more information, refer to the [Asami documentation](https://github.com/quoll/asami/wiki/Transactions).

## Operational Semantics

### Agent-based/message-passing architecture
The system uses an agent-based architecture where agents communicate through message passing. This approach allows for concurrent processing and scalability.

### Data-flow diagram
The data flow in the system can be visualized as follows:

1. **Data Ingestion**: Data is ingested into the system and stored in the Asami datastore.
2. **Rule Evaluation**: Production rules and O'Doyle rules are evaluated to determine the actions to be taken based on the data.
3. **Action Execution**: Actions specified by the rules are executed, which may involve updating the datastore or triggering other processes.

\```
+----------------+       +----------------+       +----------------+
|                |       |                |       |                |
|  Data Ingestion| ----> | Rule Evaluation| ----> | Action Execution|
|                |       |                |       |                |
+----------------+       +----------------+       +----------------+
       ^                                                |
       |                                                v
       +------------------------------------------------+
\```

For more details on the implementation, refer to the source files in the `src` directory, such as `src/pondermatic/rules/production.cljc` and `src/pondermatic/core.cljc`.

## Identifiers

### Identifiers in the system
Identifiers play a crucial role in managing entities within the system. They are used to uniquely identify and reference entities, ensuring that data can be accurately retrieved and manipulated.

#### :db/ident
The `:db/ident` attribute is used to assign a unique identifier to an entity. This identifier is a real attribute that is placed on the entity, although it is hidden when entities are retrieved via the entity function. It can be any data type and is often used to address entities in subsequent transactions.

Example:
\```
{:db/ident "widget"
 :inventory/label "Widget"
 :inventory/part-nr "THX-1138"
 :inventory/stock-count 2187}
\```

#### :id
The `:id` attribute serves a similar role to `:db/ident` but is visible to the user and will be returned by the `entity` function. This makes it easier to work with entities directly.

Example:
\```
{:id "red-apple"
 :fruit "Apple"
 :size "Large"}
\```

#### Temporary IDs
Temporary IDs are used to create references between entities during transactions. These IDs are negative integers and are automatically updated to refer to the allocated node once the entity is created.

Example:
\```
{:db/id -1
 :inventory/label "Widget"
 :inventory/part-nr "THX-1138"
 :inventory/stock-count 2187}
{:db/id -2
 :inventory/label "Doohicky"
 :inventory/part-nr "AA-23"
 :inventory/stock-count 5
 :inventory/replaces {:db/id -1}}
\```

#### Nested References
Nested objects can also have identifiers, which is useful for later reference. This allows for complex data structures with shared sub-objects.

Example:
\```
(def data [{:db/ident "charles"
            :name "Charles"
            :home {:db/ident "scarborough"
                   :town "Scarborough"
                   :county "Yorkshire"}}
           {:db/ident "jane"
            :name "Jane"
            :home {:db/ident "scarborough"}}])
\```

For more details on identifiers and their usage, refer to the [Asami documentation](https://github.com/quoll/asami/wiki/5.-Entity-Structure).

### Example Usage in Code
In the codebase, identifiers are used extensively to manage entities and their relationships. Here are some examples:

- In `src/example/ui.cljc`, identifiers are used to define the state and layout of the user interface:
  ```clojure:src/example/ui.cljc
  startLine: 6
  endLine: 19
  ```

- In `src/pondermatic/rules/production.cljc`, identifiers are used in the pattern matching and rule definitions:
  ```clojure:src/pondermatic/rules/production.cljc
  startLine: 136
  endLine: 138
  ```

- In `src/pondermatic/core.cljc`, identifiers are used to parse and manage datasets:
  ```clojure:src/pondermatic/core.cljc
  startLine: 171
  endLine: 176
  ```

These examples illustrate how identifiers are integral to the system's functionality, enabling efficient data management and rule processing.

## Transactions

### Overview
Transactions are the mechanism for modifying the database. They ensure that no previous versions of the database are altered; instead, a new database is created with the changes incorporated. This process is efficient in both speed and storage, as the old and new databases share the majority of their data.

### Transaction Data Structure
All data in a graph is of the form:

_node-1_ _edge_ _node-2_

These tuples describe two nodes and a directed-labeled edge between them. The second node can be a scalar value (such as a number or a string). These can be treated as nodes in most cases but should not have edges coming out of them.

Tuples like this are represented as _Datoms_. A Datom is an operation that describes these three elements and whether they were added or removed from the graph. For instance:

\```
#datom [:a/node-10502 :relates-to :a/node-10499 1 true]
\```

The first three elements are the first node, the edge, and the second node. The next value is the transaction number when the operation was executed. The final value is `true` to indicate that the data is added, or `false` to indicate that it is removed.

### Example Transaction
Define some data and add it to the database:

\```
(def data
  [{:message "Hi Dad" :time "2020-07-29T04:23:19.622-00:00"}
   {:message "Hello Daughter. Why are you still up?" :time "2020-07-29T04:23:53.906-00:00"}
   {:message "I'm writing docs for Asami" :time "2020-07-29T04:24:44.966-00:00"}
   {:message "What's Asami?" :time "2020-07-29T04:25:11.836-00:00"}])
(def tx (d/transact conn data))
\```

Dereferencing the response and looking at the structure with `(clojure.pprint/pprint @tx)`:

\```
{:db-before
 {:graph {:spo {}, :pos {}, :osp {}},
  :history [],
  :timestamp #inst "2020-07-29T04:26:26.050-00:00"},
 :db-after ....... ;; !!!BIG SCARY DATA STRUCTURE!!! DON'T WORRY ABOUT THIS PART!
 :tx-data
 (#datom [:a/node-10498 :db/ident :a/node-10498 1 true]
  #datom [:a/node-10498 :a/entity true 1 true]
  #datom [:a/node-10498 :message "Hi Dad" 1 true]
  #datom [:a/node-10498 :time "2020-07-29T04:23:19.622-00:00" 1 true]
  #datom [:a/node-10499 :db/ident :a/node-10499 1 true]
  #datom [:a/node-10499 :a/entity true 1 true]
  #datom [:a/node-10499 :message "Hello Daughter. Why are you still up?" 1 true]
  #datom [:a/node-10499 :time "2020-07-29T04:23:53.906-00:00" 1 true]
  #datom [:a/node-10500 :db/ident :a/node-10500 1 true]
  #datom [:a/node-10500 :a/entity true 1 true]
  #datom [:a/node-10500 :message "I'm writing docs for Asami" 1 true]
  #datom [:a/node-10500 :time "2020-07-29T04:24:44.966-00:00" 1 true]
  #datom [:a/node-10501 :db/ident :a/node-10501 1 true]
  #datom [:a/node-10501 :a/entity true 1 true]
  #datom [:a/node-10501 :message "What's Asami?" 1 true]
  #datom [:a/node-10501 :time "2020-07-29T04:25:11.836-00:00" 1 true]),
 :tempids
 #:a{:node-10498 :a/node-10498,
     :node-10499 :a/node-10499,
     :node-10500 :a/node-10500,
     :node-10501 :a/node-10501}}
\```

### Temporary IDs
Temporary IDs are used to create references between entities during transactions. These IDs are negative integers and are automatically updated to refer to the allocated node once the entity is created.

Example:
\```
{:db/id -1
 :inventory/label "Widget"
 :inventory/part-nr "THX-1138"
 :inventory/stock-count 2187}
{:db/id -2
 :inventory/label "Doohicky"
 :inventory/part-nr "AA-23"
 :inventory/stock-count 5
 :inventory/replaces {:db/id -1}}
\```

For more details on transactions and their usage, refer to the [Asami documentation](https://github.com/quoll/asami/wiki/4.-Transactions).

## Inserting vs Upserting

### Inserting
Inserting is the process of adding new data to the database. When you insert data, you are creating new entities or adding new attributes to existing entities without modifying any existing data. This is useful when you are sure that the data being added does not conflict with or need to replace any existing data.

Example:
\```
(def data
  [{:db/ident "apple"
    :fruit "Apple"
    :size "Large"
    :color "Red"}])
(def tx (d/transact conn {:tx-data data}))
\```

In the codebase, inserting can be seen in the following snippet:
```clojure:src/pondermatic/rules.cljc
startLine: 41
endLine: 49
```

### Upserting
Upserting is a combination of updating and inserting. It ensures that if the entity or attribute already exists, it will be updated; otherwise, it will be inserted. This is particularly useful when you want to ensure that the database reflects the most current state of the data without creating duplicates.

Example:
\```
(def data
  [{:db/ident "apple"
    :fruit "Apple"
    :size "Large"
    :color "Green"}])
(def tx (d/transact conn {:tx-data (upsert data)}))
\```

In the codebase, upserting can be seen in the following snippet:
```clojure:src/pondermatic/db.cljc
startLine: 122
endLine: 131
```

### Differences and Use Cases
- **Inserting** is used when you are adding entirely new data that does not need to replace or update any existing data.
- **Upserting** is used when you want to ensure that the data is current, either by updating existing entities or inserting new ones if they do not already exist.

### Example Usage in Code
In the codebase, both inserting and upserting are used to manage data efficiently:

- Inserting data:
  ```clojure:src/pondermatic/rules.cljc
  startLine: 41
  endLine: 49
  ```

- Upserting data:
  ```clojure:src/pondermatic/db.cljc
  startLine: 122
  endLine: 131
  ```

These examples illustrate how inserting and upserting are integral to the system's functionality, enabling efficient data management and ensuring data consistency.