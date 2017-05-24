package crabzilla.model;

import java.io.Serializable;

public interface AggregateRootId extends Serializable {
  String getStringValue();
}
