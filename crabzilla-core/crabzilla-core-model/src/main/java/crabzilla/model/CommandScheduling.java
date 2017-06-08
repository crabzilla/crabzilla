package crabzilla.model;

import java.io.Serializable;
import java.time.Instant;

public interface CommandScheduling extends Serializable {

  String getTargetAggregateRoot();
  Command getScheduledCommand();
  Instant getScheduledAt();

}
