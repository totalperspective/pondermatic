# API Reference (JavaScript)

## Overview

Pondermatic is a functional effect system-driven rules engine designed for use with Clojure, ClojureScript, and JavaScript. This document provides an API reference for using Pondermatic with JavaScript.

## Installation

Install via npm:

```sh
npm install @totalperspective/pondermatic
```

## Usage

### Creating an Engine

- **Description**: Creates a new engine instance.
- **Parameters**:
  - `name` (string): The name of the engine.
  - `resetDb` (boolean): Whether to reset the database.
- **Returns**: An engine instance.
- **Example**:
  ```javascript
  import pondermatic from '@totalperspective/pondermatic';
  const engine = pondermatic.createEngine('example', true);
  ```

### Defining Rules

- **Description**: Defines a set of rules.
- **Parameters**:
  - `rules` (Array<Object>): An array of rule definitions.
- **Returns**: A ruleset.
- **Example**:
  ```javascript
  const rules = pondermatic.ruleset([
    {
      id: 'example-rule',
      'rule/when': { 'data/key': '?value' },
      'rule/then': { 'data/new-key': '?value' }
    }
  ]);
  ```

### Adding Data

- **Description**: Creates a dataset from an array of data objects.
- **Parameters**:
  - `data` (Array<Object>): An array of data objects.
- **Returns**: A dataset.
- **Example**:
  ```javascript
  const data = pondermatic.dataset([{ key: 'value' }]);
  pondermatic.sh(engine, { '->db': rules });
  pondermatic.sh(engine, { '->db': data });
  ```

### Querying Data

- **Description**: Executes a query on the engine.
- **Parameters**:
  - `engine` (Engine): The engine instance.
  - `query` (string): The query string.
  - `args` (Array<any>): An array of arguments for the query.
  - `callback` (Function): A callback function to handle the query results.
- **Returns**: Query results.
- **Example**:
  ```javascript
  const q = pondermatic.q(
    engine,
    "[:find ?v . :where [?id :data/key ?v]]",
    [],
    r => {
      if (!r) {
        return;
      }
      console.log(r);
    }
  );
  ```

### Stopping the Engine

- **Description**: Stops the engine.
- **Parameters**:
  - `engine` (Engine): The engine instance.
- **Returns**: None.
- **Example**:
  ```javascript
  pondermatic.stop(engine);
  ```

## Table of Contents
- [Creating an Engine](#creating-an-engine)
- [Defining Rules](#defining-rules)
- [Adding Data](#adding-data)
- [Querying Data](#querying-data)
- [Stopping the Engine](#stopping-the-engine)

## Changelog
### Version 1.11.10
- Added support for JavaScript.
- Improved performance optimizations.
- Enhanced data source integrations.
- Additional rule definition capabilities.
- Expanded documentation and examples.
