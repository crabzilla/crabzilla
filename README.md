[![CI](https://github.com/crabzilla/crabzilla/actions/workflows/blank.yml/badge.svg)](https://github.com/crabzilla/crabzilla/actions/workflows/blank.yml)
[![codecov](https://codecov.io/gh/crabzilla/crabzilla/branch/main/graph/badge.svg)](https://codecov.io/gh/crabzilla/crabzilla)
[![](https://www.code-inspector.com/project/24241/score/svg)](https://frontend.code-inspector.com/public/project/24241/crabzilla/dashboard)
[![](https://www.code-inspector.com/project/24241/status/svg)](https://frontend.code-inspector.com/public/project/24241/crabzilla/dashboard)
[![Jitpack](https://jitpack.io/v/io.github.crabzilla/crabzilla.svg)](https://jitpack.io/#io.github.crabzilla/crabzilla)


### Goals for version 1.0.0

- [x] Command controller
  - [X] Non-blocking IO using `vertx-pg-client`
  - [X] On Demand snapshotting
  - [X] Persistent snapshotting
  - [X] Optional Command validation
  - [X] Optional State ID locking using Postgres Advisory locks
  - [X] Optional Command persistence
  - [X] Optional synchronous Read Model projection: within the same command handler transaction
  - [X] Command Handler can have external integrations: `FutureCommandHandler`
  - [X] Correlation and causation IDs
- [x] Events publisher
  - [X] Publish events with some level of idempotency using `publications` table.
  - [X] It publishes events and it's metadata as plain JSON to Vertx EventBus using request reply semantic
- [x] Events projector
  - [X] Publish events with some level of idempotency using `projections` table.
  - [X] It publishes events and it's metadata as plain JSON to Vertx EventBus using request reply semantic 
- [x] Very basic observability (logging stats)
- [ ] Getting started tutorial
- [ ] Web interface to write model (to track commands, events, etc)

### Status

It's still in very early development stage. 

### Getting started

1. Add the JitPack repository to your build file:

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
```

1. Add `crabzilla-core` to write Commands, Events, States and TestSpecifications. 

```xml
<dependency>
    <groupId>com.github.crabzilla.crabzilla</groupId>
    <artifactId>crabzilla-core</artifactId>
    <version>see latest jitpack version</version>
</dependency>
```

2. Add `crabzilla-serder-kotlin` to serialize/deserialize your Commands, Events and States to JSON.

```xml
<dependency>
    <groupId>com.github.crabzilla.crabzilla</groupId>
    <artifactId>crabzilla-core</artifactId>
    <version>see latest jitpack version</version>
</dependency>
```

3. Add `crabzilla-engine-postgres` to consistently append your events to Postgres.

```xml
<dependency>
    <groupId>com.github.crabzilla.crabzilla</groupId>
    <artifactId>crabzilla-engine-postgres</artifactId>
    <version>see latest jitpack version</version>
</dependency>
```

### Building

###### Requirements

* Java 8
* Maven (tested with 3.5.0+)
* Docker compose (tested with 1.18.0)
* Kotlin plugin for your IDE

###### Steps

1. Clone it:

```bash
git clone https://github.com/crabzilla/crabzilla
cd crabzilla
```

2. Start docker-compose running a Postgres database (port 5432 will be used):

```bash
docker-compose up
```

3. Open another terminal and build it, running both unit and integration tests:

```bash
mvn clean install -DskipTests=false
```

## Integration overview 

![GitHub Logo](/doc/crabzilla-integration.png)

## Dependencies overview (outdated)

![GitHub Logo](/crabzilla-packages.png)
