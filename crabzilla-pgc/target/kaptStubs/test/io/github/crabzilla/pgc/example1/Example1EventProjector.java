package io.github.crabzilla.pgc.example1;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J&\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u00072\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eH\u0016R\u0016\u0010\u0003\u001a\n \u0005*\u0004\u0018\u00010\u00040\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000f"}, d2 = {"Lio/github/crabzilla/pgc/example1/Example1EventProjector;", "Lio/github/crabzilla/pgc/PgcEventProjector;", "()V", "log", "Lorg/slf4j/Logger;", "kotlin.jvm.PlatformType", "handle", "Lio/vertx/core/Future;", "Ljava/lang/Void;", "pgConn", "Lio/reactiverse/pgclient/PgTransaction;", "targetId", "", "event", "Lio/github/crabzilla/DomainEvent;", "crabzilla-pgc"})
public final class Example1EventProjector implements io.github.crabzilla.pgc.PgcEventProjector {
    private final org.slf4j.Logger log = null;
    
    @org.jetbrains.annotations.NotNull()
    @java.lang.Override()
    public io.vertx.core.Future<java.lang.Void> handle(@org.jetbrains.annotations.NotNull()
    io.reactiverse.pgclient.PgTransaction pgConn, int targetId, @org.jetbrains.annotations.NotNull()
    io.github.crabzilla.DomainEvent event) {
        return null;
    }
    
    public Example1EventProjector() {
        super();
    }
}
