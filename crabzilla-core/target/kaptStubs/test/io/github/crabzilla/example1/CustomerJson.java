package io.github.crabzilla.example1;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0003J\u0018\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\tH\u0016J\u0010\u0010\n\u001a\u00020\t2\u0006\u0010\u000b\u001a\u00020\u0005H\u0016J$\u0010\f\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u000e0\r2\u0006\u0010\u000f\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\tH\u0016J\u0010\u0010\u0010\u001a\u00020\t2\u0006\u0010\u0011\u001a\u00020\u000eH\u0016J\u0010\u0010\u0012\u001a\u00020\u00022\u0006\u0010\b\u001a\u00020\tH\u0016J\u0010\u0010\u0013\u001a\u00020\t2\u0006\u0010\u0014\u001a\u00020\u0002H\u0016\u00a8\u0006\u0015"}, d2 = {"Lio/github/crabzilla/example1/CustomerJson;", "Lio/github/crabzilla/EntityJsonFunctions;", "Lio/github/crabzilla/example1/Customer;", "()V", "cmdFromJson", "Lio/github/crabzilla/Command;", "cmdName", "", "json", "Lio/vertx/core/json/JsonObject;", "cmdToJson", "cmd", "eventFromJson", "Lkotlin/Pair;", "Lio/github/crabzilla/DomainEvent;", "eventName", "eventToJson", "event", "fromJson", "toJson", "entity", "crabzilla-core"})
public final class CustomerJson implements io.github.crabzilla.EntityJsonFunctions<io.github.crabzilla.example1.Customer> {
    
    @org.jetbrains.annotations.NotNull()
    @java.lang.Override()
    public io.vertx.core.json.JsonObject toJson(@org.jetbrains.annotations.NotNull()
    io.github.crabzilla.example1.Customer entity) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    @java.lang.Override()
    public io.github.crabzilla.example1.Customer fromJson(@org.jetbrains.annotations.NotNull()
    io.vertx.core.json.JsonObject json) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    @java.lang.Override()
    public io.github.crabzilla.Command cmdFromJson(@org.jetbrains.annotations.NotNull()
    java.lang.String cmdName, @org.jetbrains.annotations.NotNull()
    io.vertx.core.json.JsonObject json) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    @java.lang.Override()
    public io.vertx.core.json.JsonObject cmdToJson(@org.jetbrains.annotations.NotNull()
    io.github.crabzilla.Command cmd) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    @java.lang.Override()
    public kotlin.Pair<java.lang.String, io.github.crabzilla.DomainEvent> eventFromJson(@org.jetbrains.annotations.NotNull()
    java.lang.String eventName, @org.jetbrains.annotations.NotNull()
    io.vertx.core.json.JsonObject json) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    @java.lang.Override()
    public io.vertx.core.json.JsonObject eventToJson(@org.jetbrains.annotations.NotNull()
    io.github.crabzilla.DomainEvent event) {
        return null;
    }
    
    public CustomerJson() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public io.vertx.core.json.JsonArray toJsonArray(@org.jetbrains.annotations.NotNull()
    java.util.List<? extends kotlin.Pair<java.lang.String, ? extends io.github.crabzilla.DomainEvent>> events) {
        return null;
    }
}
