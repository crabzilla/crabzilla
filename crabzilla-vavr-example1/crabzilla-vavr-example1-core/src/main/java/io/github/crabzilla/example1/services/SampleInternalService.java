package io.github.crabzilla.example1.services;

import java.time.Instant;
import java.util.UUID;

//tag::service[]
public interface SampleInternalService {

  UUID uuid();
  Instant now();
}
//end::service[]