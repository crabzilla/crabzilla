package crabzilla.model;

import java.time.Instant;

public interface CommandScheduling extends Event {

  String getTargetAggregateRoot();
  Command getScheduledCommand();
  Instant getScheduledAt();

}
