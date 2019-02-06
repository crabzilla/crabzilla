package io.github.crabzilla.vertx;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\b\u0007\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\b\u0010\u0004\u001a\u00020\u0005H\u0007J\b\u0010\u0002\u001a\u00020\u0003H\u0007R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\n\u00a8\u0006\u000b"}, d2 = {"Lio/github/crabzilla/vertx/CrabzillaMainModule;", "", "vertx", "Lio/vertx/core/Vertx;", "config", "Lio/vertx/core/json/JsonObject;", "(Lio/vertx/core/Vertx;Lio/vertx/core/json/JsonObject;)V", "getConfig", "()Lio/vertx/core/json/JsonObject;", "getVertx", "()Lio/vertx/core/Vertx;", "crabzilla-core"})
@dagger.Module()
public final class CrabzillaMainModule {
    @org.jetbrains.annotations.NotNull()
    private final io.vertx.core.Vertx vertx = null;
    @org.jetbrains.annotations.NotNull()
    private final io.vertx.core.json.JsonObject config = null;
    
    @org.jetbrains.annotations.NotNull()
    @javax.inject.Singleton()
    @dagger.Provides()
    public final io.vertx.core.Vertx vertx() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    @javax.inject.Singleton()
    @dagger.Provides()
    public final io.vertx.core.json.JsonObject config() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.vertx.core.Vertx getVertx() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.vertx.core.json.JsonObject getConfig() {
        return null;
    }
    
    public CrabzillaMainModule(@org.jetbrains.annotations.NotNull()
    io.vertx.core.Vertx vertx, @org.jetbrains.annotations.NotNull()
    io.vertx.core.json.JsonObject config) {
        super();
    }
}
