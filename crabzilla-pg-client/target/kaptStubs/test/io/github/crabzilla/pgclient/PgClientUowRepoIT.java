package io.github.crabzilla.pgclient;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\b\b\u0007\u0018\u0000 \u00182\u00020\u0001:\u0004\u0018\u0019\u001a\u001bB\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u0014H\u0007J\u0010\u0010\u0015\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u0014H\u0007J\u0010\u0010\u0016\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u0014H\u0007J\u0010\u0010\u0017\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u0014H\u0007R\u001a\u0010\u0003\u001a\u00020\u0004X\u0080.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\bR\u000e\u0010\t\u001a\u00020\nX\u0082.\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u000b\u001a\u00020\fX\u0080.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\r\u0010\u000e\"\u0004\b\u000f\u0010\u0010\u00a8\u0006\u001c"}, d2 = {"Lio/github/crabzilla/pgclient/PgClientUowRepoIT;", "", "()V", "repo", "Lio/github/crabzilla/vertx/UnitOfWorkRepository;", "getRepo$crabzilla_pg_client", "()Lio/github/crabzilla/vertx/UnitOfWorkRepository;", "setRepo$crabzilla_pg_client", "(Lio/github/crabzilla/vertx/UnitOfWorkRepository;)V", "vertx", "Lio/vertx/core/Vertx;", "writeDb", "Lio/reactiverse/pgclient/PgPool;", "getWriteDb$crabzilla_pg_client", "()Lio/reactiverse/pgclient/PgPool;", "setWriteDb$crabzilla_pg_client", "(Lio/reactiverse/pgclient/PgPool;)V", "a4", "", "tc", "Lio/vertx/junit5/VertxTestContext;", "a5", "s4", "setup", "Companion", "WhenAppending", "WhenSelectingByUowSeq", "WhenSelectingByVersion", "crabzilla-pg-client"})
@org.junit.jupiter.api.TestInstance(value = org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS)
@org.junit.jupiter.api.DisplayName(value = "PgClientClientUowRepo")
@org.junit.jupiter.api.extension.ExtendWith(value = {io.vertx.junit5.VertxExtension.class})
public final class PgClientUowRepoIT {
    private io.vertx.core.Vertx vertx;
    @org.jetbrains.annotations.NotNull()
    public io.reactiverse.pgclient.PgPool writeDb;
    @org.jetbrains.annotations.NotNull()
    public io.github.crabzilla.vertx.UnitOfWorkRepository repo;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String aggregateName = null;
    @org.jetbrains.annotations.NotNull()
    private static final io.github.crabzilla.example1.CustomerId customerId = null;
    @org.jetbrains.annotations.NotNull()
    private static final io.github.crabzilla.example1.CreateCustomer createCmd = null;
    @org.jetbrains.annotations.NotNull()
    private static final io.github.crabzilla.example1.CustomerCreated created = null;
    @org.jetbrains.annotations.NotNull()
    private static final io.github.crabzilla.UnitOfWork expectedUow1 = null;
    @org.jetbrains.annotations.NotNull()
    private static final io.github.crabzilla.example1.ActivateCustomer activateCmd = null;
    @org.jetbrains.annotations.NotNull()
    private static final io.github.crabzilla.example1.CustomerActivated activated = null;
    @org.jetbrains.annotations.NotNull()
    private static final io.github.crabzilla.UnitOfWork expectedUow2 = null;
    public static final io.github.crabzilla.pgclient.PgClientUowRepoIT.Companion Companion = null;
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactiverse.pgclient.PgPool getWriteDb$crabzilla_pg_client() {
        return null;
    }
    
