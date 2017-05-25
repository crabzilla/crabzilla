package crabzilla.example1.services;

import java.time.Instant;
import java.util.UUID;

public interface SampleService {

  UUID uuid();

  Instant now();

}
