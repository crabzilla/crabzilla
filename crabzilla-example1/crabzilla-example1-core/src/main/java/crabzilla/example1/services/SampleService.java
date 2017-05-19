package crabzilla.example1.services;

import java.time.LocalDateTime;
import java.util.UUID;

public interface SampleService {

  UUID uuid();

  LocalDateTime now();

}
