# Pondermatic Rule Syntax Reference

Rules in Pondermatic are defined using a map structure with specific keys. Here's a breakdown of the syntax based on the examples:

## Rule Structure

```clojure
{:id ::rule-id
 :rule/name "Rule Name"
 :rule/when condition-map
 :rule/then result-map}
```

### Key Components:

1. `:id`: A unique identifier for the rule, typically a namespaced keyword.
2. `:rule/name`: A human-readable name for the rule (string).
3. `:rule/when`: The condition part of the rule (map).
4. `:rule/then`: The consequence or action part of the rule (map).

## Condition Syntax (`:rule/when`)

The `:rule/when` key contains a map that defines the pattern to match in the data.

### Variable Binding:
- Use `?` prefix for variables: `?variable-name`
- Example: `?batter`, `?topping`, `?ppu`

### Data Structure Matching:
- Use nested maps and vectors to match complex data structures.
- Example:
  ```clojure
  {:batters [{":db/ident" ?batter-id
              :batter ?batter}]
   :toppings [{":db/ident" ?topping-id
               :type "topping"
               :topping ?topping}]}
  ```

### Specific Value Matching:
- Use strings or other literal values to match exact values.
- Example: `:type "donut"`, `:batter "Regular"`

## Result Syntax (`:rule/then`)

The `:rule/then` key contains a map that defines the action to take when the condition is met.

### Simple Value Assignment:
- Directly assign values or use bound variables.
- Example: `:type "combination"`, `:batter-id ?batter-id`

### Computed Values:
- Use a vector starting with `$` to indicate a computed value.
- Example: `:ppu (str '[$ (* 0.7 ?ppu)])`

### String Interpolation:
- Use `str` function with a vector starting with `$` for string interpolation.
- Example: `:name (str '[$ (str ?name " - Regular Glazed")])`

### Quoted Variables:
- Use single quote `'` to refer to bound variables without evaluation.
- Example: `:donut-id '?donut-id`

## Complete Rule Example

```clojure
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
             :topping-id '?topping-id}}
```
