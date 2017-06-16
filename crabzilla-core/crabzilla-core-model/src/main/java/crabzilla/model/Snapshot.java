package crabzilla.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class Snapshot<A> {

  private final A instance;
  private final Version version;

  public Version nextVersion() {
    return version.nextVersion();
  }

  public boolean isEmpty() {
    return version.getValueAsLong()==0L;
  }

}
