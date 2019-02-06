package io.github.crabzilla;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\b\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\b\u0016\u0018\u0000*\b\b\u0000\u0010\u0001*\u00020\u00022\u00020\u0003B%\u0012\u001e\u0010\u0004\u001a\u001a\u0012\n\u0012\b\u0012\u0004\u0012\u00028\u00000\u0006\u0012\n\u0012\b\u0012\u0004\u0012\u00028\u00000\u00070\u0005\u00a2\u0006\u0002\u0010\bJ4\u0010\t\u001a\b\u0012\u0004\u0012\u00028\u00000\u00062\f\u0010\n\u001a\b\u0012\u0004\u0012\u00028\u00000\u00062\n\u0010\u000b\u001a\u00060\fj\u0002`\r2\f\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00100\u000fR&\u0010\u0004\u001a\u001a\u0012\n\u0012\b\u0012\u0004\u0012\u00028\u00000\u0006\u0012\n\u0012\b\u0012\u0004\u0012\u00028\u00000\u00070\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0011"}, d2 = {"Lio/github/crabzilla/SnapshotPromoter;", "A", "Lio/github/crabzilla/Entity;", "", "trackerFactory", "Lkotlin/Function1;", "Lio/github/crabzilla/Snapshot;", "Lio/github/crabzilla/StateTransitionsTracker;", "(Lkotlin/jvm/functions/Function1;)V", "promote", "originalSnapshot", "newVersion", "", "Lio/github/crabzilla/Version;", "newEvents", "", "Lio/github/crabzilla/DomainEvent;", "crabzilla-core"})
public class SnapshotPromoter<A extends io.github.crabzilla.Entity> {
    private final kotlin.jvm.functions.Function1<io.github.crabzilla.Snapshot<? extends A>, io.github.crabzilla.StateTransitionsTracker<A>> trackerFactory = null;
    
    @org.jetbrains.annotations.NotNull()
    public final io.github.crabzilla.Snapshot<A> promote(@org.jetbrains.annotations.NotNull()
    io.github.crabzilla.Snapshot<? extends A> originalSnapshot, int newVersion, @org.jetbrains.annotations.NotNull()
    java.util.List<? extends io.github.crabzilla.DomainEvent> newEvents) {
        return null;
    }
    
    public SnapshotPromoter(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super io.github.crabzilla.Snapshot<? extends A>, io.github.crabzilla.StateTransitionsTracker<A>> trackerFactory) {
        super();
    }
}
