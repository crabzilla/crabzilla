package io.github.crabzilla;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000D\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0006\b\u0007\u0018\u00002\u00020\u0001:\u0002#$B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u001e\u001a\u00020\u001fH\u0007J\b\u0010 \u001a\u00020\u001fH\u0007J\b\u0010!\u001a\u00020\u001fH\u0007J\b\u0010\"\u001a\u00020\u001fH\u0007R\u001c\u0010\u0003\u001a\n \u0005*\u0004\u0018\u00010\u00040\u0004X\u0080\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007R\u0014\u0010\b\u001a\u00020\tX\u0080\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0014\u0010\f\u001a\u00020\rX\u0080\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u001a\u0010\u0010\u001a\u00020\u0011X\u0080.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0012\u0010\u0013\"\u0004\b\u0014\u0010\u0015R\u0014\u0010\u0016\u001a\u00020\u0017X\u0080\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019R\u0014\u0010\u001a\u001a\u00020\u001bX\u0080D\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u001d\u00a8\u0006%"}, d2 = {"Lio/github/crabzilla/CommandResultTest;", "", "()V", "commandId", "Ljava/util/UUID;", "kotlin.jvm.PlatformType", "getCommandId$crabzilla_core", "()Ljava/util/UUID;", "customerId", "Lio/github/crabzilla/example1/CustomerId;", "getCustomerId$crabzilla_core", "()Lio/github/crabzilla/example1/CustomerId;", "event", "Lio/github/crabzilla/DomainEvent;", "getEvent$crabzilla_core", "()Lio/github/crabzilla/DomainEvent;", "result", "Lio/github/crabzilla/CommandResult;", "getResult$crabzilla_core", "()Lio/github/crabzilla/CommandResult;", "setResult$crabzilla_core", "(Lio/github/crabzilla/CommandResult;)V", "uow", "Lio/github/crabzilla/UnitOfWork;", "getUow$crabzilla_core", "()Lio/github/crabzilla/UnitOfWork;", "version", "", "getVersion$crabzilla_core", "()I", "errorCanBeInstantiated", "", "errorCannnotBeNull", "successCanBeInstantiated", "successCannotBeNull", "WhenIsError", "WhenIsSuccess", "crabzilla-core"})
@org.junit.jupiter.api.DisplayName(value = "An CommandResult")
public final class CommandResultTest {
    @org.jetbrains.annotations.NotNull()
    public io.github.crabzilla.CommandResult result;
    private final int version = 1;
    @org.jetbrains.annotations.NotNull()
    private final io.github.crabzilla.example1.CustomerId customerId = null;
    private final java.util.UUID commandId = null;
    @org.jetbrains.annotations.NotNull()
    private final io.github.crabzilla.DomainEvent event = null;
    @org.jetbrains.annotations.NotNull()
    private final io.github.crabzilla.UnitOfWork uow = null;
    
    @org.jetbrains.annotations.NotNull()
    public final io.github.crabzilla.CommandResult getResult$crabzilla_core() {
        return null;
    }
    
    public final void setResult$crabzilla_core(@org.jetbrains.annotations.NotNull()
    io.github.crabzilla.CommandResult p0) {
    }
    
    public final int getVersion$crabzilla_core() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.github.crabzilla.example1.CustomerId getCustomerId$crabzilla_core() {
        return null;
    }
    
    public final java.util.UUID getCommandId$crabzilla_core() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.github.crabzilla.DomainEvent getEvent$crabzilla_core() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.github.crabzilla.UnitOfWork getUow$crabzilla_core() {
        return null;
    }
    
    @org.junit.jupiter.api.DisplayName(value = "Success can be instantiated")
    @org.junit.jupiter.api.Test()
    public final void successCanBeInstantiated() {
    }
    
    @org.junit.jupiter.api.DisplayName(value = "Success can not be null")
    @org.junit.jupiter.api.Test()
    public final void successCannotBeNull() {
    }
    
    @org.junit.jupiter.api.DisplayName(value = "Error can be instantiated")
    @org.junit.jupiter.api.Test()
    public final void errorCanBeInstantiated() {
    }
    
    @org.junit.jupiter.api.DisplayName(value = "Error cannot be null")
    @org.junit.jupiter.api.Test()
    public final void errorCannnotBeNull() {
    }
    
    public CommandResultTest() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0006\b\u0087\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\r\u0010\u0003\u001a\u00020\u0004H\u0001\u00a2\u0006\u0002\b\u0005J\r\u0010\u0006\u001a\u00020\u0004H\u0001\u00a2\u0006\u0002\b\u0007J\r\u0010\b\u001a\u00020\u0004H\u0001\u00a2\u0006\u0002\b\t\u00a8\u0006\n"}, d2 = {"Lio/github/crabzilla/CommandResultTest$WhenIsSuccess;", "", "(Lio/github/crabzilla/CommandResultTest;)V", "setUp", "", "setUp$crabzilla_core", "successMustNotRunErrorBlock", "successMustNotRunErrorBlock$crabzilla_core", "successMustRunSuccessBlock", "successMustRunSuccessBlock$crabzilla_core", "crabzilla-core"})
    @org.junit.jupiter.api.DisplayName(value = "When is success")
    @org.junit.jupiter.api.Nested()
    public final class WhenIsSuccess {
        
        @org.junit.jupiter.api.BeforeEach()
        public final void setUp$crabzilla_core() {
        }
        
        @org.junit.jupiter.api.DisplayName(value = "success must run success block")
        @org.junit.jupiter.api.Test()
        public final void successMustRunSuccessBlock$crabzilla_core() {
        }
        
        @org.junit.jupiter.api.DisplayName(value = "success cannot run an error block")
        @org.junit.jupiter.api.Test()
        public final void successMustNotRunErrorBlock$crabzilla_core() {
        }
        
        public WhenIsSuccess() {
            super();
        }
    }
    
    @kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0006\b\u0087\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\r\u0010\u0003\u001a\u00020\u0004H\u0001\u00a2\u0006\u0002\b\u0005J\r\u0010\u0006\u001a\u00020\u0004H\u0001\u00a2\u0006\u0002\b\u0007J\r\u0010\b\u001a\u00020\u0004H\u0001\u00a2\u0006\u0002\b\t\u00a8\u0006\n"}, d2 = {"Lio/github/crabzilla/CommandResultTest$WhenIsError;", "", "(Lio/github/crabzilla/CommandResultTest;)V", "errorMustNotRunSuccessBlock", "", "errorMustNotRunSuccessBlock$crabzilla_core", "errorMustRunErrorBlock", "errorMustRunErrorBlock$crabzilla_core", "setUp", "setUp$crabzilla_core", "crabzilla-core"})
    @org.junit.jupiter.api.DisplayName(value = "When is error")
    @org.junit.jupiter.api.Nested()
    public final class WhenIsError {
        
        @org.junit.jupiter.api.BeforeEach()
        public final void setUp$crabzilla_core() {
        }
        
        @org.junit.jupiter.api.DisplayName(value = "error must run error block")
        @org.junit.jupiter.api.Test()
        public final void errorMustRunErrorBlock$crabzilla_core() {
        }
        
        @org.junit.jupiter.api.DisplayName(value = "error cannot run an success block")
        @org.junit.jupiter.api.Test()
        public final void errorMustNotRunSuccessBlock$crabzilla_core() {
        }
        
        public WhenIsError() {
            super();
        }
    }
}
