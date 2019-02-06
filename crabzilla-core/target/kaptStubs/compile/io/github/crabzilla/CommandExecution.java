package io.github.crabzilla;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0014\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0002\b\u0004\b\u0086\b\u0018\u00002\u00020\u0001:\u0001&B?\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u0012\u000e\b\u0002\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007\u0012\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\n\u0012\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\f\u00a2\u0006\u0002\u0010\rJ\t\u0010\u0019\u001a\u00020\u0003H\u00c6\u0003J\u000b\u0010\u001a\u001a\u0004\u0018\u00010\u0005H\u00c6\u0003J\u000f\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\b0\u0007H\u00c6\u0003J\u0010\u0010\u001c\u001a\u0004\u0018\u00010\nH\u00c6\u0003\u00a2\u0006\u0002\u0010\u0017J\u000b\u0010\u001d\u001a\u0004\u0018\u00010\fH\u00c6\u0003JL\u0010\u001e\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u00052\u000e\b\u0002\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u00072\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\n2\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00c6\u0001\u00a2\u0006\u0002\u0010\u001fJ\u0013\u0010 \u001a\u00020!2\b\u0010\"\u001a\u0004\u0018\u00010#H\u00d6\u0003J\t\u0010$\u001a\u00020\nH\u00d6\u0001J\t\u0010%\u001a\u00020\bH\u00d6\u0001R\u0013\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u0017\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u0013\u0010\u000b\u001a\u0004\u0018\u00010\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u0015\u0010\t\u001a\u0004\u0018\u00010\n\u00a2\u0006\n\n\u0002\u0010\u0018\u001a\u0004\b\u0016\u0010\u0017\u00a8\u0006\'"}, d2 = {"Lio/github/crabzilla/CommandExecution;", "Ljava/io/Serializable;", "result", "Lio/github/crabzilla/CommandExecution$RESULT;", "commandId", "Ljava/util/UUID;", "constraints", "", "", "uowSequence", "", "unitOfWork", "Lio/github/crabzilla/UnitOfWork;", "(Lio/github/crabzilla/CommandExecution$RESULT;Ljava/util/UUID;Ljava/util/List;Ljava/lang/Integer;Lio/github/crabzilla/UnitOfWork;)V", "getCommandId", "()Ljava/util/UUID;", "getConstraints", "()Ljava/util/List;", "getResult", "()Lio/github/crabzilla/CommandExecution$RESULT;", "getUnitOfWork", "()Lio/github/crabzilla/UnitOfWork;", "getUowSequence", "()Ljava/lang/Integer;", "Ljava/lang/Integer;", "component1", "component2", "component3", "component4", "component5", "copy", "(Lio/github/crabzilla/CommandExecution$RESULT;Ljava/util/UUID;Ljava/util/List;Ljava/lang/Integer;Lio/github/crabzilla/UnitOfWork;)Lio/github/crabzilla/CommandExecution;", "equals", "", "other", "", "hashCode", "toString", "RESULT", "crabzilla-core"})
public final class CommandExecution implements java.io.Serializable {
    @org.jetbrains.annotations.NotNull()
    private final io.github.crabzilla.CommandExecution.RESULT result = null;
    @org.jetbrains.annotations.Nullable()
    private final java.util.UUID commandId = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> constraints = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Integer uowSequence = null;
    @org.jetbrains.annotations.Nullable()
    private final io.github.crabzilla.UnitOfWork unitOfWork = null;
    
    @org.jetbrains.annotations.NotNull()
    public final io.github.crabzilla.CommandExecution.RESULT getResult() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.util.UUID getCommandId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> getConstraints() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer getUowSequence() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final io.github.crabzilla.UnitOfWork getUnitOfWork() {
        return null;
    }
    
    public CommandExecution(@org.jetbrains.annotations.NotNull()
    io.github.crabzilla.CommandExecution.RESULT result, @org.jetbrains.annotations.Nullable()
    java.util.UUID commandId, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> constraints, @org.jetbrains.annotations.Nullable()
    java.lang.Integer uowSequence, @org.jetbrains.annotations.Nullable()
    io.github.crabzilla.UnitOfWork unitOfWork) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.github.crabzilla.CommandExecution.RESULT component1() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.util.UUID component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> component3() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer component4() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final io.github.crabzilla.UnitOfWork component5() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.github.crabzilla.CommandExecution copy(@org.jetbrains.annotations.NotNull()
    io.github.crabzilla.CommandExecution.RESULT result, @org.jetbrains.annotations.Nullable()
    java.util.UUID commandId, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> constraints, @org.jetbrains.annotations.Nullable()
    java.lang.Integer uowSequence, @org.jetbrains.annotations.Nullable()
    io.github.crabzilla.UnitOfWork unitOfWork) {
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
    
    @kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\b\b\u0086\u0001\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006j\u0002\b\u0007j\u0002\b\b\u00a8\u0006\t"}, d2 = {"Lio/github/crabzilla/CommandExecution$RESULT;", "", "(Ljava/lang/String;I)V", "FALLBACK", "VALIDATION_ERROR", "HANDLING_ERROR", "CONCURRENCY_ERROR", "UNKNOWN_COMMAND", "SUCCESS", "crabzilla-core"})
    public static enum RESULT {
        /*public static final*/ FALLBACK /* = new FALLBACK() */,
        /*public static final*/ VALIDATION_ERROR /* = new VALIDATION_ERROR() */,
        /*public static final*/ HANDLING_ERROR /* = new HANDLING_ERROR() */,
        /*public static final*/ CONCURRENCY_ERROR /* = new CONCURRENCY_ERROR() */,
        /*public static final*/ UNKNOWN_COMMAND /* = new UNKNOWN_COMMAND() */,
        /*public static final*/ SUCCESS /* = new SUCCESS() */;
        
        RESULT() {
        }
    }
}
