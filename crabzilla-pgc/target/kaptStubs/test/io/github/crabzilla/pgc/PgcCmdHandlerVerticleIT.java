package io.github.crabzilla.pgc;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000fH\u0007J\u0010\u0010\u0010\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000fH\u0007J\u0010\u0010\u0011\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000fH\u0007J\u0010\u0010\u0012\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000fH\u0007R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Lio/github/crabzilla/pgc/PgcCmdHandlerVerticleIT;", "", "()V", "options", "Lio/vertx/core/eventbus/DeliveryOptions;", "verticle", "Lio/github/crabzilla/pgc/PgcCmdHandlerVerticle;", "Lio/github/crabzilla/example1/Customer;", "vertx", "Lio/vertx/core/Vertx;", "writeDb", "Lio/reactiverse/pgclient/PgPool;", "a1", "", "tc", "Lio/vertx/junit5/VertxTestContext;", "a2", "a3", "setup", "crabzilla-pgc"})
@org.junit.jupiter.api.TestInstance(value = org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS)
@org.junit.jupiter.api.extension.ExtendWith(value = {io.vertx.junit5.VertxExtension.class})
public final class PgcCmdHandlerVerticleIT {
    private io.vertx.core.Vertx vertx;
    private io.reactiverse.pgclient.PgPool writeDb;
    private io.github.crabzilla.pgc.PgcCmdHandlerVerticle<io.github.crabzilla.example1.Customer> verticle;
    private final io.vertx.core.eventbus.DeliveryOptions options = null;
    
    @org.junit.jupiter.api.BeforeEach()
    public final void setup(@org.jetbrains.annotations.NotNull()
    io.vertx.junit5.VertxTestContext tc) {
    }
    
    @org.junit.jupiter.api.DisplayName(value = "given a valid command it will be SUCCESS")
    @org.junit.jupiter.api.Test()
    public final void a1(@org.jetbrains.annotations.NotNull()
    io.vertx.junit5.VertxTestContext tc) {
    }
    
    @org.junit.jupiter.api.DisplayName(value = "given an invalid command it will be VALIDATION_ERROR")
    @org.junit.jupiter.api.Test()
    public final void a2(@org.jetbrains.annotations.NotNull()
    io.vertx.junit5.VertxTestContext tc) {
    }
    
    @org.junit.jupiter.api.DisplayName(value = "given an execution error it will be HANDLING_ERROR")
    @org.junit.jupiter.api.Test()
    public final void a3(@org.jetbrains.annotations.NotNull()
    io.vertx.junit5.VertxTestContext tc) {
    }
    
    public PgcCmdHandlerVerticleIT() {
        super();
    }
}
