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

To create an engine, use the `createEngine` function:

```javascript
import pondermatic from '@totalperspective/pondermatic';

const engine = pondermatic.createEngine('example', true);
```

### Defining Rules

Rules can be defined using the `ruleset` function:

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

Data can be added to the engine using the `dataset` function and the `sh` function:

```javascript
const data = pondermatic.dataset([{ key: 'value' }]);

pondermatic.sh(engine, { '->db': rules });
pondermatic.sh(engine, { '->db': data });
```

### Querying Data

To query data, use the `q` function:

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

To stop the engine, use the `stop` function:

```javascript
pondermatic.stop(engine);
```

## API Reference

### `createEngine(name: string, resetDb: boolean): Engine`

Creates a new engine instance.

- `name`: The name of the engine.
- `resetDb`: A boolean indicating whether to reset the database.

### `ruleset(rules: Array<Object>): Ruleset`

Defines a set of rules.

- `rules`: An array of rule objects.

### `dataset(data: Array<Object>): Dataset`

Creates a dataset from an array of data objects.

- `data`: An array of data objects.

### `sh(engine: Engine, msg: Object): void`

Sends a message to the engine.

- `engine`: The engine instance.
- `msg`: The message object.

### `q(engine: Engine, query: string, args: Array<any>, callback: Function): Query`

Executes a query on the engine.

- `engine`: The engine instance.
- `query`: The query string.
- `args`: An array of arguments for the query.
- `callback`: A callback function to handle the query results.

### `stop(engine: Engine): void`

Stops the engine.

- `engine`: The engine instance.

## Examples

For more examples, refer to the [Examples](docs/examples.md) section.

## Getting Started

To get started with Pondermatic, refer to the [Getting Started](docs/getting_started.md) guide.

## Features

For a list of features, refer to the [Features](docs/features.md) section.

## Installation

For installation instructions, refer to the [Installation](docs/installation.md) guide.

## Introduction

For an introduction to Pondermatic, refer to the [Introduction](docs/introduction.md) section.
