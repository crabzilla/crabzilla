[![Build Status](https://travis-ci.org/crabzilla/crabzilla.svg?branch=master)](https://travis-ci.org/crabzilla/crabzilla)
[![codecov](https://codecov.io/gh/crabzilla/crabzilla/branch/master/graph/badge.svg)](https://codecov.io/gh/crabzilla/crabzilla)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.crabzilla/crabzilla/badge.svg)](http://search.maven.org/#artifactdetails%7Cio.github.crabzilla%7Ccrabzilla%7C0.0.5%7C)

# crabzilla

## Update: February 2021

After many months, I'm back active working on this project. I just got a job to work with Kotlin, so I'm motivated again. These are some plans:

* Dependencies upgrade (Vert.x, Kotlin, etc)
* Refactor unit of work table: splitting it into 2 tables: 1 for events and another for commands - The current model doesn't have a true event store. Also: the events table will have only one event per row instead of a json with N events given a command. The command table will have reference to the respective events. This will allow to correct events within the event store.
* I will keep with Kotlin Serialization but still wondering about an approach I've used before: https://einarwh.wordpress.com/2020/05/08/on-the-complexity-of-json-serialization/
* I will avoid trying integrations with RabbitMq, Hazelcast, etc like I did before: it isn't within the scope of Crabzilla.
* I will rewrite Accounts example app with Quarkus (I'm just waiting Quarkus to reach Vertx 4
* May be the adoption of some parts of https://arrow-kt.io/ Using optics instead of plain Kotlin copy for aggregates, for example.

### Goal for version 1.0.0

To help writing CQRS and Event Sourcing applications with [Vert.x](http://vertx.io/)

### Status

It's still in very early development stage. Do not use release 0.0.5, master branch is very different from that.

### Example
* [Accounts Example](https://github.com/crabzilla/accounts)

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

## Random notes

1. Crabzilla tries to provide a chassis for wiring and running your domain by using plain verticles.
2. If your command handling functions are pure, all side effects will occurs within:
    * core.UnitOfWorkJournal: to save the events to a database using optimistic concurrency control
    * pgc.PgcUnitOfWorkProjector: to project events to a read model using a PostgreSql database using vertx-pg-client
    * ~~jooq.JooqUowProjector: to project events to a read model using vertx-jooq-classic-reactive~~
3. So far events from all entities are written as an UnitOfWork in Json format into a single partitioned append only table.



