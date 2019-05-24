package io.github.crabzilla;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010!\n\u0002\b\n\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\b\u0002\u0018\u0000*\b\b\u0000\u0010\u0001*\u00020\u00022\u00020\u0003B-\u0012\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00028\u00000\u0005\u0012\u0018\u0010\u0006\u001a\u0014\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00028\u0000\u0012\u0004\u0012\u00028\u00000\u0007\u00a2\u0006\u0002\u0010\tJ)\u0010\u0014\u001a\b\u0012\u0004\u0012\u00028\u00000\u00002\u0018\u0010\u0015\u001a\u0014\u0012\u0004\u0012\u00028\u0000\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00170\u0016H\u0086\bJ\u001a\u0010\u0014\u001a\b\u0012\u0004\u0012\u00028\u00000\u00002\f\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\b0\u0017R\u0017\u0010\n\u001a\b\u0012\u0004\u0012\u00020\b0\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR \u0010\u0006\u001a\u0014\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00028\u0000\u0012\u0004\u0012\u00028\u00000\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001c\u0010\u000e\u001a\u00028\u0000X\u0086\u000e\u00a2\u0006\u0010\n\u0002\u0010\u0013\u001a\u0004\b\u000f\u0010\u0010\"\u0004\b\u0011\u0010\u0012\u00a8\u0006\u0019"}, d2 = {"Lio/github/crabzilla/StateTransitionsTracker;", "A", "Lio/github/crabzilla/Entity;", "", "originalSnapshot", "Lio/github/crabzilla/Snapshot;", "applyEventsFn", "Lkotlin/Function2;", "Lio/github/crabzilla/DomainEvent;", "(Lio/github/crabzilla/Snapshot;Lkotlin/jvm/functions/Function2;)V", "appliedEvents", "", "getAppliedEvents", "()Ljava/util/List;", "currentState", "getCurrentState", "()Lio/github/crabzilla/Entity;", "setCurrentState", "(Lio/github/crabzilla/Entity;)V", "Lio/github/crabzilla/Entity;", "applyEvents", "fn", "Lkotlin/Function1;", "", "events", "crabzilla-core"})
public final class StateTransitionsTracker<A extends io.github.crabzilla.Entity> {
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<io.github.crabzilla.DomainEvent> appliedEvents = null;
    @org.jetbrains.annotations.NotNull()
    private A currentState;
    private final kotlin.jvm.functions.Function2<io.github.crabzilla.DomainEvent, A, A> applyEventsFn = null;
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<io.github.crabzilla.DomainEvent> getAppliedEvents() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final A getCurrentState() {
        return null;
    }
    
    public final void setCurrentState(@org.jetbrains.annotations.NotNull()
    A p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.github.crabzilla.StateTransitionsTracker<A> applyEvents(@org.jetbrains.annotations.NotNull()
    java.util.List<? extends io.github.crabzilla.DomainEvent> events) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.github.crabzilla.StateTransitionsTracker<A> applyEvents(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super A, ? extends java.util.List<? extends io.github.crabzilla.DomainEvent>> fn) {
        return null;
    }
    
    public StateTransitionsTracker(@org.jetbrains.annotations.NotNull()
    io.github.crabzilla.Snapshot<A> originalSnapshot, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function2<? super io.github.crabzilla.DomainEvent, ? super A, ? extends A> applyEventsFn) {
        super();
    }
}
