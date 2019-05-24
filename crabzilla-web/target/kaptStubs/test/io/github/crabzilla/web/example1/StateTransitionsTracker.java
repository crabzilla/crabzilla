package io.github.crabzilla.web.example1;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\b\u0006\u0018\u0000*\b\b\u0000\u0010\u0001*\u00020\u00022\u00020\u0003:\u0001\u0018B-\u0012\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00028\u00000\u0005\u0012\u0018\u0010\u0006\u001a\u0014\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00028\u0000\u0012\u0004\u0012\u00028\u00000\u0007\u00a2\u0006\u0002\u0010\tJ)\u0010\u0010\u001a\b\u0012\u0004\u0012\u00028\u00000\u00002\u0018\u0010\u0011\u001a\u0014\u0012\u0004\u0012\u00028\u0000\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00130\u0012H\u0086\bJ\u001a\u0010\u0010\u001a\b\u0012\u0004\u0012\u00028\u00000\u00002\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\b0\u0013J\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\b0\u0013J\u000b\u0010\u0016\u001a\u00028\u0000\u00a2\u0006\u0002\u0010\u0017R \u0010\u0006\u001a\u0014\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00028\u0000\u0012\u0004\u0012\u00028\u00000\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0011\u0010\n\u001a\u00020\u000b8F\u00a2\u0006\u0006\u001a\u0004\b\n\u0010\fR\u0014\u0010\u0004\u001a\b\u0012\u0004\u0012\u00028\u00000\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R$\u0010\r\u001a\u0018\u0012\u0014\u0012\u0012\u0012\u0004\u0012\u00028\u00000\u000fR\b\u0012\u0004\u0012\u00028\u00000\u00000\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0019"}, d2 = {"Lio/github/crabzilla/web/example1/StateTransitionsTracker;", "A", "Lio/github/crabzilla/Entity;", "", "originalSnapshot", "Lio/github/crabzilla/Snapshot;", "applyEventsFn", "Lkotlin/Function2;", "Lio/github/crabzilla/DomainEvent;", "(Lio/github/crabzilla/Snapshot;Lkotlin/jvm/functions/Function2;)V", "isEmpty", "", "()Z", "stateTransitions", "", "Lio/github/crabzilla/web/example1/StateTransitionsTracker$StateTransition;", "applyEvents", "aggregateRootMethodFn", "Lkotlin/Function1;", "", "events", "collectEvents", "currentState", "()Lio/github/crabzilla/Entity;", "StateTransition", "crabzilla-web"})
public final class StateTransitionsTracker<A extends io.github.crabzilla.Entity> {
    private final java.util.List<io.github.crabzilla.web.example1.StateTransitionsTracker<A>.StateTransition<A>> stateTransitions = null;
    private final io.github.crabzilla.Snapshot<A> originalSnapshot = null;
    private final kotlin.jvm.functions.Function2<io.github.crabzilla.DomainEvent, A, A> applyEventsFn = null;
    
    @org.jetbrains.annotations.NotNull()
    public final io.github.crabzilla.web.example1.StateTransitionsTracker<A> applyEvents(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super A, ? extends java.util.List<? extends io.github.crabzilla.DomainEvent>> aggregateRootMethodFn) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<io.github.crabzilla.DomainEvent> collectEvents() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final A currentState() {
        return null;
    }
    
    public final boolean isEmpty() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.github.crabzilla.web.example1.StateTransitionsTracker<A> applyEvents(@org.jetbrains.annotations.NotNull()
    java.util.List<? extends io.github.crabzilla.DomainEvent> events) {
        return null;
    }
    
    public StateTransitionsTracker(@org.jetbrains.annotations.NotNull()
    io.github.crabzilla.Snapshot<A> originalSnapshot, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function2<? super io.github.crabzilla.DomainEvent, ? super A, ? extends A> applyEventsFn) {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\b\u0080\u0004\u0018\u0000*\n\b\u0001\u0010\u0001 \u0001*\u00020\u00022\u00020\u0003B\u0015\u0012\u0006\u0010\u0004\u001a\u00028\u0001\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\u0002\u0010\u0007R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u0013\u0010\u0004\u001a\u00028\u0001\u00a2\u0006\n\n\u0002\u0010\f\u001a\u0004\b\n\u0010\u000b\u00a8\u0006\r"}, d2 = {"Lio/github/crabzilla/web/example1/StateTransitionsTracker$StateTransition;", "T", "Lio/github/crabzilla/Entity;", "", "newInstance", "afterThisEvent", "Lio/github/crabzilla/DomainEvent;", "(Lio/github/crabzilla/web/example1/StateTransitionsTracker;Lio/github/crabzilla/Entity;Lio/github/crabzilla/DomainEvent;)V", "getAfterThisEvent", "()Lio/github/crabzilla/DomainEvent;", "getNewInstance", "()Lio/github/crabzilla/Entity;", "Lio/github/crabzilla/Entity;", "crabzilla-web"})
    public final class StateTransition<T extends io.github.crabzilla.Entity> {
        @org.jetbrains.annotations.NotNull()
        private final T newInstance = null;
        @org.jetbrains.annotations.NotNull()
        private final io.github.crabzilla.DomainEvent afterThisEvent = null;
        
        @org.jetbrains.annotations.NotNull()
        public final T getNewInstance() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final io.github.crabzilla.DomainEvent getAfterThisEvent() {
            return null;
        }
        
        public StateTransition(@org.jetbrains.annotations.NotNull()
        T newInstance, @org.jetbrains.annotations.NotNull()
        io.github.crabzilla.DomainEvent afterThisEvent) {
            super();
        }
    }
}
