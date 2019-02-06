package io.github.crabzilla;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000F\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u000f\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B/\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\n\u0010\u0006\u001a\u00060\u0007j\u0002`\b\u0012\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\n\u00a2\u0006\u0002\u0010\fJ\t\u0010\u0015\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0016\u001a\u00020\u0005H\u00c6\u0003J\r\u0010\u0017\u001a\u00060\u0007j\u0002`\bH\u00c6\u0003J\u000f\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u000b0\nH\u00c6\u0003J;\u0010\u0019\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\f\b\u0002\u0010\u0006\u001a\u00060\u0007j\u0002`\b2\u000e\b\u0002\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nH\u00c6\u0001J\u0013\u0010\u001a\u001a\u00020\u001b2\b\u0010\u001c\u001a\u0004\u0018\u00010\u001dH\u00d6\u0003J\t\u0010\u001e\u001a\u00020\u0007H\u00d6\u0001J\u0006\u0010\u001f\u001a\u00020 J\t\u0010!\u001a\u00020\"H\u00d6\u0001R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0017\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0015\u0010\u0006\u001a\u00060\u0007j\u0002`\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014\u00a8\u0006#"}, d2 = {"Lio/github/crabzilla/UnitOfWork;", "Ljava/io/Serializable;", "unitOfWorkId", "Ljava/util/UUID;", "command", "Lio/github/crabzilla/Command;", "version", "", "Lio/github/crabzilla/Version;", "events", "", "Lio/github/crabzilla/DomainEvent;", "(Ljava/util/UUID;Lio/github/crabzilla/Command;ILjava/util/List;)V", "getCommand", "()Lio/github/crabzilla/Command;", "getEvents", "()Ljava/util/List;", "getUnitOfWorkId", "()Ljava/util/UUID;", "getVersion", "()I", "component1", "component2", "component3", "component4", "copy", "equals", "", "other", "", "hashCode", "targetId", "Lio/github/crabzilla/EntityId;", "toString", "", "crabzilla-core"})
public final class UnitOfWork implements java.io.Serializable {
    @org.jetbrains.annotations.NotNull()
    private final java.util.UUID unitOfWorkId = null;
    @org.jetbrains.annotations.NotNull()
    private final io.github.crabzilla.Command command = null;
    private final int version = 0;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<io.github.crabzilla.DomainEvent> events = null;
    
    @org.jetbrains.annotations.NotNull()
    public final io.github.crabzilla.EntityId targetId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.UUID getUnitOfWorkId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.github.crabzilla.Command getCommand() {
        return null;
    }
    
    public final int getVersion() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<io.github.crabzilla.DomainEvent> getEvents() {
        return null;
    }
    
    public UnitOfWork(@org.jetbrains.annotations.NotNull()
    java.util.UUID unitOfWorkId, @org.jetbrains.annotations.NotNull()
    io.github.crabzilla.Command command, int version, @org.jetbrains.annotations.NotNull()
    java.util.List<? extends io.github.crabzilla.DomainEvent> events) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.UUID component1() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.github.crabzilla.Command component2() {
        return null;
    }
    
    public final int component3() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<io.github.crabzilla.DomainEvent> component4() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.github.crabzilla.UnitOfWork copy(@org.jetbrains.annotations.NotNull()
    java.util.UUID unitOfWorkId, @org.jetbrains.annotations.NotNull()
    io.github.crabzilla.Command command, int version, @org.jetbrains.annotations.NotNull()
    java.util.List<? extends io.github.crabzilla.DomainEvent> events) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    @java.lang.Override()
    public java.lang.String toString() {
        return null;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object p0) {
        return false;
    }
}
