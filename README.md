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

## References

1. To know more about CQRS, please read [this](https://gist.github.com/kellabyte/1964094) 
2. To know more about Event Sourcing, please read [Event Sourcing in practice](https://ookami86.github.io/event-sourcing-in-practice/#title.md)

## TODOs

1. To explain trade offs for [these practical problems](https://ookami86.github.io/event-sourcing-in-practice/#making-eventsourcing-work/01-issues-in-practice.md)
