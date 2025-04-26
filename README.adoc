:sourcedir: src/main/java
:source-highlighter: highlightjs
:highlightjsdir: highlight
:highlightjs-theme: rainbow
:revnumber: {project-version}
:example-caption!:
ifndef::imagesdir[:imagesdir: images]
ifndef::sourcedir[:sourcedir: ../../main/java]
:toclevels: 4


https://www.oracle.com/java/[image:https://img.shields.io/badge/Java-17-purple.svg[Vertx]]
https://kotlinlang.org/[image:https://img.shields.io/badge/Kotlin-1.9.22-purple.svg[Vertx]]
https://vertx.io[image:https://img.shields.io/badge/vert.x-4.5.1-purple.svg[Vertx]]
https://github.com/crabzilla/crabzilla/actions/workflows/blank.yml[image:https://github.com/crabzilla/crabzilla/actions/workflows/blank.yml/badge.svg[CI]]
https://codecov.io/gh/crabzilla/crabzilla[image:https://codecov.io/gh/crabzilla/crabzilla/branch/main/graph/badge.svg[codecov]]
https://jitpack.io/#io.github.crabzilla/crabzilla[image:https://jitpack.io/v/io.github.crabzilla/crabzilla.svg[Jitpack]]

== 2025 update: it's now deprecated
I decided to start another experiment: https://github.com/rodolfodpk/crablet. 
Anyway, I have learned a lot when developing it. 

== Objectives

. Allows writing your testable `CQRS/ES` model with minimal dependencies.
. Allows consistently handling your commands and save resulting events into `Postgres`.
. Allows consistently and ordered projecting your events to your view models in `Postgres`.
. Allows consistently and ordered publishing your events to Vertx eventbus, so you can integrate with any message broker, database, cache, etc.

== Approach

* Using https://vertx.io/docs/vertx-pg-client/java/[Vertx] `non-blocking IO` Postgres driver.
* Using key `Postgres` features: json, notification and advisory locks.
* Trying to be idiomatic Kotlin: type safeness: immutability, pattern matching, honouring your constructors.

== crabzilla-core

To express your domain model.

* State transitions occurs after computing results of pure functions `(State, Event) -> State`.
* Tests in BDD way without any dependency to Vertx: only to `crabzilla-core`:
** given command, then assertion
** given events, when command, then assertion
** given commands or events in any order, then assertion

== WIP

