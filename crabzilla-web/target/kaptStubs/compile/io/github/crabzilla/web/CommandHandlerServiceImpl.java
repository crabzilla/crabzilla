package io.github.crabzilla.web;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0016\u0018\u0000 \u00132\u00020\u0001:\u0001\u0013B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J*\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u00052\u0006\u0010\r\u001a\u00020\u000e2\u0012\u0010\u000f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00120\u00110\u0010R\u0016\u0010\u0007\u001a\n \t*\u0004\u0018\u00010\b0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0014"}, d2 = {"Lio/github/crabzilla/web/CommandHandlerServiceImpl;", "", "vertx", "Lio/vertx/core/Vertx;", "projectionEndpoint", "", "(Lio/vertx/core/Vertx;Ljava/lang/String;)V", "commandDeliveryOptions", "Lio/vertx/core/eventbus/DeliveryOptions;", "kotlin.jvm.PlatformType", "postCommand", "", "handlerEndpoint", "command", "Lio/github/crabzilla/Command;", "handler", "Lio/vertx/core/Handler;", "Lio/vertx/core/AsyncResult;", "Lio/github/crabzilla/CommandExecution;", "Companion", "crabzilla-web"})
public class CommandHandlerServiceImpl {
    private final io.vertx.core.eventbus.DeliveryOptions commandDeliveryOptions = null;
    private final io.vertx.core.Vertx vertx = null;
    private final java.lang.String projectionEndpoint = null;
    private static org.slf4j.Logger log;
    public static final io.github.crabzilla.web.CommandHandlerServiceImpl.Companion Companion = null;
    
    public final void postCommand(@org.jetbrains.annotations.NotNull()
    java.lang.String handlerEndpoint, @org.jetbrains.annotations.NotNull()
    io.github.crabzilla.Command command, @org.jetbrains.annotations.NotNull()
    io.vertx.core.Handler<io.vertx.core.AsyncResult<io.github.crabzilla.CommandExecution>> handler) {
    }
    
    public CommandHandlerServiceImpl(@org.jetbrains.annotations.NotNull()
    io.vertx.core.Vertx vertx, @org.jetbrains.annotations.NotNull()
    java.lang.String projectionEndpoint) {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\"\u0010\u0003\u001a\n \u0005*\u0004\u0018\u00010\u00040\u0004X\u0080\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0006\u0010\u0007\"\u0004\b\b\u0010\t\u00a8\u0006\n"}, d2 = {"Lio/github/crabzilla/web/CommandHandlerServiceImpl$Companion;", "", "()V", "log", "Lorg/slf4j/Logger;", "kotlin.jvm.PlatformType", "getLog$crabzilla_web", "()Lorg/slf4j/Logger;", "setLog$crabzilla_web", "(Lorg/slf4j/Logger;)V", "crabzilla-web"})
    public static final class Companion {
        
        public final org.slf4j.Logger getLog$crabzilla_web() {
            return null;
        }
        
        public final void setLog$crabzilla_web(org.slf4j.Logger p0) {
        }
        
        private Companion() {
            super();
        }
    }
}
