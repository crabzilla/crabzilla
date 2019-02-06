package io.github.crabzilla.vertx;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0005\b\u0001\u0018\u00002\u00020\u0001:\u0001\u000eB\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\t\u001a\u00020\nH\u0007J\b\u0010\u000b\u001a\u00020\nH\u0007J\b\u0010\f\u001a\u00020\nH\u0007J\b\u0010\r\u001a\u00020\nH\u0007R\u001a\u0010\u0003\u001a\u00020\u0004X\u0086.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\b\u00a8\u0006\u000f"}, d2 = {"Lio/github/crabzilla/vertx/CrabzillaVerticleTest;", "", "()V", "vertx", "Lio/vertx/core/Vertx;", "getVertx", "()Lio/vertx/core/Vertx;", "setVertx", "(Lio/vertx/core/Vertx;)V", "crabzillaVerticle", "", "crabzillaVerticleFactory", "crabzillaVerticleRole", "setUp", "SampleCrabzillaVerticle", "crabzilla-core"})
@org.junit.jupiter.api.TestInstance(value = org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS)
public final class CrabzillaVerticleTest {
    @org.jetbrains.annotations.NotNull()
    public io.vertx.core.Vertx vertx;
    
    @org.jetbrains.annotations.NotNull()
    public final io.vertx.core.Vertx getVertx() {
        return null;
    }
    
    public final void setVertx(@org.jetbrains.annotations.NotNull()
    io.vertx.core.Vertx p0) {
    }
    
    @org.junit.jupiter.api.BeforeEach()
    public final void setUp() {
    }
    
    @org.junit.jupiter.api.Test()
    public final void crabzillaVerticleRole() {
    }
    
    @org.junit.jupiter.api.Test()
    public final void crabzillaVerticle() {
    }
    
    @org.junit.jupiter.api.Test()
    public final void crabzillaVerticleFactory() {
    }
    
    public CrabzillaVerticleTest() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006R\u0014\u0010\u0002\u001a\u00020\u0003X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0014\u0010\u0004\u001a\u00020\u0005X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\n\u00a8\u0006\u000b"}, d2 = {"Lio/github/crabzilla/vertx/CrabzillaVerticleTest$SampleCrabzillaVerticle;", "Lio/github/crabzilla/vertx/CrabzillaVerticle;", "name", "", "role", "Lio/github/crabzilla/vertx/VerticleRole;", "(Ljava/lang/String;Lio/github/crabzilla/vertx/VerticleRole;)V", "getName", "()Ljava/lang/String;", "getRole", "()Lio/github/crabzilla/vertx/VerticleRole;", "crabzilla-core"})
    public static final class SampleCrabzillaVerticle extends io.github.crabzilla.vertx.CrabzillaVerticle {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String name = null;
        @org.jetbrains.annotations.NotNull()
        private final io.github.crabzilla.vertx.VerticleRole role = null;
        
        @org.jetbrains.annotations.NotNull()
        @java.lang.Override()
        public java.lang.String getName() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        @java.lang.Override()
        public io.github.crabzilla.vertx.VerticleRole getRole() {
            return null;
        }
        
        public SampleCrabzillaVerticle(@org.jetbrains.annotations.NotNull()
        java.lang.String name, @org.jetbrains.annotations.NotNull()
        io.github.crabzilla.vertx.VerticleRole role) {
            super(null, null);
        }
    }
}
