package io.github.crabzilla.vertx;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\"\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\u001b\u0012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\u0002\u0010\u0007J\u001a\u0010\u000b\u001a\u0004\u0018\u00010\f2\u0006\u0010\r\u001a\u00020\n2\u0006\u0010\u000e\u001a\u00020\u000fH\u0016J\b\u0010\u0010\u001a\u00020\nH\u0016R\u001a\u0010\b\u001a\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\u00040\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0011"}, d2 = {"Lio/github/crabzilla/vertx/CrabzillaVerticleFactory;", "Lio/vertx/core/spi/VerticleFactory;", "verticles", "", "Lio/github/crabzilla/vertx/CrabzillaVerticle;", "role", "Lio/github/crabzilla/vertx/VerticleRole;", "(Ljava/util/Set;Lio/github/crabzilla/vertx/VerticleRole;)V", "map", "", "", "createVerticle", "Lio/vertx/core/Verticle;", "name", "classLoader", "Ljava/lang/ClassLoader;", "prefix", "crabzilla-core"})
public final class CrabzillaVerticleFactory implements io.vertx.core.spi.VerticleFactory {
    private final java.util.Map<java.lang.String, io.github.crabzilla.vertx.CrabzillaVerticle> map = null;
    private final io.github.crabzilla.vertx.VerticleRole role = null;
    
    @org.jetbrains.annotations.NotNull()
    @java.lang.Override()
    public java.lang.String prefix() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    @java.lang.Override()
    public io.vertx.core.Verticle createVerticle(@org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.NotNull()
    java.lang.ClassLoader classLoader) throws java.lang.Exception {
        return null;
    }
    
    public CrabzillaVerticleFactory(@org.jetbrains.annotations.NotNull()
    java.util.Set<? extends io.github.crabzilla.vertx.CrabzillaVerticle> verticles, @org.jetbrains.annotations.NotNull()
    io.github.crabzilla.vertx.VerticleRole role) {
        super();
    }
}
