[![Build Status](https://travis-ci.org/crabzilla/crabzilla.svg?branch=master)](https://travis-ci.org/crabzilla/crabzilla)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.crabzilla/crabzilla/badge.svg)](http://search.maven.org/#artifactdetails%7Cio.github.crabzilla%7Ccrabzilla%7C0.0.5%7C)

# crabzilla

* [Overview](https://crabzilla.github.io/crabzilla/docs/overview.html)
* [kotlin-example1-manual](https://crabzilla.github.io/crabzilla/docs/kotlin-example1-manual.html)
* [Architecture decision records](https://github.com/crabzilla/crabzilla/tree/master/doc/architecture/decisions)

## Maven

Your core domain module must import:

```xml
<dependency>
  <groupId>io.github.crabzilla</groupId>
  <artifactId>crabzilla-core</artifactId>
  <version>0.0.6-SNAPSHOT</version>
</dependency>
```

and your service module must import:

```xml
<dependency>
  <groupId>io.github.crabzilla</groupId>
  <artifactId>crabzilla-vertx</artifactId>
  <version>0.0.6-SNAPSHOT</version>
</dependency>
```

## Archetypes

* TODO

## How to run the example

### Requirements

* Java 8
* Maven
* Docker compose
* Kotlin plugin for your IDE

### Steps

1. Clone it:

```bash
git clone https://github.com/crabzilla/crabzilla
cd crabzilla
```

2. Start a MySql instance. You can use docker-compose:

```bash
docker-compose up
```

3. Now you can run both unit and integration tests against database:

```bash
mvn clean install
```

4. Now you finally can run the current [example1](crabzilla-example1/crabzilla-example1-service/src/main/java/io/github/crabzilla/example1/Example1Launcher.kt):

```bash
cd crabzilla-example1/crabzilla-example1-service
java -jar target/crabzilla-example1-service-0.0.6-SNAPSHOT-fat.jar -conf target/classes/conf/config.properties

```
 
## License

Copyright © 2017 Rodolfo de Paula

Distributed under Apache License 2.0.