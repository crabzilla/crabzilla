package crabzilla.stack;

import crabzilla.model.Command;

import java.time.Instant;

public interface CommandScheduling {

  String getTargetAggregateRoot();
  Command getScheduledCommand();
  Instant getScheduledAt();

}
