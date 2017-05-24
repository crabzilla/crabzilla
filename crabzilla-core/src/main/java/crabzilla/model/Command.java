package crabzilla.model;

import java.io.Serializable;
import java.util.UUID;

public interface Command extends Serializable {

  UUID getCommandId();

  AggregateRootId getTargetId();

}