    public final void setWriteDb$crabzilla_pg_client(@org.jetbrains.annotations.NotNull()
    io.reactiverse.pgclient.PgPool p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.github.crabzilla.vertx.UnitOfWorkRepository getRepo$crabzilla_pg_client() {
        return null;
    }
    
    public final void setRepo$crabzilla_pg_client(@org.jetbrains.annotations.NotNull()
    io.github.crabzilla.vertx.UnitOfWorkRepository p0) {
    }
    
    @org.junit.jupiter.api.BeforeEach()
    public final void setup(@org.jetbrains.annotations.NotNull()
    io.vertx.junit5.VertxTestContext tc) {
    }
    
    @org.junit.jupiter.api.DisplayName(value = "can queries an unit of work row by it\'s command id")
    @org.junit.jupiter.api.Test()
    public final void a4(@org.jetbrains.annotations.NotNull()
    io.vertx.junit5.VertxTestContext tc) {
    }
    
    @org.junit.jupiter.api.DisplayName(value = "can queries an unit of work row by it\'s uow id")
    @org.junit.jupiter.api.Test()
    public final void a5(@org.jetbrains.annotations.NotNull()
    io.vertx.junit5.VertxTestContext tc) {
    }
    
    @org.junit.jupiter.api.DisplayName(value = "can queries only above version 1")
    @org.junit.jupiter.api.Test()
    public final void s4(@org.jetbrains.annotations.NotNull()
    io.vertx.junit5.VertxTestContext tc) {
    }
    
    public PgClientUowRepoIT() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0087\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0007J\u0010\u0010\u0007\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0007J\u0010\u0010\b\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0007J\u0010\u0010\t\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0007\u00a8\u0006\n"}, d2 = {"Lio/github/crabzilla/pgclient/PgClientUowRepoIT$WhenSelectingByUowSeq;", "", "(Lio/github/crabzilla/pgclient/PgClientUowRepoIT;)V", "a1", "", "tc", "Lio/vertx/junit5/VertxTestContext;", "a2", "a3", "a4", "crabzilla-pg-client"})
    @org.junit.jupiter.api.extension.ExtendWith(value = {io.vertx.junit5.VertxExtension.class})
    @org.junit.jupiter.api.DisplayName(value = "When selecting by uow sequence")
    @org.junit.jupiter.api.Nested()
    public final class WhenSelectingByUowSeq {
        
        @org.junit.jupiter.api.DisplayName(value = "can queries an empty repo")
        @org.junit.jupiter.api.Test()
        public final void a1(@org.jetbrains.annotations.NotNull()
        io.vertx.junit5.VertxTestContext tc) {
        }
        
        @org.junit.jupiter.api.DisplayName(value = "can queries a single unit of work row")
        @org.junit.jupiter.api.Test()
        public final void a2(@org.jetbrains.annotations.NotNull()
        io.vertx.junit5.VertxTestContext tc) {
        }
        
        @org.junit.jupiter.api.DisplayName(value = "can queries two unit of work rows")
        @org.junit.jupiter.api.Test()
        public final void a3(@org.jetbrains.annotations.NotNull()
        io.vertx.junit5.VertxTestContext tc) {
        }
        
        @org.junit.jupiter.api.DisplayName(value = "can queries by uow sequence")
        @org.junit.jupiter.api.Test()
        public final void a4(@org.jetbrains.annotations.NotNull()
        io.vertx.junit5.VertxTestContext tc) {
        }
        
        public WhenSelectingByUowSeq() {
            super();
        }
    }
    
    @kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0087\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0007J\u0010\u0010\u0007\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0007J\u0010\u0010\b\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0007\u00a8\u0006\t"}, d2 = {"Lio/github/crabzilla/pgclient/PgClientUowRepoIT$WhenSelectingByVersion;", "", "(Lio/github/crabzilla/pgclient/PgClientUowRepoIT;)V", "a2", "", "tc", "Lio/vertx/junit5/VertxTestContext;", "a3", "a4", "crabzilla-pg-client"})
    @org.junit.jupiter.api.extension.ExtendWith(value = {io.vertx.junit5.VertxExtension.class})
    @org.junit.jupiter.api.DisplayName(value = "When selecting by version")
    @org.junit.jupiter.api.Nested()
    public final class WhenSelectingByVersion {
        
        @org.junit.jupiter.api.DisplayName(value = "can queries a single unit of work row")
        @org.junit.jupiter.api.Test()
        public final void a2(@org.jetbrains.annotations.NotNull()
        io.vertx.junit5.VertxTestContext tc) {
        }
        
        @org.junit.jupiter.api.DisplayName(value = "can queries two unit of work rows")
        @org.junit.jupiter.api.Test()
        public final void a3(@org.jetbrains.annotations.NotNull()
        io.vertx.junit5.VertxTestContext tc) {
        }
        
        @org.junit.jupiter.api.DisplayName(value = "can queries by version")
        @org.junit.jupiter.api.Test()
        public final void a4(@org.jetbrains.annotations.NotNull()
        io.vertx.junit5.VertxTestContext tc) {
        }
        
        public WhenSelectingByVersion() {
            super();
        }
    }
    
    @kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0087\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0007J\u0010\u0010\u0007\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0007J\u0010\u0010\b\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0007\u00a8\u0006\t"}, d2 = {"Lio/github/crabzilla/pgclient/PgClientUowRepoIT$WhenAppending;", "", "(Lio/github/crabzilla/pgclient/PgClientUowRepoIT;)V", "s1", "", "tc", "Lio/vertx/junit5/VertxTestContext;", "s2", "s3", "crabzilla-pg-client"})
    @org.junit.jupiter.api.extension.ExtendWith(value = {io.vertx.junit5.VertxExtension.class})
    @org.junit.jupiter.api.DisplayName(value = "When appending")
    @org.junit.jupiter.api.Nested()
    public final class WhenAppending {
        
        @org.junit.jupiter.api.DisplayName(value = "can append version 1")
        @org.junit.jupiter.api.Test()
        public final void s1(@org.jetbrains.annotations.NotNull()
        io.vertx.junit5.VertxTestContext tc) {
        }
        
        @org.junit.jupiter.api.DisplayName(value = "cannot append version 1 twice")
        @org.junit.jupiter.api.Test()
        public final void s2(@org.jetbrains.annotations.NotNull()
        io.vertx.junit5.VertxTestContext tc) {
        }
        
        @org.junit.jupiter.api.DisplayName(value = "can append version 1 and version 2")
        @org.junit.jupiter.api.Test()
        public final void s3(@org.jetbrains.annotations.NotNull()
        io.vertx.junit5.VertxTestContext tc) {
        }
        
        public WhenAppending() {
            super();
        }
    }
    
    @kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000D\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0011\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006R\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u0011\u0010\u000b\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0011\u0010\u000f\u001a\u00020\u0010\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0011\u0010\u0013\u001a\u00020\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u0011\u0010\u0017\u001a\u00020\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u001aR\u0011\u0010\u001b\u001a\u00020\u001c\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u001eR\u0011\u0010\u001f\u001a\u00020\u001c\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010\u001e\u00a8\u0006!"}, d2 = {"Lio/github/crabzilla/pgclient/PgClientUowRepoIT$Companion;", "", "()V", "activateCmd", "Lio/github/crabzilla/example1/ActivateCustomer;", "getActivateCmd", "()Lio/github/crabzilla/example1/ActivateCustomer;", "activated", "Lio/github/crabzilla/example1/CustomerActivated;", "getActivated", "()Lio/github/crabzilla/example1/CustomerActivated;", "aggregateName", "", "getAggregateName", "()Ljava/lang/String;", "createCmd", "Lio/github/crabzilla/example1/CreateCustomer;", "getCreateCmd", "()Lio/github/crabzilla/example1/CreateCustomer;", "created", "Lio/github/crabzilla/example1/CustomerCreated;", "getCreated", "()Lio/github/crabzilla/example1/CustomerCreated;", "customerId", "Lio/github/crabzilla/example1/CustomerId;", "getCustomerId", "()Lio/github/crabzilla/example1/CustomerId;", "expectedUow1", "Lio/github/crabzilla/UnitOfWork;", "getExpectedUow1", "()Lio/github/crabzilla/UnitOfWork;", "expectedUow2", "getExpectedUow2", "crabzilla-pg-client"})
    public static final class Companion {
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getAggregateName() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.github.crabzilla.example1.CustomerId getCustomerId() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.github.crabzilla.example1.CreateCustomer getCreateCmd() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.github.crabzilla.example1.CustomerCreated getCreated() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.github.crabzilla.UnitOfWork getExpectedUow1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.github.crabzilla.example1.ActivateCustomer getActivateCmd() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.github.crabzilla.example1.CustomerActivated getActivated() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.github.crabzilla.UnitOfWork getExpectedUow2() {
            return null;
        }
        
        private Companion() {
            super();
        }
    }
}
