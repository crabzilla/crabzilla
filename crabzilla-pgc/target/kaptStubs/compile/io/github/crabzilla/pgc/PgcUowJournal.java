package io.github.crabzilla.pgc;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u0000 \u0011*\b\b\u0000\u0010\u0001*\u00020\u00022\u00020\u0003:\u0001\u0011B\u001b\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00028\u00000\u0007\u00a2\u0006\u0002\u0010\bJ$\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\u0012\u0010\r\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00100\u000f0\u000eH\u0016R\u0014\u0010\u0006\u001a\b\u0012\u0004\u0012\u00028\u00000\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0012"}, d2 = {"Lio/github/crabzilla/pgc/PgcUowJournal;", "E", "Lio/github/crabzilla/Entity;", "Lio/github/crabzilla/UnitOfWorkJournal;", "pgPool", "Lio/reactiverse/pgclient/PgPool;", "jsonFunctions", "Lio/github/crabzilla/EntityJsonFunctions;", "(Lio/reactiverse/pgclient/PgPool;Lio/github/crabzilla/EntityJsonFunctions;)V", "append", "", "unitOfWork", "Lio/github/crabzilla/UnitOfWork;", "aHandler", "Lio/vertx/core/Handler;", "Lio/vertx/core/AsyncResult;", "Ljava/math/BigInteger;", "Companion", "crabzilla-pgc"})
public final class PgcUowJournal<E extends io.github.crabzilla.Entity> implements io.github.crabzilla.UnitOfWorkJournal {
    private final io.reactiverse.pgclient.PgPool pgPool = null;
    private final io.github.crabzilla.EntityJsonFunctions<E> jsonFunctions = null;
    private static final org.slf4j.Logger log = null;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String SQL_SELECT_CURRENT_VERSION = "select max(version) as last_version from units_of_work where ar_id = $1 and ar_name = $2 ";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String SQL_APPEND_UOW = "insert into units_of_work (uow_events, cmd_id, cmd_name, cmd_data, ar_name, ar_id, version) values ($1, $2, $3, $4, $5, $6, $7) returning uow_id";
    public static final io.github.crabzilla.pgc.PgcUowJournal.Companion Companion = null;
    
    @java.lang.Override()
    public void append(@org.jetbrains.annotations.NotNull()
    io.github.crabzilla.UnitOfWork unitOfWork, @org.jetbrains.annotations.NotNull()
    io.vertx.core.Handler<io.vertx.core.AsyncResult<java.math.BigInteger>> aHandler) {
    }
    
    public PgcUowJournal(@org.jetbrains.annotations.NotNull()
    io.reactiverse.pgclient.PgPool pgPool, @org.jetbrains.annotations.NotNull()
    io.github.crabzilla.EntityJsonFunctions<E> jsonFunctions) {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u001c\u0010\u0006\u001a\n \b*\u0004\u0018\u00010\u00070\u0007X\u0080\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\n\u00a8\u0006\u000b"}, d2 = {"Lio/github/crabzilla/pgc/PgcUowJournal$Companion;", "", "()V", "SQL_APPEND_UOW", "", "SQL_SELECT_CURRENT_VERSION", "log", "Lorg/slf4j/Logger;", "kotlin.jvm.PlatformType", "getLog$crabzilla_pgc", "()Lorg/slf4j/Logger;", "crabzilla-pgc"})
    public static final class Companion {
        
        public final org.slf4j.Logger getLog$crabzilla_pgc() {
            return null;
        }
        
        private Companion() {
            super();
        }
    }
}
