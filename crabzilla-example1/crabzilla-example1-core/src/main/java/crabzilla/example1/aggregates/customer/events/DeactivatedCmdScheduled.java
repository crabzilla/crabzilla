package crabzilla.example1.aggregates.customer.events;

import crabzilla.example1.aggregates.customer.commands.DeactivateCustomerCmd;
import crabzilla.model.CommandScheduling;
import crabzilla.model.Event;
import lombok.Value;

import java.time.Instant;

@Value
public class DeactivatedCmdScheduled implements Event, CommandScheduling {

  String targetAggregateRoot;
  DeactivateCustomerCmd scheduledCommand;
  Instant scheduledAt;

}
