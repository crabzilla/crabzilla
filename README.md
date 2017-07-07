# crabzilla 

Yet another Event Sourcing experiment

## Status

Currently it's just some code with poor test coverage but the "~~dirty~~ explorations phase" is probably done. I think the future is more predictable: to write tests, documents, refactor, etc 

## Goal for version 1.0.0

It has an ambitious goal: to help you write your domain model with very little framework overhead and smoothly deploy it on a state of art Java 8 reactive applications platform backed by a rock solid relational database of your choice.

## How

The approach is to use functions everywhere within your domain. Ideally your domain model code will be very testable and side effect free. Then you will be able to deploy your domain model into a reactive engine built with Vertx. This engine provides verticles and components for the full CQRS / Events Sourcing lifecycle. 

## What

Here are some of them:  

1. A REST verticle to receive commands 

2. An eventbus consumer to handle commands. It will invoke your domain function with business code. There are samples using Vavr pattern matching or you can extend some very simple abstract classes from crabzila.stack package. An interesting aspect: since your domain code is side effect free (well, except the EventsProjector), the side effects related to command handling will occurs within this verticle. Isolating your side effects is a goal of Functional  Programming.   

3. An event store implementation. The current implementation is based on a relational database. Others may be implemented in the future but right now the goal is to help you to develop and deploy your domain with a very simple (but robust) software stack. The current example is based on MYSQL using JSON columns. 

4. An eventbus consumer to handle events projection to the read model database. Current example is using JOOQ.

Version 1.0.0 scope also has other components covering features for sagas (or process managers) and command scheduling. 

## Reactive

All command handling i/o (http, jdbc) is using reactive apis from Vertx. You don't need to use reactive apis within your domain code to, for example, to call external services. You can let your domain code very simple and testable / mockable but even so you will achieve a much better performance and resilience. The only pieces of your code that will have side effects are those related to the projection of domain events to your read model.

## Getting started


1. Build it running unit tests but skipping integration tests:

```bash
cd crabzilla
mvn clean install -DskipITs=true
```

1. Starts a MySql instance. You can use docker-compose:

```bash
docker-compose build
docker-compose up
```

2. Create the database schema using [Flyway](https://flywaydb.org/):

```bash
cd crabzilla-example1/crabzilla-example1-database
mvn compile flyway:migrate
```

3. Optionally, you may want to regenerate classes reflecting the database using [JOOQ](https://www.jooq.org/)

```bash
mvn clean compile -P jooq
```

4. Now you can run integration tests against database, skipping the unit tests:

```bash
# go back to crabzilla root
cd ../..
mvn verify -DskipUTs=true 
```

5. Now you finally can run the example:

```bash
java -jar crabzilla-vertx-example1/target/crabzilla-vertx-example1-1.0-SNAPSHOT-fat.jar 
```

## References

1. To know more about CQRS, please read [this](https://gist.github.com/kellabyte/1964094) 
2. To know more about Event Sourcing, please read [Event Sourcing in practice](https://ookami86.github.io/event-sourcing-in-practice/#title.md)

## Trade offs 

[Greg Young: Don't write a new CQRS ES framework! This is not a framework: it's a reference application.](https://www.youtube.com/watch?v=LDW0QWie21s)

Since Crabzilla was not built to solve any specific business problem, let's check how it deal with [these practical problems](https://ookami86.github.io/event-sourcing-in-practice/#making-eventsourcing-work/01-issues-in-practice.md):

1. [Problem: Confusing Event Sourcing and Command Sourcing](https://ookami86.github.io/event-sourcing-in-practice/#making-eventsourcing-work/02-confusing-event-sourcing-with-command-sourcing.md)

Crabzilla is an Event Sourcing framework.

2. [Problem: Side-effects](https://ookami86.github.io/event-sourcing-in-practice/#slide-38)

Crabzilla approach is based on the first solution of this slide: "Separate side-effect and state change". This allows you to apply past events in order to build your aggregate root state (forming a snapshot) without triggering any side effect, even if the services used by your aggregate root have side effects. The [VertxCommandHandlerVerticle]() is where all cache and database side effects occurs when a command is handled. The exception is when the services used by your aggregate root have side effects. In this case you are responsible to manage idempotent processing or even doing rollbacks on side effects triggered by services.

3. [Problem: Reporting & Queries](https://ookami86.github.io/event-sourcing-in-practice/#slide-42)

Crabzilla has an example where a query model (or read model) is populated by an implementation of [EventsProjector](crabzilla-vertx-example1/src/main/java/crabzilla/example1/Example1EventProjector.java)

4. [Problem: Evolving events](https://ookami86.github.io/event-sourcing-in-practice/#slide-51)

Crabzilla approach is based on the third solution of the slide: Snapshotting. More specifically, before going into the database to scan for new events, Crabzilla will load the current [Snapshot](crabzilla-core/src/main/java/crabzilla/model/Snapshot.java) from a [Caffeine](https://github.com/ben-manes/caffeine) cache. You can load the Snapshot from a pre calculated table or from all raw events data.

5. [Problem: Concurrent writes](https://ookami86.github.io/event-sourcing-in-practice/#making-eventsourcing-work/18-concurrent-modifictations.md)

[VertxUnitOfWorkRepository](crabzilla-vertx/src/main/java/crabzilla/vertx/repositories/VertxUnitOfWorkRepository.java) uses optimistic locking when appending UnitOfWork instances. 


