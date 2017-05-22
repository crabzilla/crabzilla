package crabzilla;

import lombok.Value;

@Value
public class Version {

  final long valueAsLong;

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
