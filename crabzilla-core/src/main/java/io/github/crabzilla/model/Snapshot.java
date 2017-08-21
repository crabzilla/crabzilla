package io.github.crabzilla.model;

import lombok.Value;

@Value
public class Snapshot<A extends Aggregate> {

  private final A instance;
  private final Version version;

  public Version nextVersion() {
    return version.nextVersion();
  }

  public boolean isEmpty() {
    return version.getValueAsLong()==0L;
  }

}
