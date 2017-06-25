package crabzilla.model;

import lombok.Value;

import java.beans.ConstructorProperties;
import java.io.Serializable;

@Value
public class Version implements Serializable {

  final long valueAsLong;

  @ConstructorProperties({"valueAsLong"})
  public Version(long valueAsLong) {
    if (valueAsLong < 0) throw new IllegalArgumentException("Version must be = zero or positive");
    this.valueAsLong = valueAsLong;
  }

  public static Version create(long version) {
    return new Version(version);
  }

  public static Version create(int version) {
    return new Version(version);
  }

  public Version nextVersion() {
    return new Version(valueAsLong + 1);
  }

}
