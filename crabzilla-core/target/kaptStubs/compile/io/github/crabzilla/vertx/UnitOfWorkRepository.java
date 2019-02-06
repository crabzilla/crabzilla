package io.github.crabzilla.vertx;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000F\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\bf\u0018\u00002\u00020\u0001J&\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u00072\u0006\u0010\t\u001a\u00020\nH&J\'\u0010\u000b\u001a\u00020\u00032\u0006\u0010\f\u001a\u00020\n2\u0006\u0010\r\u001a\u00020\u000e2\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00050\u0007H\u00a6\u0002J\u001e\u0010\u000f\u001a\u00020\u00032\u0006\u0010\u0010\u001a\u00020\u000e2\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00050\u0007H&J\u001e\u0010\u0011\u001a\u00020\u00032\u0006\u0010\u0012\u001a\u00020\u000e2\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00050\u0007H&J,\u0010\u0013\u001a\u00020\u00032\u0006\u0010\u0014\u001a\u00020\b2\u0006\u0010\u0015\u001a\u00020\b2\u0012\u0010\u0006\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00170\u00160\u0007H&J2\u0010\u0018\u001a\u00020\u00032\u0006\u0010\r\u001a\u00020\b2\n\u0010\u0019\u001a\u00060\bj\u0002`\u001a2\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u001b0\u00072\u0006\u0010\t\u001a\u00020\nH&\u00a8\u0006\u001c"}, d2 = {"Lio/github/crabzilla/vertx/UnitOfWorkRepository;", "", "append", "", "unitOfWork", "Lio/github/crabzilla/UnitOfWork;", "future", "Lio/vertx/core/Future;", "", "aggregateRootName", "", "get", "query", "id", "Ljava/util/UUID;", "getUowByCmdId", "cmdId", "getUowByUowId", "uowId", "selectAfterUowSequence", "uowSequence", "maxRows", "", "Lio/github/crabzilla/vertx/ProjectionData;", "selectAfterVersion", "version", "Lio/github/crabzilla/Version;", "Lio/github/crabzilla/SnapshotData;", "crabzilla-core"})
public abstract interface UnitOfWorkRepository {
    
    public abstract void getUowByCmdId(@org.jetbrains.annotations.NotNull()
    java.util.UUID cmdId, @org.jetbrains.annotations.NotNull()
    io.vertx.core.Future<io.github.crabzilla.UnitOfWork> future);
    
    public abstract void getUowByUowId(@org.jetbrains.annotations.NotNull()
    java.util.UUID uowId, @org.jetbrains.annotations.NotNull()
    io.vertx.core.Future<io.github.crabzilla.UnitOfWork> future);
    
    public abstract void get(@org.jetbrains.annotations.NotNull()
    java.lang.String query, @org.jetbrains.annotations.NotNull()
    java.util.UUID id, @org.jetbrains.annotations.NotNull()
    io.vertx.core.Future<io.github.crabzilla.UnitOfWork> future);
    
    public abstract void selectAfterVersion(int id, int version, @org.jetbrains.annotations.NotNull()
    io.vertx.core.Future<io.github.crabzilla.SnapshotData> future, @org.jetbrains.annotations.NotNull()
    java.lang.String aggregateRootName);
    
    public abstract void append(@org.jetbrains.annotations.NotNull()
    io.github.crabzilla.UnitOfWork unitOfWork, @org.jetbrains.annotations.NotNull()
    io.vertx.core.Future<java.lang.Integer> future, @org.jetbrains.annotations.NotNull()
    java.lang.String aggregateRootName);
    
    public abstract void selectAfterUowSequence(int uowSequence, int maxRows, @org.jetbrains.annotations.NotNull()
    io.vertx.core.Future<java.util.List<io.github.crabzilla.vertx.ProjectionData>> future);
}
