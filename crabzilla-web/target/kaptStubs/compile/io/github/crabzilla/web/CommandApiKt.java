package io.github.crabzilla.web;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 2, d1 = {"\u00008\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u001a\u0016\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f\u001a\u001e\u0010\r\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\u000e\u001a\u00020\u000f\u001a\"\u0010\u0010\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00130\u00120\u00112\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u0014\u001a\u00020\u0015\"\"\u0010\u0000\u001a\n \u0002*\u0004\u0018\u00010\u00010\u0001X\u0080\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0003\u0010\u0004\"\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0016"}, d2 = {"log", "Lio/vertx/core/logging/Logger;", "kotlin.jvm.PlatformType", "getLog", "()Lio/vertx/core/logging/Logger;", "setLog", "(Lio/vertx/core/logging/Logger;)V", "getUowByCmdId", "", "routingContext", "Lio/vertx/ext/web/RoutingContext;", "uowRepository", "Lio/github/crabzilla/vertx/UnitOfWorkRepository;", "postCommandHandler", "handlerService", "Lio/github/crabzilla/web/CommandHandlerServiceImpl;", "resultHandler", "Lio/vertx/core/Handler;", "Lio/vertx/core/AsyncResult;", "Lio/github/crabzilla/CommandExecution;", "httpResp", "Lio/vertx/core/http/HttpServerResponse;", "crabzilla-web"})
public final class CommandApiKt {
    private static io.vertx.core.logging.Logger log;
    
    public static final io.vertx.core.logging.Logger getLog() {
        return null;
    }
    
    public static final void setLog(io.vertx.core.logging.Logger p0) {
    }
    
    public static final void postCommandHandler(@org.jetbrains.annotations.NotNull()
    io.vertx.ext.web.RoutingContext routingContext, @org.jetbrains.annotations.NotNull()
    io.github.crabzilla.vertx.UnitOfWorkRepository uowRepository, @org.jetbrains.annotations.NotNull()
    io.github.crabzilla.web.CommandHandlerServiceImpl handlerService) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public static final io.vertx.core.Handler<io.vertx.core.AsyncResult<io.github.crabzilla.CommandExecution>> resultHandler(@org.jetbrains.annotations.NotNull()
    io.vertx.ext.web.RoutingContext routingContext, @org.jetbrains.annotations.NotNull()
    io.vertx.core.http.HttpServerResponse httpResp) {
        return null;
    }
    
    public static final void getUowByCmdId(@org.jetbrains.annotations.NotNull()
    io.vertx.ext.web.RoutingContext routingContext, @org.jetbrains.annotations.NotNull()
    io.github.crabzilla.vertx.UnitOfWorkRepository uowRepository) {
    }
}
