package io.github.crabzilla.pgclient;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0017\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0007J\u0018\u0010\t\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0007\u00a8\u0006\n"}, d2 = {"Lio/github/crabzilla/pgclient/PgClientModule;", "", "()V", "pgPool1", "Lio/reactiverse/pgclient/PgPool;", "config", "Lio/vertx/core/json/JsonObject;", "vertx", "Lio/vertx/core/Vertx;", "pgPool2", "crabzilla-pg-client"})
@dagger.Module()
public class PgClientModule {
    
    @org.jetbrains.annotations.NotNull()
    @io.github.crabzilla.vertx.WriteDatabase()
    @javax.inject.Singleton()
    @dagger.Provides()
    public final io.reactiverse.pgclient.PgPool pgPool1(@org.jetbrains.annotations.NotNull()
    io.vertx.core.json.JsonObject config, @org.jetbrains.annotations.NotNull()
    io.vertx.core.Vertx vertx) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    @io.github.crabzilla.vertx.ReadDatabase()
    @javax.inject.Singleton()
    @dagger.Provides()
    public final io.reactiverse.pgclient.PgPool pgPool2(@org.jetbrains.annotations.NotNull()
    io.vertx.core.json.JsonObject config, @org.jetbrains.annotations.NotNull()
    io.vertx.core.Vertx vertx) {
        return null;
    }
    
    public PgClientModule() {
        super();
    }
}
