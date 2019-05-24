package io.github.crabzilla.web.example1;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u0000 $2\u00020\u0001:\u0001$B\u0019\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0018\u0010\u001a\u001a\u00020\f2\u0006\u0010\u001b\u001a\u00020\u00052\u0006\u0010\u001c\u001a\u00020\u001dH\u0002J\u0016\u0010\u001e\u001a\u00020\u001f2\f\u0010 \u001a\b\u0012\u0004\u0012\u00020\"0!H\u0016J\b\u0010#\u001a\u00020\u001fH\u0016R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u001a\u0010\u000b\u001a\u00020\fX\u0086.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\r\u0010\u000e\"\u0004\b\u000f\u0010\u0010R\u001a\u0010\u0011\u001a\u00020\u0012X\u0086.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0013\u0010\u0014\"\u0004\b\u0015\u0010\u0016R\u001a\u0010\u0017\u001a\u00020\fX\u0086.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0018\u0010\u000e\"\u0004\b\u0019\u0010\u0010\u00a8\u0006%"}, d2 = {"Lio/github/crabzilla/web/example1/Example1Verticle;", "Lio/vertx/core/AbstractVerticle;", "httpPort", "", "configFile", "", "(ILjava/lang/String;)V", "getConfigFile", "()Ljava/lang/String;", "getHttpPort", "()I", "readModelDb", "Lio/reactiverse/pgclient/PgPool;", "getReadModelDb", "()Lio/reactiverse/pgclient/PgPool;", "setReadModelDb", "(Lio/reactiverse/pgclient/PgPool;)V", "server", "Lio/vertx/core/http/HttpServer;", "getServer", "()Lio/vertx/core/http/HttpServer;", "setServer", "(Lio/vertx/core/http/HttpServer;)V", "writeModelDb", "getWriteModelDb", "setWriteModelDb", "pgPool", "id", "config", "Lio/vertx/core/json/JsonObject;", "start", "", "startFuture", "Lio/vertx/core/Future;", "Ljava/lang/Void;", "stop", "Companion", "crabzilla-web"})
public final class Example1Verticle extends io.vertx.core.AbstractVerticle {
    @org.jetbrains.annotations.NotNull()
    public io.vertx.core.http.HttpServer server;
    @org.jetbrains.annotations.NotNull()
    public io.reactiverse.pgclient.PgPool writeModelDb;
    @org.jetbrains.annotations.NotNull()
    public io.reactiverse.pgclient.PgPool readModelDb;
    private final int httpPort = 0;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String configFile = null;
    private static org.slf4j.Logger log;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String CUSTOMER_AGGREGATE_ROOT = "customer";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXAMPLE1_PROJECTION_ENDPOINT = "example1_projection_endpoint";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String COMMAND_NAME_PARAMETER = "commandName";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String COMMAND_ENTITY_ID_PARAMETER = "entityId";
    public static final io.github.crabzilla.web.example1.Example1Verticle.Companion Companion = null;
    
    @org.jetbrains.annotations.NotNull()
    public final io.vertx.core.http.HttpServer getServer() {
        return null;
    }
    
    public final void setServer(@org.jetbrains.annotations.NotNull()
    io.vertx.core.http.HttpServer p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactiverse.pgclient.PgPool getWriteModelDb() {
        return null;
    }
    
    public final void setWriteModelDb(@org.jetbrains.annotations.NotNull()
    io.reactiverse.pgclient.PgPool p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.reactiverse.pgclient.PgPool getReadModelDb() {
        return null;
    }
    
    public final void setReadModelDb(@org.jetbrains.annotations.NotNull()
    io.reactiverse.pgclient.PgPool p0) {
    }
    
    @java.lang.Override()
    public void start(@org.jetbrains.annotations.NotNull()
    io.vertx.core.Future<java.lang.Void> startFuture) {
    }
    
    @java.lang.Override()
    public void stop() {
    }
    
    private final io.reactiverse.pgclient.PgPool pgPool(java.lang.String id, io.vertx.core.json.JsonObject config) {
        return null;
    }
    
    public final int getHttpPort() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getConfigFile() {
        return null;
    }
    
    public Example1Verticle(int httpPort, @org.jetbrains.annotations.NotNull()
    java.lang.String configFile) {
        super();
    }
    
    public Example1Verticle() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0006\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\"\u0010\b\u001a\n \n*\u0004\u0018\u00010\t0\tX\u0080\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000b\u0010\f\"\u0004\b\r\u0010\u000e\u00a8\u0006\u000f"}, d2 = {"Lio/github/crabzilla/web/example1/Example1Verticle$Companion;", "", "()V", "COMMAND_ENTITY_ID_PARAMETER", "", "COMMAND_NAME_PARAMETER", "CUSTOMER_AGGREGATE_ROOT", "EXAMPLE1_PROJECTION_ENDPOINT", "log", "Lorg/slf4j/Logger;", "kotlin.jvm.PlatformType", "getLog$crabzilla_web", "()Lorg/slf4j/Logger;", "setLog$crabzilla_web", "(Lorg/slf4j/Logger;)V", "crabzilla-web"})
    public static final class Companion {
        
        public final org.slf4j.Logger getLog$crabzilla_web() {
            return null;
        }
        
        public final void setLog$crabzilla_web(org.slf4j.Logger p0) {
        }
        
        private Companion() {
            super();
        }
    }
}
