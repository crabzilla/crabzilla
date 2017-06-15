package crabzilla.stack.model;

import crabzilla.model.AggregateRoot;
import crabzilla.model.Snapshot;
import lombok.Value;

import java.io.Serializable;

@Value
public class SnapshotMessage<A extends AggregateRoot> implements Serializable {

  public enum LoadedFromEnum {
    FROM_DB, FROM_CACHE, FROM_BOTH
  }

  Snapshot<A> snapshot;
  LoadedFromEnum loadedFromEnum;

  public boolean hasNewSnapshot() {
    return !loadedFromEnum.equals(LoadedFromEnum.FROM_CACHE);
  }

}
