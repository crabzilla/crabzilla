[![Build Status](https://travis-ci.org/crabzilla/crabzilla.svg?branch=master)](https://travis-ci.org/crabzilla/crabzilla)
[![codecov](https://codecov.io/gh/crabzilla/crabzilla/branch/master/graph/badge.svg)](https://codecov.io/gh/crabzilla/crabzilla)
[![](https://jitpack.io/v/io.github.crabzilla/crabzilla.svg)](https://jitpack.io/#io.github.crabzilla/crabzilla)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.crabzilla/crabzilla/badge.svg)](http://search.maven.org/#artifactdetails%7Cio.github.crabzilla%7Ccrabzilla%7C0.0.5%7C)


### Goals for version 1.0.0

- [x] Command controller
- [x] Events publisher
- [ ] Getting started tutorial
- [ ] Observability (publishing stats to eventbus)
- [ ] Web interface to write model (to track commands, events, etc)

### Status

It's still in very early development stage. Do not use release 0.0.5, master branch is very different from that.

### Getting started

1. Add the JitPack repository to your build file:

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
```

2. Add the dependency:

```xml
<dependency>
    <groupId>com.github.User</groupId>
    <artifactId>crabzilla-pgc</artifactId>
    <version>v0.1.3</version>
</dependency>
```

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

## Dependencies overview

![GitHub Logo](/crabzilla-packages.png)



