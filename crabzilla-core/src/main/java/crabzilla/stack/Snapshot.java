package crabzilla.stack;

import crabzilla.Version;
import lombok.Value;

@Value
public class Snapshot<A> {

  final A instance;
  final Version version;

}
