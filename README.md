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

