package io.github.crabzilla.example1.customer;

import io.github.crabzilla.core.DomainEvent;
import io.github.crabzilla.core.entity.EntityCommand;
import io.github.crabzilla.core.entity.EntityId;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

public class CustomerData {

  @Value
  public static class CustomerId implements EntityId {
    String stringValue;

    @Override
    public String stringValue() {
      return stringValue;
    }
  }

  // tag::commands[]

  @Value
  public static class CreateCustomer implements EntityCommand {
    UUID commandId;
    CustomerId targetId;
    String name;
  }

  @Value
  public static class ActivateCustomer implements EntityCommand {
    UUID commandId;
    CustomerId targetId;
    String reason;
  }

  @Value
  public static class CreateActivateCustomer implements EntityCommand {
    UUID commandId;
    CustomerId targetId;
    String name;
    String reason;
  }

  @Value
  public static class DeactivateCustomer implements EntityCommand {
    UUID commandId;
    CustomerId targetId;
    String reason;
  }

  // end::commands[]

  /**
   * This command is just for testing purposes
   */
  @Value
  public static class UnknownCommand implements EntityCommand {
    UUID commandId;
    CustomerId targetId;
  }

  // tag::events[]

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

  // end::events[]

}
