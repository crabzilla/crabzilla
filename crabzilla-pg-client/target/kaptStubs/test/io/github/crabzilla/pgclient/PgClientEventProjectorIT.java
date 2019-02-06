package io.github.crabzilla.pgclient;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\b\u0007\u0018\u0000 \u00112\u00020\u0001:\u0001\u0011B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0007J\u0010\u0010\r\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0007J\u0010\u0010\u000e\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0007J\u0010\u0010\u000f\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0007J\u0010\u0010\u0010\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0007R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0012"}, d2 = {"Lio/github/crabzilla/pgclient/PgClientEventProjectorIT;", "", "()V", "eventProjector", "Lio/github/crabzilla/pgclient/PgClientEventProjector;", "readDb", "Lio/reactiverse/pgclient/PgPool;", "vertx", "Lio/vertx/core/Vertx;", "a1", "", "tc", "Lio/vertx/junit5/VertxTestContext;", "a2", "a4", "a5", "setup", "Companion", "crabzilla-pg-client"})
@org.junit.jupiter.api.TestInstance(value = org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS)
@org.junit.jupiter.api.DisplayName(value = "PgClientEventProjector")
@org.junit.jupiter.api.extension.ExtendWith(value = {io.vertx.junit5.VertxExtension.class})
public final class PgClientEventProjectorIT {
    private io.vertx.core.Vertx vertx;
    private io.reactiverse.pgclient.PgPool readDb;
    private io.github.crabzilla.pgclient.PgClientEventProjector eventProjector;
    private static final org.slf4j.Logger log = null;
    @org.jetbrains.annotations.NotNull()
    private static final io.github.crabzilla.example1.CustomerId customerId1 = null;
    @org.jetbrains.annotations.NotNull()
    private static final io.github.crabzilla.example1.CreateActivateCustomer command = null;
    @org.jetbrains.annotations.NotNull()
    private static final io.github.crabzilla.example1.CustomerCreated created1 = null;
    @org.jetbrains.annotations.NotNull()
    private static final io.github.crabzilla.example1.CustomerActivated activated1 = null;
    @org.jetbrains.annotations.NotNull()
    private static final io.github.crabzilla.example1.CustomerDeactivated deactivated1 = null;
    @org.jetbrains.annotations.NotNull()
    private static final io.github.crabzilla.example1.CustomerId customerId2 = null;
    @org.jetbrains.annotations.NotNull()
    private static final io.github.crabzilla.example1.CustomerCreated created2 = null;
    @org.jetbrains.annotations.NotNull()
    private static final io.github.crabzilla.example1.CustomerActivated activated2 = null;
    @org.jetbrains.annotations.NotNull()
    private static final io.github.crabzilla.example1.CustomerDeactivated deactivated2 = null;
    @org.jetbrains.annotations.NotNull()
    private static final io.github.crabzilla.example1.CustomerId customerId3 = null;
    @org.jetbrains.annotations.NotNull()
    private static final io.github.crabzilla.example1.CustomerCreated created3 = null;
    @org.jetbrains.annotations.NotNull()
    private static final io.github.crabzilla.example1.CustomerActivated activated3 = null;
    @org.jetbrains.annotations.NotNull()
    private static final io.github.crabzilla.example1.CustomerDeactivated deactivated3 = null;
    private static final int uowSequence1 = 1;
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.jvm.functions.Function4<io.reactiverse.pgclient.PgConnection, java.lang.Integer, io.github.crabzilla.DomainEvent, io.vertx.core.Handler<io.vertx.core.AsyncResult<java.lang.Void>>, kotlin.Unit> projectorHandler = null;
    public static final io.github.crabzilla.pgclient.PgClientEventProjectorIT.Companion Companion = null;
    
    @org.junit.jupiter.api.BeforeEach()
    public final void setup(@org.jetbrains.annotations.NotNull()
    io.vertx.junit5.VertxTestContext tc) {
    }
    
    @org.junit.jupiter.api.DisplayName(value = "can project a couple of events: created and activated")
    @org.junit.jupiter.api.Test()
    public final void a1(@org.jetbrains.annotations.NotNull()
    io.vertx.junit5.VertxTestContext tc) {
    }
    
    @org.junit.jupiter.api.DisplayName(value = "can project 3 events: created, activated and deactivated")
    @org.junit.jupiter.api.Test()
    public final void a2(@org.jetbrains.annotations.NotNull()
    io.vertx.junit5.VertxTestContext tc) {
    }
    
    @org.junit.jupiter.api.DisplayName(value = "cannot project more than 6 events within one transaction")
    @org.junit.jupiter.api.Test()
    public final void a4(@org.jetbrains.annotations.NotNull()
    io.vertx.junit5.VertxTestContext tc) {
    }
    
    @org.junit.jupiter.api.DisplayName(value = "on any any SQL error it must rollback all events projections")
    @org.junit.jupiter.api.Test()
    public final void a5(@org.jetbrains.annotations.NotNull()
    io.vertx.junit5.VertxTestContext tc) {
    }
    
