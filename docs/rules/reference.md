# Rule Language Reference

This document describes the corrected grammar of the rule language used in the Pondermatic library, addressing specific points about predicates, expressions, and inverse relations.

## Rule Definition

```ebnf
Rule ::= "{" RuleID "," RuleName? "," RuleWhen "," RuleThen "}"

RuleID ::= ":id" ":" Identifier
RuleName ::= ":rule/name" ":" String
RuleWhen ::= ":rule/when" ":" WhenPattern
RuleThen ::= ":rule/then" ":" ThenPattern
```

## WhenPattern

```ebnf
WhenPattern ::= WhenObjectPattern
              | SetPattern
              | "'" (WhenObjectPattern | SetPattern) "'"

WhenObjectPattern ::= "{" (WhenPatternEntry ("," WhenPatternEntry)*)? "}"

SetPattern ::= "#{" (SetPatternElement ("," SetPatternElement)*)? "}"

SetPatternElement ::= WhenObjectPattern | PredicateClause

WhenPatternEntry ::= (Keyword | String) ":" WhenPatternValue

WhenPatternValue ::= LiteralValue
                   | LogicVariable
                   | NestedWhenObjectPattern
                   | WhenVector
                   | ModifiedAttribute
                   | EntityCapture
                   | InverseRelation

NestedWhenObjectPattern ::= WhenObjectPattern
```

- WhenPatterns can be represented directly in Clojure EDN or as a string (enclosed in single quotes).
- SetPatterns can include both WhenObjectPatterns and PredicateClauses.
- All LogicVariables used in a PredicateClause must appear in at least one WhenObjectPattern within the same SetPattern.

## ThenPattern

```ebnf
ThenPattern ::= ThenObjectPattern
              | "'" ThenObjectPattern "'"

ThenObjectPattern ::= "{" (ThenPatternEntry ("," ThenPatternEntry)*)? "}"

ThenPatternEntry ::= (Keyword | String) ":" ThenPatternValue
                   | (Keyword | String) "'" ":" ThenPatternValue

ThenPatternValue ::= LiteralValue
                   | LogicVariable
                   | NestedThenObjectPattern
                   | ThenVector
                   | Expression
                   | EntityMerge

NestedThenObjectPattern ::= ThenObjectPattern
```

## LiteralValue

```ebnf
LiteralValue ::= String | Number | Boolean | Keyword | nil
```

## LogicVariable

```ebnf
LogicVariable ::= "?" Identifier
```

## WhenVector

```ebnf
WhenVector ::= "[" WhenObjectPattern "]"
```

## ThenVector

```ebnf
ThenVector ::= "[" (ThenPatternValue ("," ThenPatternValue)*)? "]"
```

## ModifiedAttribute

```ebnf
ModifiedAttribute ::= "(" Modifier Attribute ")"

Modifier ::= ":skip" | "not="
Attribute ::= Keyword | String
```

## EntityCapture

```ebnf
EntityCapture ::= "&" LogicVariable
```

## EntityMerge

```ebnf
EntityMerge ::= "&" LogicVariable
```

## InverseRelation

```ebnf
InverseRelation ::= Keyword
```

- Inverse relations are specified by prefixing the name part of a keyword with an underscore, e.g., `:data/_rel`.

## Expression

```ebnf
Expression ::= "[" "$" ExpressionBody "]"
             | "#expr" ClojureExpression

ExpressionBody ::= ClojureExpression
                 | "(" ClojureExpression "," MergeFunction ")"

MergeFunction ::= Identifier

ClojureExpression ::= /* Any valid Clojure expression */
```

- Only the `[$ ]` form for expressions supports a merge option.
- The `#expr` form does not support a merge option.

## PredicateClause

```ebnf
PredicateClause ::= "(" PredicateFunction LogicVariable+ LiteralValue* ")"

PredicateFunction ::= "=" | "not=" | ">" | "<" | ">=" | "<="
```

- PredicateClauses are limited to built-in comparison functions.
- Custom predicates are not supported out of the box.

## Examples with Explanations

1. SetPattern with PredicateClause:
   ```clojure
   :rule/when #{
     {:id ?id :type "user" :age ?age}
     {:id ?order-id :type "order" :user-id ?id :total ?total}
     (> ?age 18)
     (> ?total 100.00)
   }
   ```
   Explanation: This pattern matches users over 18 who have placed orders with a total exceeding $100. It demonstrates the use of multiple object patterns and predicate clauses within a set pattern. The predicates ensure that only adult users with significant orders are selected.

2. Rule with inverse relation:
   ```clojure
   {:rule/when '{:id ?order-id :type "order"
                 :data/_orders {:id ?user-id :type "user"}}
    :rule/then '{:id ?user-id :order-count' [$ (inc ?order-count)]}}
   ```
   Explanation: This rule finds orders and their associated users using an inverse relation (:data/_orders). It then increments the order count for the user. The inverse relation allows traversing from the order to the user, demonstrating how to navigate relationships in both directions.

3. Rule with expression using merge function:
   ```clojure
   {:rule/when '{:id ?id :type "product" :price ?price}
    :rule/then '{:id ?id :max-price' [$ (max ?price 100.00), max]}}
   ```
   Explanation: This rule updates the 'max-price' of a product to be either its current price or 100.00, whichever is higher. The use of the 'max' merge function ensures that if this rule is applied multiple times, it will always keep the highest value encountered.

