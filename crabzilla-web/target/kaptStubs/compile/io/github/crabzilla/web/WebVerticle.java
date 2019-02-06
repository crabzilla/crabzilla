package io.github.crabzilla.web;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0002\b\u0002\u0018\u0000 \u00112\u00020\u0001:\u0001\u0011B-\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u00a2\u0006\u0002\u0010\fJ\b\u0010\u000f\u001a\u00020\u0010H\u0016R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0002\u001a\u00020\u0003X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0012"}, d2 = {"Lio/github/crabzilla/web/WebVerticle;", "Lio/github/crabzilla/vertx/CrabzillaVerticle;", "name", "", "config", "Lio/vertx/core/json/JsonObject;", "healthCheckHandler", "Lio/vertx/ext/healthchecks/HealthCheckHandler;", "uowRepository", "Lio/github/crabzilla/vertx/UnitOfWorkRepository;", "handlerService", "Lio/github/crabzilla/web/CommandHandlerServiceImpl;", "(Ljava/lang/String;Lio/vertx/core/json/JsonObject;Lio/vertx/ext/healthchecks/HealthCheckHandler;Lio/github/crabzilla/vertx/UnitOfWorkRepository;Lio/github/crabzilla/web/CommandHandlerServiceImpl;)V", "getName", "()Ljava/lang/String;", "start", "", "Companion", "crabzilla-web"})
public final class WebVerticle extends io.github.crabzilla.vertx.CrabzillaVerticle {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String name = null;
    private final io.vertx.core.json.JsonObject config = null;
    private final io.vertx.ext.healthchecks.HealthCheckHandler healthCheckHandler = null;
    private final io.github.crabzilla.vertx.UnitOfWorkRepository uowRepository = null;
    private final io.github.crabzilla.web.CommandHandlerServiceImpl handlerService = null;
    private static io.vertx.core.logging.Logger log;
    public static final io.github.crabzilla.web.WebVerticle.Companion Companion = null;
    
    @java.lang.Override()
    public void start() {
    }
    
    @org.jetbrains.annotations.NotNull()
    @java.lang.Override()
    public java.lang.String getName() {
        return null;
    }
    
    public WebVerticle(@org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.NotNull()
    io.vertx.core.json.JsonObject config, @org.jetbrains.annotations.NotNull()
    io.vertx.ext.healthchecks.HealthCheckHandler healthCheckHandler, @org.jetbrains.annotations.NotNull()
    io.github.crabzilla.vertx.UnitOfWorkRepository uowRepository, @org.jetbrains.annotations.NotNull()
    io.github.crabzilla.web.CommandHandlerServiceImpl handlerService) {
        super(null, null);
    }
    
    @kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\"\u0010\u0003\u001a\n \u0005*\u0004\u0018\u00010\u00040\u0004X\u0080\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0006\u0010\u0007\"\u0004\b\b\u0010\t\u00a8\u0006\n"}, d2 = {"Lio/github/crabzilla/web/WebVerticle$Companion;", "", "()V", "log", "Lio/vertx/core/logging/Logger;", "kotlin.jvm.PlatformType", "getLog$crabzilla_web", "()Lio/vertx/core/logging/Logger;", "setLog$crabzilla_web", "(Lio/vertx/core/logging/Logger;)V", "crabzilla-web"})
    public static final class Companion {
        
        public final io.vertx.core.logging.Logger getLog$crabzilla_web() {
            return null;
        }
        
        public final void setLog$crabzilla_web(io.vertx.core.logging.Logger p0) {
        }
        
        private Companion() {
            super();
        }
    }
}
