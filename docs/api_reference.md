# API Reference

## Public Functions and Macros

### `create-engine`
- **Description**: Creates a new engine instance.
- **Parameters**:
  - `name` (string): The name of the engine.
  - `reset-db?` (boolean): Whether to reset the database.
- **Returns**: An engine instance.
- **Example**:
  \`\`\`clojure
  (def engine (create-engine "example" true))
  \`\`\`

### `ruleset`
- **Description**: Defines a set of rules.
- **Parameters**:
  - `rules` (vector): A vector of rule definitions.
- **Returns**: A ruleset.
- **Example**:
  \`\`\`clojure
  (def rules
    (ruleset
     [{:id :example-rule
       :rule/when {:data/key ?value}
       :rule/then {:data/new-key ?value}}]))
  \`\`\`

### `dataset`
- **Description**: Defines a dataset.
- **Parameters**:
  - `data` (vector): A vector of data entries.
- **Returns**: A dataset.
- **Example**:
  \`\`\`clojure
  (def data
    (dataset
     [{:key "value"}]))
  \`\`\`

### `sh`
- **Description**: Executes a command on the engine.
- **Parameters**:
  - `engine` (engine): The engine instance.
  - `command` (map): The command to execute.
- **Returns**: The result of the command.
- **Example**:
  \`\`\`clojure
  (sh engine {:->db rules})
  \`\`\`

### `stop`
- **Description**: Stops the engine.
- **Parameters**:
  - `engine` (engine): The engine instance.
- **Returns**: None.
- **Example**:
  \`\`\`clojure
  (stop engine)
  \`\`\`

## Table of Contents
- [Public Functions and Macros](#public-functions-and-macros)
  - [`create-engine`](#create-engine)
  - [`ruleset`](#ruleset)
  - [`dataset`](#dataset)
  - [`sh`](#sh)
  - [`stop`](#stop)

## Changelog
### Version 1.11.10
- Added support for JavaScript.
- Improved performance optimizations.
- Enhanced data source integrations.
- Additional rule definition capabilities.
- Expanded documentation and examples.