package io.github.crabzilla.pgclient;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\bg\u0018\u00002\u00020\u0001J\b\u0010\u0002\u001a\u00020\u0003H\'J\b\u0010\u0004\u001a\u00020\u0005H&J\b\u0010\u0006\u001a\u00020\u0003H\'\u00a8\u0006\u0007"}, d2 = {"Lio/github/crabzilla/pgclient/PgClientTestComponent;", "", "readDb", "Lio/reactiverse/pgclient/PgPool;", "vertx", "Lio/vertx/core/Vertx;", "writeDb", "crabzilla-pg-client"})
@dagger.Component(modules = {io.github.crabzilla.vertx.CrabzillaMainModule.class, io.github.crabzilla.pgclient.PgClientModule.class})
@javax.inject.Singleton()
public abstract interface PgClientTestComponent {
    
    @org.jetbrains.annotations.NotNull()
    public abstract io.vertx.core.Vertx vertx();
    
    @org.jetbrains.annotations.NotNull()
    @io.github.crabzilla.vertx.ReadDatabase()
    public abstract io.reactiverse.pgclient.PgPool readDb();
    
    @org.jetbrains.annotations.NotNull()
    @io.github.crabzilla.vertx.WriteDatabase()
    public abstract io.reactiverse.pgclient.PgPool writeDb();
}
