package crabzilla.example1.aggregates.customer.commands;

import crabzilla.example1.aggregates.customer.CustomerId;
import crabzilla.model.Command;
import lombok.Value;

import java.util.UUID;

@Value
public class ActivateCustomerCmd implements Command {
  UUID commandId;
  CustomerId targetId;
  String reason;
}
