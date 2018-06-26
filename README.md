[![Build Status](https://travis-ci.org/crabzilla/crabzilla.svg?branch=master)](https://travis-ci.org/crabzilla/crabzilla)
[![codecov](https://codecov.io/gh/crabzilla/crabzilla/branch/master/graph/badge.svg)](https://codecov.io/gh/crabzilla/crabzilla)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.crabzilla/crabzilla/badge.svg)](http://search.maven.org/#artifactdetails%7Cio.github.crabzilla%7Ccrabzilla%7C0.0.5%7C)

# crabzilla

## Goal for version 1.0.0

To help you write your domain model with very little framework overhead and smoothly put it to work using a stack based
on [Vert.x](http://vertx.io/) and a relational database of your choice.

## Status

Its still in very early development stage. APIs can change. So far Crabzilla has only an Aggregate example. Inspired by 
[Eventstorming](http://eventstorming.com), the goal is to develop examples and implementations for an ExternalSystem, 
a Listener and a ProcessManager / Saga. Do not use release 0.0.5, master branch is very different from that. 

## Modules

* crabzilla-core      → Schema, Serialization, Verticles, Repositories, etc. Your model will depend on this.
* crabzilla-pg-client → Implementation for an UnitOfWorkRepo and EventsProjector.
* crabzilla-web       → HealthVerticle or RestVerticle. Depends on vertx-web module. 

## Links (deprecated)

* [kotlin-example1-manual](https://crabzilla.github.io/crabzilla/docs/kotlin-example1-manual.html)
* [Architecture decision records](https://github.com/crabzilla/crabzilla/tree/master/doc/architecture/decisions)

### Requirements

* Java 8
* Maven (tested with 3.5.0+)
* Docker compose (tested with 1.18.0)
* Kotlin plugin for your IDE

### Steps

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
mvn clean install
```

### Random notes

1. Crabzilla tries to provide a chassis for wiring and running your domain by using verticles and other components.
2. If your functions are pure, all side effects will occurs within UnitOfWorkRepository and EventsProjector components.
3. As result, you will have a domain service leveraging some Vert.x power: reactive http, jdbc, rpc, distributed HA, etc.
4. So far events from all entities are written as an UnitOfWork in Json format into a single partitioned append only table.
5. So far simplicity in order to develop domain code always wins on any trade off.
6. Another concern is to develop modularized solutions within a monolith and then, eventually and only if needed, to seamless break it into smaller services. See the 2 examples: crabzilla-example1-monolith and crabzilla-example1-ha.
7. So far it's using "classical" Vertx apis. I do plan to eventually rewrite some code using RxJava or Kotlin corroutines.

### Dependencies

I know any Java library should be very conservative about dependency to other libraries. But these are helping a lot in Crabzilla: 

1. [jackson-kotlin-plugin](https://github.com/FasterXML/jackson-module-kotlin) Used to ser/des polymorphic objects (commands, events, etc) 
2. [ExpiringMap](https://github.com/jhalterman/expiringmap) Used as a mechanism to plug lazy entry loading of Snapshots. This is useful for entities with lot of events.


