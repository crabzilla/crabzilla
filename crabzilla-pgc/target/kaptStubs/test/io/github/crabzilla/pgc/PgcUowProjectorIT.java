package io.github.crabzilla.pgc;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\n\b\u0007\u0018\u0000 \u00152\u00020\u0001:\u0001\u0015B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0007J\u0010\u0010\r\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0007J\u0010\u0010\u000e\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0007J\u0010\u0010\u000f\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0007J\u0010\u0010\u0010\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0007J\u0010\u0010\u0011\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0007J\u0010\u0010\u0012\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0007J\u0010\u0010\u0013\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0007J\u0010\u0010\u0014\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0007R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0016"}, d2 = {"Lio/github/crabzilla/pgc/PgcUowProjectorIT;", "", "()V", "readDb", "Lio/reactiverse/pgclient/PgPool;", "uowProjector", "Lio/github/crabzilla/pgc/PgcUowProjector;", "vertx", "Lio/vertx/core/Vertx;", "a1", "", "tc", "Lio/vertx/junit5/VertxTestContext;", "a10", "a2", "a3", "a4", "a5", "a6", "a7", "setup", "Companion", "crabzilla-pgc"})
@org.junit.jupiter.api.TestInstance(value = org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS)
@org.junit.jupiter.api.extension.ExtendWith(value = {io.vertx.junit5.VertxExtension.class})
public final class PgcUowProjectorIT {
    private io.vertx.core.Vertx vertx;
    private io.reactiverse.pgclient.PgPool readDb;
    private io.github.crabzilla.pgc.PgcUowProjector uowProjector;
    private static final org.slf4j.Logger log = null;
    public static final io.github.crabzilla.pgc.PgcUowProjectorIT.Companion Companion = null;
    
    @org.junit.jupiter.api.BeforeEach()
    public final void setup(@org.jetbrains.annotations.NotNull()
    io.vertx.junit5.VertxTestContext tc) {
    }
    
    @org.junit.jupiter.api.DisplayName(value = "can project 1 event")
    @org.junit.jupiter.api.Test()
    public final void a1(@org.jetbrains.annotations.NotNull()
    io.vertx.junit5.VertxTestContext tc) {
    }
    
    @org.junit.jupiter.api.DisplayName(value = "can project 2 events: created and activated")
    @org.junit.jupiter.api.Test()
    public final void a2(@org.jetbrains.annotations.NotNull()
    io.vertx.junit5.VertxTestContext tc) {
    }
    
    @org.junit.jupiter.api.DisplayName(value = "can project 3 events: created, activated and deactivated")
    @org.junit.jupiter.api.Test()
    public final void a3(@org.jetbrains.annotations.NotNull()
    io.vertx.junit5.VertxTestContext tc) {
    }
    
    @org.junit.jupiter.api.DisplayName(value = "can project 4 events: created, activated, deactivated, activated")
    @org.junit.jupiter.api.Test()
    public final void a4(@org.jetbrains.annotations.NotNull()
    io.vertx.junit5.VertxTestContext tc) {
    }
    
    @org.junit.jupiter.api.DisplayName(value = "can project 5 events: created, activated, deactivated, activated, deactivated")
    @org.junit.jupiter.api.Test()
    public final void a5(@org.jetbrains.annotations.NotNull()
    io.vertx.junit5.VertxTestContext tc) {
    }
    
    @org.junit.jupiter.api.DisplayName(value = "can project 6 events: created, activated, deactivated, activated, deactivated")
    @org.junit.jupiter.api.Test()
    public final void a6(@org.jetbrains.annotations.NotNull()
    io.vertx.junit5.VertxTestContext tc) {
    }
    
    @org.junit.jupiter.api.DisplayName(value = "cannot project more than 6 events within one transaction")
    @org.junit.jupiter.api.Test()
    public final void a7(@org.jetbrains.annotations.NotNull()
    io.vertx.junit5.VertxTestContext tc) {
    }
    
    @org.junit.jupiter.api.DisplayName(value = "on any any SQL error it must rollback all events projections")
    @org.junit.jupiter.api.Test()
    public final void a10(@org.jetbrains.annotations.NotNull()
    io.vertx.junit5.VertxTestContext tc) {
    }
    
    public PgcUowProjectorIT() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u001c\u0010\u0003\u001a\n \u0005*\u0004\u0018\u00010\u00040\u0004X\u0080\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007\u00a8\u0006\b"}, d2 = {"Lio/github/crabzilla/pgc/PgcUowProjectorIT$Companion;", "", "()V", "log", "Lorg/slf4j/Logger;", "kotlin.jvm.PlatformType", "getLog$crabzilla_pgc", "()Lorg/slf4j/Logger;", "crabzilla-pgc"})
    public static final class Companion {
        
        public final org.slf4j.Logger getLog$crabzilla_pgc() {
            return null;
        }
        
        private Companion() {
            super();
        }
    }
}
