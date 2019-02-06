package io.github.crabzilla.pgclient;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000b\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010 \n\u0002\b\u0003\u0018\u0000  2\u00020\u0001:\u0001 B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u008e\u0001\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2p\u0010\r\u001al\u0012\u0013\u0012\u00110\u000f\u00a2\u0006\f\b\u0010\u0012\b\b\u0004\u0012\u0004\b\b(\u0011\u0012\u0013\u0012\u00110\u0012\u00a2\u0006\f\b\u0010\u0012\b\b\u0004\u0012\u0004\b\b(\u0013\u0012\u0013\u0012\u00110\u0014\u00a2\u0006\f\b\u0010\u0012\b\b\u0004\u0012\u0004\b\b(\u0015\u0012\u001f\u0012\u001d\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00180\u00170\u0016\u00a2\u0006\f\b\u0010\u0012\b\b\u0004\u0012\u0004\b\b(\u0019\u0012\u0004\u0012\u00020\n0\u000ej\u0002`\u001a2\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u001c0\u001bJ\u001c\u0010\u001d\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00180\u001b0\u001e2\u0006\u0010\u001f\u001a\u00020\u0012H\u0002R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006!"}, d2 = {"Lio/github/crabzilla/pgclient/PgClientEventProjector;", "", "pgPool", "Lio/reactiverse/pgclient/PgPool;", "name", "", "(Lio/reactiverse/pgclient/PgPool;Ljava/lang/String;)V", "getName", "()Ljava/lang/String;", "handle", "", "uowProjectionData", "Lio/github/crabzilla/vertx/ProjectionData;", "projectorHandler", "Lkotlin/Function4;", "Lio/reactiverse/pgclient/PgConnection;", "Lkotlin/ParameterName;", "pgConn", "", "targetId", "Lio/github/crabzilla/DomainEvent;", "event", "Lio/vertx/core/Handler;", "Lio/vertx/core/AsyncResult;", "Ljava/lang/Void;", "future", "Lio/github/crabzilla/pgclient/ProjectorHandler;", "Lio/vertx/core/Future;", "", "listOfutures", "", "size", "Companion", "crabzilla-pg-client"})
public final class PgClientEventProjector {
    private final io.reactiverse.pgclient.PgPool pgPool = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String name = null;
    private static final io.vertx.core.logging.Logger log = null;
    public static final int NUMBER_OF_FUTURES = 6;
    public static final io.github.crabzilla.pgclient.PgClientEventProjector.Companion Companion = null;
    
    public final void handle(@org.jetbrains.annotations.NotNull()
    io.github.crabzilla.vertx.ProjectionData uowProjectionData, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function4<? super io.reactiverse.pgclient.PgConnection, ? super java.lang.Integer, ? super io.github.crabzilla.DomainEvent, ? super io.vertx.core.Handler<io.vertx.core.AsyncResult<java.lang.Void>>, kotlin.Unit> projectorHandler, @org.jetbrains.annotations.NotNull()
    io.vertx.core.Future<java.lang.Boolean> future) {
    }
    
    private final java.util.List<io.vertx.core.Future<java.lang.Void>> listOfutures(int size) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getName() {
        return null;
    }
    
    public PgClientEventProjector(@org.jetbrains.annotations.NotNull()
    io.reactiverse.pgclient.PgPool pgPool, @org.jetbrains.annotations.NotNull()
    java.lang.String name) {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u001c\u0010\u0005\u001a\n \u0007*\u0004\u0018\u00010\u00060\u0006X\u0080\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\t\u00a8\u0006\n"}, d2 = {"Lio/github/crabzilla/pgclient/PgClientEventProjector$Companion;", "", "()V", "NUMBER_OF_FUTURES", "", "log", "Lio/vertx/core/logging/Logger;", "kotlin.jvm.PlatformType", "getLog$crabzilla_pg_client", "()Lio/vertx/core/logging/Logger;", "crabzilla-pg-client"})
    public static final class Companion {
        
        public final io.vertx.core.logging.Logger getLog$crabzilla_pg_client() {
            return null;
        }
        
        private Companion() {
            super();
        }
    }
}
