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

* crabzilla-core            → Command, DomainEvent, Snapshot, etc Your domain will depend on this.
* crabzilla-example1        → An example using Crabzilla. 
* crabzilla-vertx-core      → Verticles, Repositories, etc. Your services will depend on this.
* crabzilla-vertx-web       → CommandRestVerticle. Depends on vertx-web module.

## Links

* [kotlin-example1-manual](https://crabzilla.github.io/crabzilla/docs/kotlin-example1-manual.html)
* [Architecture decision records](https://github.com/crabzilla/crabzilla/tree/master/doc/architecture/decisions)

## How to run the example

crabzilla-example1-ha has these three services:

![alt text](https://github.com/crabzilla/crabzilla/blob/master/doc/asciidoc/images/crabzilla-bc-architecture.png "crabzilla-example1-ha")

while crabzilla-example1-monolith has these 3 services embedded within just one service.

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

2. Build it, running both unit and integration tests (ports 3306 and 8080 will be used):

```bash
mvn clean install
```

3. Now you can run the **crabzilla-example1-ha**: 

```bash
cd crabzilla-example1/crabzilla-example1-services/crabzilla-example1-ha
docker-compose up
```

4. Now you can finally submit a command: 

```bash
curl -X POST \
  http://localhost:8080/customer/commands \
  -H 'content-type: application/json' \
  -d '{
  "@class" : "io.github.crabzilla.example1.customer.CreateCustomer",
  "commandId" : "b128066b-64f1-457d-8e35-3f019175468c",
  "targetId" : {
    "@class" : "io.github.crabzilla.example1.customer.CustomerId",
    "id" : "6dec4ef2-1882-4e60-9292-ef4a0cba9b06"
  },
  "name" : "6dec4ef2-1882-4e60-9292-ef4a0cba9b06"
}
'
```

### Random notes

1. Crabzilla attempts to provide a chassis for wiring and running your domain by using verticles and other components.
2. Domain Model code is agnostic about any persistence, fp or reactive frameworks. It's mostly expressed as functions.
3. If your functions are pure, all mutability is segregated to UnitOfWorkRepository and EventsProjector components.
4. As result, you will have a domain service leveraging some Vert.x power: reactive http, jdbc, rpc, distributed HA, etc.
5. Another result is the domain can be very focused and agnostic about the infrastructure and can use blocking api's.  
6. So far events from all entities are written as an UnitOfWork in Json format into a single partitioned append only table.
7. So far simplicity in order to develop domain code always wins on any trade off.

### Dependencies

I know any Java library should be very conservative about dependecy to other libraries. But these are helping a lot in Crabzilla: 

1. [jackson-module-plugin](https://github.com/FasterXML/jackson-module-kotlin) Used to ser/des polymorphic objects (commands, events, etc) 
2. [Dagger2](https://google.github.io/dagger/) It's very light, statically compiled and just works.
3. [Jdbi](http://jdbi.org/) Used to implement DAOs and repositories consumed by your domain code. 
4. [ExpiringMap](https://github.com/jhalterman/expiringmap) Used as a mechanism to plug lazy entry loading for Snapshots. This is useful for entities with lot of events.

Except for Jackson, these dependencies are used only in crabzilla-vertx and not in your domain code.