4. Rule with #expr expression (no merge option):
   ```clojure
   {:rule/when '{:id ?id :type "product" :price ?price}
    :rule/then '{:id ?id :discounted-price' #expr (math.round (* ?price 0.9))}}
   ```
   Explanation: This rule calculates a 10% discounted price for a product and rounds it to the nearest integer. The #expr form is used here because we're performing a more complex calculation that doesn't require a merge function. Note the use of the math.round function to ensure a whole number result.

## Complete Function Reference for Expressions

Important Note: In addition to the specialized functions listed below, the entire Clojure standard library of functions is available for use in Pondermatic rules. This greatly expands the capabilities of the rule language, allowing for a wide range of operations and data manipulations.

The following list highlights some commonly used functions from both the Clojure standard library and the specialized namespaces provided by Pondermatic. For a complete reference of Clojure's standard functions, please refer to the official Clojure documentation.

1. Math Functions (ns: math)
   - math.abs: (math.abs x) - Returns the absolute value of x
   - math.max: (math.max x y & more) - Returns the greatest of the given numbers
   - math.min: (math.min x y & more) - Returns the least of the given numbers
   - math.round: (math.round x) - Rounds x to the nearest integer
   - math.ceil: (math.ceil x) - Returns the smallest integer greater than or equal to x
   - math.floor: (math.floor x) - Returns the largest integer less than or equal to x

2. String Functions (ns: str)
   - str.lower-case: (str.lower-case s) - Converts string to all lower-case
   - str.upper-case: (str.upper-case s) - Converts string to all upper-case
   - str.trim: (str.trim s) - Removes whitespace from both ends of string
   - str.join: (str.join separator coll) - Returns a string of all elements in coll, separated by separator

3. Case Conversion Functions (ns: case)
   - case.camel: (case.camel s) - Converts string to camelCase
   - case.kebab: (case.kebab s) - Converts string to kebab-case
   - case.snake: (case.snake s) - Converts string to snake_case
   - case.upper: (case.upper s) - Converts string to UPPER_CASE
   - case.lower: (case.lower s) - Converts string to lower_case
   - case.normalize: (case.normalize s) - Normalizes string (lowercase with spaces replaced by underscores)

4. Inflection Functions (ns: inflection)
   - inflection.plural: (inflection.plural word) - Returns the plural form of the word
   - inflection.singular: (inflection.singular word) - Returns the singular form of the word
   - inflection.ordinalize: (inflection.ordinalize number) - Converts number to its ordinal form

5. Hash Functions (ns: hash)
   - hash.uuid: (hash.uuid x) - Generates a deterministic UUID based on input x
   - hash.squuid: (hash.squuid) - Generates a semi-sequential UUID
   - hash.b64: (hash.b64 x) - Generates a base64 hash of input x

6. Walk Functions (ns: w)
   - w.postwalk: (w.postwalk f form) - Performs a depth-first, post-order traversal of form, calling f on each sub-form
   - w.prewalk: (w.prewalk f form) - Performs a depth-first, pre-order traversal of form, calling f on each sub-form

7. Collection Functions (core namespace, no prefix needed)
   - count: (count coll) - Returns the number of items in the collection
   - first: (first coll) - Returns the first item in the collection
   - last: (last coll) - Returns the last item in the collection
   - nth: (nth coll index) - Returns the item at the specified index in the collection
   - conj: (conj coll x) - Returns a new collection with the x added
   - assoc: (assoc map key val) - Returns a new map with the key/value pair added

8. Logical Functions (core namespace, no prefix needed)
   - and: (and x y & more) - Returns true if all arguments are true
   - or: (or x y & more) - Returns true if any argument is true
   - not: (not x) - Returns true if x is logical false, false otherwise

9. Comparison Functions (core namespace, no prefix needed)
   - =, not=, <, >, <=, >=: Standard comparison operators

10. Type Conversion Functions (core namespace, no prefix needed)
    - str: (str x) - Converts x to a string
    - keyword: (keyword x) - Converts x to a keyword
    - int: (int x) - Converts x to an integer

11. Date and Time Functions (ns: t)
    - t.now: (t.now) - Returns the current date-time
    - t.date: (t.date) - Returns the current date
    - t.time: (t.time) - Returns the current time
    - t.plus: (t.plus date-time amount unit) - Adds a duration to a date-time
    - t.minus: (t.minus date-time amount unit) - Subtracts a duration from a date-time

12. Additional Utility Functions
    - read-string: (read-string s) - Parses a string containing EDN data
    - stash: (stash template data) - Renders a mustache template with the given data

Example usage in rules, including some standard Clojure functions:

```clojure
{:rule/when '{:id ?id :type "product" :name ?name :price ?price :tags ?tags :created-at ?created-at}
 :rule/then '{:id ?id 
              :discounted-price' #expr (math.round (* ?price 0.9))
              :upper-name' #expr (case.upper ?name)
              :plural-name' #expr (inflection.plural ?name)
              :hash-id' #expr (hash.uuid ?id)
              :transformed-data' #expr (w.postwalk (fn [x] (if (number? x) (inc x) x)) ?data)
              :days-since-creation' #expr (t.days (t.between ?created-at (t.now)))
              :template-result' #expr (stash "Hello, {{name}}! Your product costs ${{price}}." 
                                             {:name ?name :price ?price})
              :tag-count' #expr (count ?tags)
              :sorted-tags' #expr (sort ?tags)
              :first-tag' #expr (first ?tags)
              :tag-string' #expr (str.join ", " ?tags)
              :even-price?' #expr (even? (int ?price))}}
```

This comprehensive reference covers a wide range of functions available in expressions within Pondermatic rules, including both specialized functions and standard Clojure functions. Users can combine these functions to create complex logic and data transformations in their rules, leveraging the full power of Clojure along with the additional capabilities provided by Pondermatic.
