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
    private String stringValue;

    @Override
    public String stringValue() {
      return stringValue;
    }
  }

  // tag::commands[]

  @Value
  public static class CreateCustomer implements EntityCommand {
    private UUID commandId;
    private CustomerId targetId;
    private String name;
  }

  @Value
  public static class ActivateCustomer implements EntityCommand {
    private UUID commandId;
    private CustomerId targetId;
    private String reason;
  }

  @Value
  public static class CreateActivateCustomer implements EntityCommand {
    private UUID commandId;
    private CustomerId targetId;
    private String name;
    private String reason;
  }

  // end::commands[]

  @Value
  public static class DeactivateCustomer implements EntityCommand {
    private UUID commandId;
    private CustomerId targetId;
    private String reason;
  }

  @Value
  public static class UnknownCommand implements EntityCommand {
    private UUID commandId;
    private CustomerData.CustomerId targetId;
  }

  // tag::events[]

  @Value
  public static class CustomerActivated implements DomainEvent {
    private String reason;
    private Instant when;
  }

  @Value
  public static class CustomerCreated implements DomainEvent {
    private CustomerId id;
    private String name;
  }

  @Value
  public static class CustomerDeactivated implements DomainEvent {
    private String reason;
    private Instant when;
  }

  // end::events[]

}
