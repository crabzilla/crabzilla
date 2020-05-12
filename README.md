[![Build Status](https://travis-ci.org/crabzilla/crabzilla.svg?branch=master)](https://travis-ci.org/crabzilla/crabzilla)
[![codecov](https://codecov.io/gh/crabzilla/crabzilla/branch/master/graph/badge.svg)](https://codecov.io/gh/crabzilla/crabzilla)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.crabzilla/crabzilla/badge.svg)](http://search.maven.org/#artifactdetails%7Cio.github.crabzilla%7Ccrabzilla%7C0.0.5%7C)

# crabzilla

## Goal for version 1.0.0

To help writing CQRS and Event Sourcing applications with [Vert.x](http://vertx.io/)

## Status

Its still in very early development stage. Do not use release 0.0.5, master branch is very different from that.

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
mvn clean install -DskipTests=false
```

### Random notes

1. Crabzilla tries to provide a chassis for wiring and running your domain by using verticles and other components.
2. If your functions are pure, all side effects will occurs within UnitOfWorkJournal and EventsProjector components.
3. So far events from all entities are written as an UnitOfWork in Json format into a single partitioned append only table.



