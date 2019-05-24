package io.github.crabzilla;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\bf\u0018\u0000*\b\b\u0000\u0010\u0001*\u00020\u00022\u00020\u0003J*\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072\u0018\u0010\b\u001a\u0014\u0012\u0010\u0012\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00028\u00000\u000b0\n0\tH&J2\u0010\f\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072\f\u0010\r\u001a\b\u0012\u0004\u0012\u00028\u00000\u000b2\u0012\u0010\b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000e0\n0\tH&\u00a8\u0006\u000f"}, d2 = {"Lio/github/crabzilla/SnapshotRepository;", "E", "Lio/github/crabzilla/Entity;", "", "retrieve", "", "entityId", "", "aHandler", "Lio/vertx/core/Handler;", "Lio/vertx/core/AsyncResult;", "Lio/github/crabzilla/Snapshot;", "upsert", "snapshot", "Ljava/lang/Void;", "crabzilla-core"})
public abstract interface SnapshotRepository<E extends io.github.crabzilla.Entity> {
    
    public abstract void retrieve(int entityId, @org.jetbrains.annotations.NotNull()
    io.vertx.core.Handler<io.vertx.core.AsyncResult<io.github.crabzilla.Snapshot<E>>> aHandler);
    
    public abstract void upsert(int entityId, @org.jetbrains.annotations.NotNull()
    io.github.crabzilla.Snapshot<E> snapshot, @org.jetbrains.annotations.NotNull()
    io.vertx.core.Handler<io.vertx.core.AsyncResult<java.lang.Void>> aHandler);
}
