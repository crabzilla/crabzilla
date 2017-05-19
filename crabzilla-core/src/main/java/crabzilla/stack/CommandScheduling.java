package crabzilla.stack;

import crabzilla.model.Command;

import java.time.Instant;

public interface CommandScheduling {

  Command getScheduledCommand();
  Instant getScheduledAt();

}
