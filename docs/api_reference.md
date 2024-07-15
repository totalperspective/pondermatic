# API Reference

## Overview

The `pondermatic` library provides a functional effect system driven rules engine. This document serves as a reference for the API provided by the library.

## Table of Contents

- [Engine](#engine)
- [Rules](#rules)
- [Dataset](#dataset)
- [Commands](#commands)
- [Query](#query)
- [Utilities](#utilities)

## Engine

### `createEngine`

Creates a new engine instance.

```
(def engine (pondermatic.createEngine "example" true))
```

### `stop`

Stops the engine.

```
pondermatic.stop(engine)
```

## Rules

### `ruleset`

Defines a set of rules.

```
(def rules (pondermatic.ruleset [
  {
    :id "example-rule",
    :rule/when {:data/key "?value"},
    :rule/then {:data/new-key "?value"}
  }
]))
```

## Dataset

### `dataset`

Defines a dataset.

```
(def data (pondermatic.dataset [
  {:key "value"}
]))
```

## Commands

### `sh`

Executes a command on the engine.

```
pondermatic.sh(engine, {:->db rules})
pondermatic.sh(engine, {:->db data})
```

## Query

### `q`

Executes a query on the engine.

```
(def q (pondermatic.q engine
  "[:find ?v . :where [?id :data/new-key ?v]]"
  []
  (fn [result]
    (js/console.log result))))
```

## Utilities

### `uuid-hash`

Generates a UUID hash.

```
(def uuid (pondermatic.uuid-hash "example"))
```

### `log`

Logs a message.

```
pondermatic.log(:info "This is an info message")
```

### `eval-string`

Evaluates a string as code.

```
(def result (pondermatic.eval-string "(+ 1 2 3)"))
```

For more detailed examples and usage, refer to the [Examples](examples.md) and [Getting Started](getting_started.md) documentation.
