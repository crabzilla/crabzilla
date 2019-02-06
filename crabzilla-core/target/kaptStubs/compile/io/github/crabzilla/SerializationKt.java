package io.github.crabzilla;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 2, d1 = {"\u0000&\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\u001a\u0016\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000b\u001a\u0016\u0010\f\u001a\u00020\u000b2\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u0007\u001a\u001c\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00030\u00022\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\u000e\u001a\u00020\u000b\u001a\u001c\u0010\u000f\u001a\u00020\u000b2\u0006\u0010\b\u001a\u00020\t2\f\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00030\u0002\"\u001d\u0010\u0000\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00030\u00020\u0001\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0004\u0010\u0005\u00a8\u0006\u0011"}, d2 = {"eventsListType", "Lcom/fasterxml/jackson/core/type/TypeReference;", "", "Lio/github/crabzilla/DomainEvent;", "getEventsListType", "()Lcom/fasterxml/jackson/core/type/TypeReference;", "commandFromJson", "Lio/github/crabzilla/Command;", "mapper", "Lcom/fasterxml/jackson/databind/ObjectMapper;", "command", "", "commandToJson", "listOfEventsFromJson", "eventsAsJson", "listOfEventsToJson", "events", "crabzilla-core"})
public final class SerializationKt {
    @org.jetbrains.annotations.NotNull()
    private static final com.fasterxml.jackson.core.type.TypeReference<java.util.List<io.github.crabzilla.DomainEvent>> eventsListType = null;
    
    @org.jetbrains.annotations.NotNull()
    public static final com.fasterxml.jackson.core.type.TypeReference<java.util.List<io.github.crabzilla.DomainEvent>> getEventsListType() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public static final java.util.List<io.github.crabzilla.DomainEvent> listOfEventsFromJson(@org.jetbrains.annotations.NotNull()
    com.fasterxml.jackson.databind.ObjectMapper mapper, @org.jetbrains.annotations.NotNull()
    java.lang.String eventsAsJson) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String listOfEventsToJson(@org.jetbrains.annotations.NotNull()
    com.fasterxml.jackson.databind.ObjectMapper mapper, @org.jetbrains.annotations.NotNull()
    java.util.List<? extends io.github.crabzilla.DomainEvent> events) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public static final io.github.crabzilla.Command commandFromJson(@org.jetbrains.annotations.NotNull()
    com.fasterxml.jackson.databind.ObjectMapper mapper, @org.jetbrains.annotations.NotNull()
    java.lang.String command) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String commandToJson(@org.jetbrains.annotations.NotNull()
    com.fasterxml.jackson.databind.ObjectMapper mapper, @org.jetbrains.annotations.NotNull()
    io.github.crabzilla.Command command) {
        return null;
    }
}
