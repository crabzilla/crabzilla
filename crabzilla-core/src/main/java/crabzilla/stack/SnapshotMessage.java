package crabzilla.stack;

import crabzilla.model.AggregateRoot;
import lombok.Value;

@Value
public class SnapshotMessage<A extends AggregateRoot> {

  public enum LoadedFromEnum {
    FROM_DB, FROM_CACHE, FROM_BOTH
  }

  Snapshot<A> snapshot;
  LoadedFromEnum loadedFromEnum;

}
