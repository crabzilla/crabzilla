package crabzilla.example1.services;

import java.time.Instant;
import java.util.UUID;

public class SampleInternalServiceImpl implements SampleInternalService {

  public UUID uuid() {
    return UUID.randomUUID();
  }

  public Instant now() {
    return Instant.now();
  }

}
