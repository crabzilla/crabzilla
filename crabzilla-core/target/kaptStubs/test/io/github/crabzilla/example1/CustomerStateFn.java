package io.github.crabzilla.example1;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0003J\u0018\u0010\u0004\u001a\u00020\u00022\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u0002H\u0016J\b\u0010\b\u001a\u00020\u0002H\u0016\u00a8\u0006\t"}, d2 = {"Lio/github/crabzilla/example1/CustomerStateFn;", "Lio/github/crabzilla/EntityStateFunctions;", "Lio/github/crabzilla/example1/Customer;", "()V", "applyEvent", "event", "Lio/github/crabzilla/DomainEvent;", "state", "initialState", "crabzilla-core"})
public final class CustomerStateFn implements io.github.crabzilla.EntityStateFunctions<io.github.crabzilla.example1.Customer> {
    
    @org.jetbrains.annotations.NotNull()
    @java.lang.Override()
    public io.github.crabzilla.example1.Customer initialState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    @java.lang.Override()
    public io.github.crabzilla.example1.Customer applyEvent(@org.jetbrains.annotations.NotNull()
    io.github.crabzilla.DomainEvent event, @org.jetbrains.annotations.NotNull()
    io.github.crabzilla.example1.Customer state) {
        return null;
    }
    
    public CustomerStateFn() {
        super();
    }
}
