# 2. Replace Caffeine cache lib with ExpiringMap

Date: 2017-08-05

## Status

Accepted

## Context

Initially, the main requirement was: (local) cache must provide an easy way to plug a String -> Snapshot<E> as a lazy
way to load an entry from a database, service, etc and an efficient TTL policy.

[Caffeine](https://github.com/ben-manes/caffeine) is perfect for this. The native Vert.x option for a local map is just
a ConcurrentMap without any expiring policy or enforced size limit. Another option would be to use
[AsyncMap with Hazelcast](http://vertx.io/docs/vertx-hazelcast/java/#_using_hazelcast_async_methods) but I noticed I
would need to run my unit tests with a clustered Vert.x instance and also to configure TTL in Hazelcast xml.

## Decision

The decision is to avoid cache being aware from lazy load from a database, etc. When the cache doesn't contain an entry,
it will just return null. The verticle consuming it will then try to load an Snapshot instance from another Krabzilla
component. This component can a DAO like EntityUnitOfWorkRepository, so can be executed within the event loop.

Since now the cache responsibility is lower, [ExpiringMap](https://github.com/jhalterman/expiringmap) can be used since
it's simpler and enough for this use case and also very smaller than Caffeine.

## Consequences

1) To open an issue to create new SnapshotLoader component.
2) [Issue 20](https://github.com/crabzilla/crabzilla/issues/20) can be closed.
3) [Vert.x Issue 282](https://github.com/vert-x3/issues/issues/282) can be closed.



