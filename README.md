[![Build Status](https://travis-ci.org/crabzilla/crabzilla.svg?branch=master)](https://travis-ci.org/crabzilla/crabzilla)
[![codecov](https://codecov.io/gh/crabzilla/crabzilla/branch/master/graph/badge.svg)](https://codecov.io/gh/crabzilla/crabzilla)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.crabzilla/crabzilla/badge.svg)](http://search.maven.org/#artifactdetails%7Cio.github.crabzilla%7Ccrabzilla%7C0.0.5%7C)

# crabzilla

## Goal for version 1.0.0

To help you write your domain model with very little framework overhead and smoothly put it to work using a stack based
on Vert.x and a relational database of your choice.

## Status

Its still in very early development stage. APIs can change. So far Crabzilla has only an Aggregate example. Inspired by 
[Eventstorming](http://eventstorming.com), the goal is to develop examples and implementations for an ExternalSystem, 
a Listener and a ProcessManager / Saga.

## Modules

* **crabzilla-core**            → Command, DomainEvent, Aggregate, Snapshot, etc Your domain will depend on it.
* **crabzilla-example1**        → An example using Crabzilla. 
* **crabzilla-vertx-core**      → Codecs, Helpers, etc. Your services will depend on it.
* **crabzilla-vertx-handler**   → CommandHandlerVerticles and UnitOfWorkRepositories.
* **crabzilla-vertx-projector** → ProjectionHandlerVerticle, EventsProjector and ProjectionRepository.
* **crabzilla-vertx-web**       → CommandRestVerticle. Depends on vertx-web module.
* **crabzilla-vertx-dagger**    → Useful modules definitions you can optionally leverage.

## Links

* [kotlin-example1-manual](https://crabzilla.github.io/crabzilla/docs/kotlin-example1-manual.html)
* [Architecture decision records](https://github.com/crabzilla/crabzilla/tree/master/doc/architecture/decisions)

## How to run the example

### Requirements

* Java 8
* Maven
* Docker compose
* Kotlin plugin for your IDE

### Steps

1. Clone it:

```bash
git clone https://github.com/crabzilla/crabzilla
cd crabzilla
```

2. Start a MySql instance. You can use docker-compose:

```bash
docker-compose up
```

3. Build it, running both unit and integration tests against database:

```bash
mvn clean install
```

4. Now you can run the **crabzilla-example1-dual-db**: 

```bash
cd crabzilla-example1/crabzilla-example1-services/crabzilla-example1-dual-db
```

5. Now you can run the command handler service: 

```bash
cd crabzilla-example1-dual-db-handler
java -jar target/crabzilla-example1-dual-db-handler-0.0.6-SNAPSHOT-fat.jar \
     -conf target/classes/conf/config.properties

```

6. Finally you can also run the events projection service:

```bash
cd crabzilla-example1-dual-db-projector
java -jar target/crabzilla-example1-dual-db-projector-0.0.6-SNAPSHOT-fat.jar \
     -conf target/classes/conf/config.properties

```

