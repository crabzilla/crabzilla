package io.github.crabzilla.web;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u001e\u0010\u0003\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00060\u00050\u00042\b\b\u0001\u0010\u0007\u001a\u00020\bH\u0007J\u001e\u0010\t\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00060\u00050\u00042\b\b\u0001\u0010\n\u001a\u00020\bH\u0007\u00a8\u0006\u000b"}, d2 = {"Lio/github/crabzilla/web/WebModule;", "", "()V", "healthcheck1", "Lio/vertx/core/Handler;", "Lio/vertx/core/Future;", "Lio/vertx/ext/healthchecks/Status;", "readPool", "Lio/reactiverse/pgclient/PgPool;", "healthcheck2", "writePool", "crabzilla-web"})
@dagger.Module()
public final class WebModule {
    
    @org.jetbrains.annotations.NotNull()
    @dagger.multibindings.StringKey(value = "read-database")
    @dagger.multibindings.IntoMap()
    @dagger.Provides()
    public final io.vertx.core.Handler<io.vertx.core.Future<io.vertx.ext.healthchecks.Status>> healthcheck1(@org.jetbrains.annotations.NotNull()
    @io.github.crabzilla.vertx.ReadDatabase()
    io.reactiverse.pgclient.PgPool readPool) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    @dagger.multibindings.StringKey(value = "write-database")
    @dagger.multibindings.IntoMap()
    @dagger.Provides()
    public final io.vertx.core.Handler<io.vertx.core.Future<io.vertx.ext.healthchecks.Status>> healthcheck2(@org.jetbrains.annotations.NotNull()
    @io.github.crabzilla.vertx.WriteDatabase()
    io.reactiverse.pgclient.PgPool writePool) {
        return null;
    }
    
    public WebModule() {
        super();
    }
}
