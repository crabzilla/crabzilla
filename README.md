[![Build Status](https://travis-ci.org/crabzilla/crabzilla.svg?branch=master)](https://travis-ci.org/crabzilla/crabzilla)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.crabzilla/crabzilla/badge.svg)](http://search.maven.org/#artifactdetails%7Cio.github.crabzilla%7Ccrabzilla%7C0.0.5%7C)

# crabzilla

* [Overview](https://crabzilla.github.io/crabzilla/docs/overview.html)
* [kotlin-example1-manual](https://crabzilla.github.io/crabzilla/docs/kotlin-example1-manual.html)
* [Architecture decision records](https://github.com/crabzilla/crabzilla/tree/master/doc/architecture/decisions)

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

3. Build it, running both unit and integration tests against database:

```bash
mvn clean install
```

4. Now you can run the [command handler example](crabzilla-example1/crabzilla-example1-handler-service/src/main/java/io/github/crabzilla/example1/HandlerServiceLauncher.kt):

```bash
cd crabzilla-example1/crabzilla-example1-handler-service
java -jar target/crabzilla-example1-handler-service-0.0.6-SNAPSHOT-fat.jar -conf target/classes/conf/config.properties

```

5. Finally you can also run the [events projection example](crabzilla-example1/crabzilla-example1-projector-service/src/main/java/io/github/crabzilla/example1/ProjectorServiceLauncher.kt):

```bash
cd crabzilla-example1/crabzilla-example1-projector-service
java -jar target/crabzilla-example1-projector-service-0.0.6-SNAPSHOT-fat.jar -conf target/classes/conf/config.properties

```

## Maven

Your core domain module must import:

```xml
<dependency>
  <groupId>io.github.crabzilla</groupId>
  <artifactId>crabzilla-core</artifactId>
  <version>0.0.6-SNAPSHOT</version>
</dependency>
```

and your command handler service module must import:

```xml
<dependency>
  <groupId>io.github.crabzilla</groupId>
  <artifactId>crabzilla-vertx-handler</artifactId>
  <version>0.0.6-SNAPSHOT</version>
</dependency>
```

and your events projector service module must import:

```xml
<dependency>
  <groupId>io.github.crabzilla</groupId>
  <artifactId>crabzilla-vertx-projector</artifactId>
  <version>0.0.6-SNAPSHOT</version>
</dependency>
```

and if you want to expose you command handlers to the web you may want to import:

```xml
<dependency>
  <groupId>io.github.crabzilla</groupId>
  <artifactId>crabzilla-vertx-web</artifactId>
  <version>0.0.6-SNAPSHOT</version>
</dependency>
```

## Archetypes

* TODO

 
 
## License

Copyright © 2017 Rodolfo de Paula

Distributed under Apache License 2.0.