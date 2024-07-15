# Rule Syntax

The rule syntax in our system is defined using Clojure data structures and follows a specific pattern. Below is an example of how rules are defined and used within the codebase.

## Example Rule Definition

```clojure:src/example/production_rules.cljc
(def rules
  (p/ruleset
   [{:id ::combinations
     :rule/name "Combinations"
     :rule/when '{:batters [{":db/ident" ?batter-id
                             :batter ?batter}]
                  :toppings [{":db/ident" ?topping-id
                              :type "topping"
                              :topping ?topping}]}
     :rule/then '{":db/ident" [?batter-id ?topping-id]
                  :type "combination"
                  :batter-id ?batter-id
                  :topping-id ?topping-id}}
    {:id ::regular-glazed-offer
     :rule/name "Regular glazed offer"
     :rule/when '{":db/ident" ?donut-id
                  :type "donut"
                  :name ?name
                  :ppu ?ppu
                  :batters [{":db/ident" ?batter-id
                             :batter "Regular"}]
                  :toppings [{":db/ident" ?topping-id
                              :topping "Glazed"}]}
     :rule/then {:type "offer"
                 :ppu (str '[$ (* 0.7 ?ppu)])
                 :name (str '[$ (str ?name " - Regular Glazed")])
                 :donut-id '?donut-id
                 :batter-id '?batter-id
                 :topping-id '?topping-id}}]))
```

## Rule Components

Each rule consists of the following components:

- **id**: A unique identifier for the rule.
- **rule/when**: A condition that must be met for the rule to be triggered.
- **rule/then**: The action to be taken when the condition is met.

### Example Breakdown

1. **Combinations Rule**
   - **id**: `::combinations`
   - **rule/when**: The condition checks for combinations of batters and toppings.
   - **rule/then**: Creates a combination entity with the batter and topping IDs.

2. **Regular Glazed Offer Rule**
   - **id**: `::regular-glazed-offer`
   - **rule/when**: The condition checks for donuts with a regular batter and glazed topping.
   - **rule/then**: Creates an offer with a discounted price and a new name.

## Rule Parsing

The rules are parsed and converted into a format that the engine can understand using the `ruleset` function.

```clojure:src/pondermatic/core.cljc
startLine: 64
endLine: 70
```

This function performs several transformations:
- **id->ident**: Converts IDs to a specific format.
- **kw->qkw**: Converts keywords to qualified keywords.
- **parse-strings**: Parses string values.
- **parse-patterns**: Parses rule patterns.
- **component->entity**: Converts components to entities.

## JavaScript Support

In addition to the pure Clojure syntax, our system also supports JavaScript integration. The following example demonstrates how rules can be defined in a JavaScript-compatible format.

```javascript:src/example/js_syntax.cljs
const rules = pondermatic.ruleset([
  {
    "id": "terminate/activate",
    "rule/when": "{terminate/reason ?reason}",
    "rule/then": {
      "id": "terminate/task",
      "type": "task",
      "task/active?": true,
      "task/priority": 100
    }
  },
  {
    "id": "other/activate",
    "rule/when": "{some/var ?val} (> ?val 0)",
    "rule/then": {
      "id": "other/task",
      "type": "task",
      "task/active?": true,
      "task/priority": 100
    }
  }
]);
```

### Example Breakdown

1. **Terminate/Activate Rule**
   - **id**: "terminate/activate"
   - **rule/when**: The condition checks for the presence of `terminate/reason`.
   - **rule/then**: Activates the task with a priority of 100.

2. **Other/Activate Rule**
   - **id**: "other/activate"
   - **rule/when**: The condition checks if `some/var` is greater than 0.
   - **rule/then**: Activates the task with a priority of 100.

## Syntax Reference

### Pattern Parsing

The `parse-pattern` function is used to parse patterns into a structured format. Here are some examples:

```clojure:src/pondermatic/rules/production.cljc
startLine: 17
endLine: 70
```

- **Simple Attribute Matching**:
  ```clojure
  (parse-pattern '{:attr :val} {})
  ; => {::tag :join
  ;     ::id ?id
  ;     ::select [{::tag :project
  ;                ::attr {::tag :attribute
  ;                        ::attribute :attr}
  ;                ::val {::tag :value
  ;                       ::value :val}}]}
  ```

- **Nested Attribute Matching**:
  ```clojure
  (parse-pattern '{:id :id1
                   :attr {:id :id2
                          :attr2 :val}} {})
  ; => {::tag :join
  ;     ::id :id1
  ;     ::select [{::tag :project
  ;                ::attr {::tag :attribute
  ;                        ::attribute :attr}
  ;                ::val {::tag :join
  ;                       ::id :id2
  ;                       ::select [{::tag :project
  ;                                  ::attr {::tag :attribute
  ;                                          ::attribute :attr2}
  ;                                  ::val {::tag :value
  ;                                         ::value :val}}]}}]}
  ```

### Pattern to What Conversion

The `pattern->what` function converts parsed patterns into a "what" format. Here are some examples:

```clojure:src/pondermatic/rules/production.cljc
startLine: 648
endLine: 782
```

- **Simple Attribute**:
  ```clojure
  (pattern->what '{:attr ?val})
  ; => [[_ :attr '?val]]
  ```

- **Nested Attribute**:
  ```clojure
  (pattern->what '{:attr [{:attr2 ?val}]})
  ; => [[?b :p/contained-by ?a]
  ;     [?b :p/attr :attr]
  ;     [?b :a/first ?c]
  ;     [?c :attr2 '?val]]
  ```

## Conclusion

Understanding the rule syntax and its components is crucial for defining and managing rules within the system. The provided examples and parsing functions illustrate how rules are structured and processed. The system supports both pure Clojure syntax and JavaScript-compatible syntax, enabling flexibility in rule definition and integration.
