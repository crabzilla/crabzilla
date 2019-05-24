package io.github.crabzilla;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000R\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0000\bf\u0018\u00002\u00020\u0001J*\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0018\u0010\u0006\u001a\u0014\u0012\u0010\u0012\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\t0\b0\u0007H&J$\u0010\u000b\u001a\u00020\u00032\u0006\u0010\f\u001a\u00020\r2\u0012\u0010\u0006\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\b0\u0007H&J$\u0010\u000e\u001a\u00020\u00032\u0006\u0010\u000f\u001a\u00020\u00102\u0012\u0010\u0006\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\b0\u0007H&J2\u0010\u0011\u001a\u00020\u00032\u0006\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0012\u001a\u00020\u00052\u0018\u0010\u0006\u001a\u0014\u0012\u0010\u0012\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00130\t0\b0\u0007H&J8\u0010\u0014\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\n\u0010\u0015\u001a\u00060\u0005j\u0002`\u00162\u0006\u0010\u0017\u001a\u00020\u00182\u0012\u0010\u0006\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00190\b0\u0007H&\u00a8\u0006\u001a"}, d2 = {"Lio/github/crabzilla/UnitOfWorkRepository;", "", "getAllUowByEntityId", "", "id", "", "aHandler", "Lio/vertx/core/Handler;", "Lio/vertx/core/AsyncResult;", "", "Lio/github/crabzilla/UnitOfWork;", "getUowByCmdId", "cmdId", "Ljava/util/UUID;", "getUowByUowId", "uowId", "Ljava/math/BigInteger;", "selectAfterUowId", "maxRows", "Lio/github/crabzilla/UnitOfWorkEvents;", "selectAfterVersion", "version", "Lio/github/crabzilla/Version;", "aggregateRootName", "", "Lio/github/crabzilla/RangeOfEvents;", "crabzilla-core"})
public abstract interface UnitOfWorkRepository {
    
    public abstract void getUowByCmdId(@org.jetbrains.annotations.NotNull()
    java.util.UUID cmdId, @org.jetbrains.annotations.NotNull()
    io.vertx.core.Handler<io.vertx.core.AsyncResult<io.github.crabzilla.UnitOfWork>> aHandler);
    
    public abstract void getUowByUowId(@org.jetbrains.annotations.NotNull()
    java.math.BigInteger uowId, @org.jetbrains.annotations.NotNull()
    io.vertx.core.Handler<io.vertx.core.AsyncResult<io.github.crabzilla.UnitOfWork>> aHandler);
    
    public abstract void selectAfterVersion(int id, int version, @org.jetbrains.annotations.NotNull()
    java.lang.String aggregateRootName, @org.jetbrains.annotations.NotNull()
    io.vertx.core.Handler<io.vertx.core.AsyncResult<io.github.crabzilla.RangeOfEvents>> aHandler);
    
    public abstract void selectAfterUowId(@org.jetbrains.annotations.NotNull()
    java.math.BigInteger uowId, int maxRows, @org.jetbrains.annotations.NotNull()
    io.vertx.core.Handler<io.vertx.core.AsyncResult<java.util.List<io.github.crabzilla.UnitOfWorkEvents>>> aHandler);
    
    public abstract void getAllUowByEntityId(int id, @org.jetbrains.annotations.NotNull()
    io.vertx.core.Handler<io.vertx.core.AsyncResult<java.util.List<io.github.crabzilla.UnitOfWork>>> aHandler);
}
