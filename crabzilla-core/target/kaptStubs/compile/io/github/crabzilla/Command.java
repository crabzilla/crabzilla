package io.github.crabzilla;

@kotlin.Metadata(mv = {1, 1, 13}, bv = {1, 0, 3}, k = 1, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\bg\u0018\u00002\u00020\u0001R\u0012\u0010\u0002\u001a\u00020\u0003X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0004\u0010\u0005R\u0012\u0010\u0006\u001a\u00020\u0007X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\b\u0010\t\u00a8\u0006\n"}, d2 = {"Lio/github/crabzilla/Command;", "Ljava/io/Serializable;", "commandId", "Ljava/util/UUID;", "getCommandId", "()Ljava/util/UUID;", "targetId", "Lio/github/crabzilla/EntityId;", "getTargetId", "()Lio/github/crabzilla/EntityId;", "crabzilla-core"})
@com.fasterxml.jackson.annotation.JsonTypeInfo(use = com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS, include = com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract interface Command extends java.io.Serializable {
    
    @org.jetbrains.annotations.NotNull()
    public abstract java.util.UUID getCommandId();
    
    @org.jetbrains.annotations.NotNull()
    public abstract io.github.crabzilla.EntityId getTargetId();
}
