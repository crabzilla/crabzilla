package crabzilla.example1.aggregates;

import crabzilla.model.DomainEvent;
import crabzilla.model.EntityCommand;
import crabzilla.model.EntityId;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

public class CustomerData {

  @Value
  public static class CustomerId implements EntityId {
    String stringValue;
  }

  // commands

  @Value
  public static class ActivateCustomerCmd implements EntityCommand {
    UUID commandId;
    CustomerId targetId;
    String reason;
  }

  @Value
  public static class CreateActivateCustomerCmd implements EntityCommand {
    UUID commandId;
    CustomerId targetId;
    String name;
    String reason;
  }

  @Value
  public static class CreateCustomerCmd implements EntityCommand {
    UUID commandId;
    CustomerId targetId;
    String name;
  }

  @Value
  public static class DeactivateCustomerCmd implements EntityCommand {
    UUID commandId;
    CustomerId targetId;
    String reason;
  }

  // events

  @Value
  public static class CustomerActivated implements DomainEvent {
    String reason;
    Instant when;
  }

  @Value
  public static class CustomerCreated implements DomainEvent {
    CustomerId id;
    String name;
  }

  @Value
  public static class CustomerDeactivated implements DomainEvent {
    String reason;
    Instant when;
  }

}
