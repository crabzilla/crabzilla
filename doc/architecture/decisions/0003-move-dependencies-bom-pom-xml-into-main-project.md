# 3. move dependencies BOM pom.xml into main project

Date: 2017-08-05

## Status

Accepted

## Context

A pom.xml with bill of materials dependencies is good for any library / framework.

## Decision

The trade off here is to move BOM to the main project in order to build it on Travis CI.

## Consequences

A BOM project could be back once it is published to Maven Central. For now, the goal is to build Krabzilla on Travis
without much effort.
