package crabzilla.example1.aggregates.customer;

import crabzilla.model.EntityId;
import lombok.Value;

@Value
public class CustomerId implements EntityId {

  String stringValue;

}
