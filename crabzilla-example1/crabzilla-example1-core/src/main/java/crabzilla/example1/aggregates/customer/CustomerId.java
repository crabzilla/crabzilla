package crabzilla.example1.aggregates.customer;

import crabzilla.model.AggregateRootId;
import lombok.Value;

@Value
public class CustomerId implements AggregateRootId {

  String stringValue;

}
