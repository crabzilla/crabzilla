package io.github.crabzilla.pgclient;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000P\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0016\u0018\u0000 \u001f2\u00020\u0001:\u0001\u001fB\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J&\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\n2\u0006\u0010\f\u001a\u00020\rH\u0016J\'\u0010\u000e\u001a\u00020\u00062\u0006\u0010\u000f\u001a\u00020\r2\u0006\u0010\u0010\u001a\u00020\u00112\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\b0\nH\u0096\u0002J\u001e\u0010\u0012\u001a\u00020\u00062\u0006\u0010\u0013\u001a\u00020\u00112\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\b0\nH\u0016J\u001e\u0010\u0014\u001a\u00020\u00062\u0006\u0010\u0015\u001a\u00020\u00112\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\b0\nH\u0016J,\u0010\u0016\u001a\u00020\u00062\u0006\u0010\u0017\u001a\u00020\u000b2\u0006\u0010\u0018\u001a\u00020\u000b2\u0012\u0010\t\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001a0\u00190\nH\u0016J2\u0010\u001b\u001a\u00020\u00062\u0006\u0010\u0010\u001a\u00020\u000b2\n\u0010\u001c\u001a\u00060\u000bj\u0002`\u001d2\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u001e0\n2\u0006\u0010\f\u001a\u00020\rH\u0016R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006 "}, d2 = {"Lio/github/crabzilla/pgclient/PgClientUowRepo;", "Lio/github/crabzilla/vertx/UnitOfWorkRepository;", "pgPool", "Lio/reactiverse/pgclient/PgPool;", "(Lio/reactiverse/pgclient/PgPool;)V", "append", "", "unitOfWork", "Lio/github/crabzilla/UnitOfWork;", "future", "Lio/vertx/core/Future;", "", "aggregateRootName", "", "get", "query", "id", "Ljava/util/UUID;", "getUowByCmdId", "cmdId", "getUowByUowId", "uowId", "selectAfterUowSequence", "uowSequence", "maxRows", "", "Lio/github/crabzilla/vertx/ProjectionData;", "selectAfterVersion", "version", "Lio/github/crabzilla/Version;", "Lio/github/crabzilla/SnapshotData;", "Companion", "crabzilla-pg-client"})
public class PgClientUowRepo implements io.github.crabzilla.vertx.UnitOfWorkRepository {
    private final io.reactiverse.pgclient.PgPool pgPool = null;
    private static final io.vertx.core.logging.Logger log = null;
    private static final java.lang.String UOW_ID = "uow_id";
    private static final java.lang.String UOW_EVENTS = "uow_events";
    private static final java.lang.String CMD_DATA = "cmd_data";
    private static final java.lang.String VERSION = "version";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String SQL_SELECT_UOW_BY_CMD_ID = "select * from units_of_work where cmd_id = $1 ";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String SQL_SELECT_UOW_BY_UOW_ID = "select * from units_of_work where uow_id = $1 ";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String SQL_SELECT_AFTER_VERSION = "select uow_events, version from units_of_work where ar_id = $1 and ar_name = $2 and version > $3 order by version ";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String SQL_SELECT_CURRENT_VERSION = "select max(version) as last_version from units_of_work where ar_id = $1 and ar_name = $2 ";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String SQL_INSERT_UOW = "insert into units_of_work (uow_id, uow_events, cmd_id, cmd_data, ar_name, ar_id, version) values ($1, $2, $3, $4, $5, $6, $7) returning uow_seq_number";
    public static final io.github.crabzilla.pgclient.PgClientUowRepo.Companion Companion = null;
    
    @java.lang.Override()
    public void getUowByCmdId(@org.jetbrains.annotations.NotNull()
    java.util.UUID cmdId, @org.jetbrains.annotations.NotNull()
    io.vertx.core.Future<io.github.crabzilla.UnitOfWork> future) {
    }
    
    @java.lang.Override()
    public void getUowByUowId(@org.jetbrains.annotations.NotNull()
    java.util.UUID uowId, @org.jetbrains.annotations.NotNull()
    io.vertx.core.Future<io.github.crabzilla.UnitOfWork> future) {
    }
    
    @java.lang.Override()
    public void get(@org.jetbrains.annotations.NotNull()
    java.lang.String query, @org.jetbrains.annotations.NotNull()
    java.util.UUID id, @org.jetbrains.annotations.NotNull()
    io.vertx.core.Future<io.github.crabzilla.UnitOfWork> future) {
    }
    
    @java.lang.Override()
    public void selectAfterVersion(int id, int version, @org.jetbrains.annotations.NotNull()
    io.vertx.core.Future<io.github.crabzilla.SnapshotData> future, @org.jetbrains.annotations.NotNull()
    java.lang.String aggregateRootName) {
    }
    
    @java.lang.Override()
    public void selectAfterUowSequence(int uowSequence, int maxRows, @org.jetbrains.annotations.NotNull()
    io.vertx.core.Future<java.util.List<io.github.crabzilla.vertx.ProjectionData>> future) {
    }
    
    @java.lang.Override()
    public void append(@org.jetbrains.annotations.NotNull()
    io.github.crabzilla.UnitOfWork unitOfWork, @org.jetbrains.annotations.NotNull()
    io.vertx.core.Future<java.lang.Integer> future, @org.jetbrains.annotations.NotNull()
    java.lang.String aggregateRootName) {
    }
    
    public PgClientUowRepo(@org.jetbrains.annotations.NotNull()
    io.reactiverse.pgclient.PgPool pgPool) {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u001c\u0010\r\u001a\n \u000f*\u0004\u0018\u00010\u000e0\u000eX\u0080\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011\u00a8\u0006\u0012"}, d2 = {"Lio/github/crabzilla/pgclient/PgClientUowRepo$Companion;", "", "()V", "CMD_DATA", "", "SQL_INSERT_UOW", "SQL_SELECT_AFTER_VERSION", "SQL_SELECT_CURRENT_VERSION", "SQL_SELECT_UOW_BY_CMD_ID", "SQL_SELECT_UOW_BY_UOW_ID", "UOW_EVENTS", "UOW_ID", "VERSION", "log", "Lio/vertx/core/logging/Logger;", "kotlin.jvm.PlatformType", "getLog$crabzilla_pg_client", "()Lio/vertx/core/logging/Logger;", "crabzilla-pg-client"})
    public static final class Companion {
        
        public final io.vertx.core.logging.Logger getLog$crabzilla_pg_client() {
            return null;
        }
        
        private Companion() {
            super();
        }
    }
}
