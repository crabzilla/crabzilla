[![Build Status](https://travis-ci.org/crabzilla/crabzilla.svg?branch=master)](https://travis-ci.org/crabzilla/crabzilla)

# crabzilla

Yet another Event Sourcing experiment

[Overview](https://crabzilla.github.io/crabzilla/docs/overview.html)

[Example-manual](https://crabzilla.github.io/crabzilla/docs/vavr-example-manual.html)

[Architecture decision records](https://github.com/crabzilla/crabzilla/tree/master/doc/architecture/decisions)


## How to run the example

1. Clone Crabzilla and build it running unit tests but skipping integration tests:

```bash
git clone https://github.com/crabzilla/crabzilla
cd crabzilla
mvn clean install -DskipITs=true
```

2. Start a MySql instance. You can use docker-compose:

```bash
docker-compose up
```

3. Create the database schema using [Flyway](https://flywaydb.org/):

```bash
cd crabzilla-example1/crabzilla-example1-database
mvn compile flyway:migrate
```

4. Now you can run integration tests against database, skipping the unit tests:

```bash
# go back to crabzilla root
cd ../..
mvn verify -DskipUTs=true 
```

5. Now you finally can run the current [example](crabzilla-example1/crabzilla-example1-service/src/main/java/crabzilla/example1/Example1Launcher.java):

```bash
java -jar crabzilla-example1/crabzilla-example1-service/target/crabzilla-example1-service-1.0-SNAPSHOT-fat.jar
```

## Wiki 

You can find more info on [wiki](https://github.com/crabzilla/crabzilla/wiki)
