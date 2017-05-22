package crabzilla.model;

import java.util.UUID;

public interface Command {

  UUID getCommandId();

  AggregateRootId getTargetId();

}