    public PgClientEventProjectorIT() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000r\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0011\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006R\u0011\u0010\u0007\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\u0006R\u0011\u0010\t\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u0006R\u0011\u0010\u000b\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0011\u0010\u000f\u001a\u00020\u0010\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0011\u0010\u0013\u001a\u00020\u0010\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0012R\u0011\u0010\u0015\u001a\u00020\u0010\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0012R\u0011\u0010\u0017\u001a\u00020\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u001aR\u0011\u0010\u001b\u001a\u00020\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u001aR\u0011\u0010\u001d\u001a\u00020\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u001aR\u0011\u0010\u001f\u001a\u00020 \u00a2\u0006\b\n\u0000\u001a\u0004\b!\u0010\"R\u0011\u0010#\u001a\u00020 \u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010\"R\u0011\u0010%\u001a\u00020 \u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010\"R\u001c\u0010\'\u001a\n )*\u0004\u0018\u00010(0(X\u0080\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b*\u0010+R{\u0010,\u001al\u0012\u0013\u0012\u00110.\u00a2\u0006\f\b/\u0012\b\b0\u0012\u0004\b\b(1\u0012\u0013\u0012\u001102\u00a2\u0006\f\b/\u0012\b\b0\u0012\u0004\b\b(3\u0012\u0013\u0012\u001104\u00a2\u0006\f\b/\u0012\b\b0\u0012\u0004\b\b(5\u0012\u001f\u0012\u001d\u0012\n\u0012\b\u0012\u0004\u0012\u0002080706\u00a2\u0006\f\b/\u0012\b\b0\u0012\u0004\b\b(9\u0012\u0004\u0012\u00020:0-j\u0002`;\u00a2\u0006\b\n\u0000\u001a\u0004\b<\u0010=R\u0014\u0010>\u001a\u000202X\u0086D\u00a2\u0006\b\n\u0000\u001a\u0004\b?\u0010@\u00a8\u0006A"}, d2 = {"Lio/github/crabzilla/pgclient/PgClientEventProjectorIT$Companion;", "", "()V", "activated1", "Lio/github/crabzilla/example1/CustomerActivated;", "getActivated1", "()Lio/github/crabzilla/example1/CustomerActivated;", "activated2", "getActivated2", "activated3", "getActivated3", "command", "Lio/github/crabzilla/example1/CreateActivateCustomer;", "getCommand", "()Lio/github/crabzilla/example1/CreateActivateCustomer;", "created1", "Lio/github/crabzilla/example1/CustomerCreated;", "getCreated1", "()Lio/github/crabzilla/example1/CustomerCreated;", "created2", "getCreated2", "created3", "getCreated3", "customerId1", "Lio/github/crabzilla/example1/CustomerId;", "getCustomerId1", "()Lio/github/crabzilla/example1/CustomerId;", "customerId2", "getCustomerId2", "customerId3", "getCustomerId3", "deactivated1", "Lio/github/crabzilla/example1/CustomerDeactivated;", "getDeactivated1", "()Lio/github/crabzilla/example1/CustomerDeactivated;", "deactivated2", "getDeactivated2", "deactivated3", "getDeactivated3", "log", "Lorg/slf4j/Logger;", "kotlin.jvm.PlatformType", "getLog$crabzilla_pg_client", "()Lorg/slf4j/Logger;", "projectorHandler", "Lkotlin/Function4;", "Lio/reactiverse/pgclient/PgConnection;", "Lkotlin/ParameterName;", "name", "pgConn", "", "targetId", "Lio/github/crabzilla/DomainEvent;", "event", "Lio/vertx/core/Handler;", "Lio/vertx/core/AsyncResult;", "Ljava/lang/Void;", "future", "", "Lio/github/crabzilla/pgclient/ProjectorHandler;", "getProjectorHandler", "()Lkotlin/jvm/functions/Function4;", "uowSequence1", "getUowSequence1", "()I", "crabzilla-pg-client"})
    public static final class Companion {
        
        public final org.slf4j.Logger getLog$crabzilla_pg_client() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.github.crabzilla.example1.CustomerId getCustomerId1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.github.crabzilla.example1.CreateActivateCustomer getCommand() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.github.crabzilla.example1.CustomerCreated getCreated1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.github.crabzilla.example1.CustomerActivated getActivated1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.github.crabzilla.example1.CustomerDeactivated getDeactivated1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.github.crabzilla.example1.CustomerId getCustomerId2() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.github.crabzilla.example1.CustomerCreated getCreated2() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.github.crabzilla.example1.CustomerActivated getActivated2() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.github.crabzilla.example1.CustomerDeactivated getDeactivated2() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.github.crabzilla.example1.CustomerId getCustomerId3() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.github.crabzilla.example1.CustomerCreated getCreated3() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.github.crabzilla.example1.CustomerActivated getActivated3() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.github.crabzilla.example1.CustomerDeactivated getDeactivated3() {
            return null;
        }
        
        public final int getUowSequence1() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final kotlin.jvm.functions.Function4<io.reactiverse.pgclient.PgConnection, java.lang.Integer, io.github.crabzilla.DomainEvent, io.vertx.core.Handler<io.vertx.core.AsyncResult<java.lang.Void>>, kotlin.Unit> getProjectorHandler() {
            return null;
        }
        
        private Companion() {
            super();
        }
    }
}
