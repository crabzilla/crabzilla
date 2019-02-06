package io.github.crabzilla;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u0000 \u00112\u00020\u0001:\u0001\u0011B!\b\u0002\u0012\b\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u0012\u000e\u0010\u0004\u001a\n\u0018\u00010\u0005j\u0004\u0018\u0001`\u0006\u00a2\u0006\u0002\u0010\u0007J\u001e\u0010\f\u001a\u00020\r2\u0016\u0010\u000e\u001a\u0012\u0012\b\u0012\u00060\u0005j\u0002`\u0006\u0012\u0004\u0012\u00020\r0\u000fJ\u001c\u0010\u0010\u001a\u00020\r2\u0014\u0010\u000e\u001a\u0010\u0012\u0006\u0012\u0004\u0018\u00010\u0003\u0012\u0004\u0012\u00020\r0\u000fR\u0019\u0010\u0004\u001a\n\u0018\u00010\u0005j\u0004\u0018\u0001`\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u0013\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000b\u00a8\u0006\u0012"}, d2 = {"Lio/github/crabzilla/CommandResult;", "", "unitOfWork", "Lio/github/crabzilla/UnitOfWork;", "exception", "Ljava/lang/Exception;", "Lkotlin/Exception;", "(Lio/github/crabzilla/UnitOfWork;Ljava/lang/Exception;)V", "getException", "()Ljava/lang/Exception;", "getUnitOfWork", "()Lio/github/crabzilla/UnitOfWork;", "inCaseOfError", "", "uowFn", "Lkotlin/Function1;", "inCaseOfSuccess", "Companion", "crabzilla-core"})
public final class CommandResult {
    @org.jetbrains.annotations.Nullable()
    private final io.github.crabzilla.UnitOfWork unitOfWork = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Exception exception = null;
    public static final io.github.crabzilla.CommandResult.Companion Companion = null;
    
    public final void inCaseOfSuccess(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super io.github.crabzilla.UnitOfWork, kotlin.Unit> uowFn) {
    }
    
    public final void inCaseOfError(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Exception, kotlin.Unit> uowFn) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final io.github.crabzilla.UnitOfWork getUnitOfWork() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Exception getException() {
        return null;
    }
    
    private CommandResult(io.github.crabzilla.UnitOfWork unitOfWork, java.lang.Exception exception) {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0003\u001a\u00020\u00042\n\u0010\u0005\u001a\u00060\u0006j\u0002`\u0007J\u0010\u0010\b\u001a\u00020\u00042\b\u0010\t\u001a\u0004\u0018\u00010\n\u00a8\u0006\u000b"}, d2 = {"Lio/github/crabzilla/CommandResult$Companion;", "", "()V", "error", "Lio/github/crabzilla/CommandResult;", "e", "Ljava/lang/Exception;", "Lkotlin/Exception;", "success", "uow", "Lio/github/crabzilla/UnitOfWork;", "crabzilla-core"})
    public static final class Companion {
        
        @org.jetbrains.annotations.NotNull()
        public final io.github.crabzilla.CommandResult success(@org.jetbrains.annotations.Nullable()
        io.github.crabzilla.UnitOfWork uow) {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.github.crabzilla.CommandResult error(@org.jetbrains.annotations.NotNull()
        java.lang.Exception e) {
            return null;
        }
        
        private Companion() {
            super();
        }
    }
}
