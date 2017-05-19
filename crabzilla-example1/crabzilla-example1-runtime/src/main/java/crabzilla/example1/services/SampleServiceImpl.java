package crabzilla.example1.services;

import java.time.LocalDateTime;
import java.util.UUID;

public class SampleServiceImpl implements SampleService {

  public UUID uuid() {
    return UUID.randomUUID();
  }

  public LocalDateTime now() {
    return LocalDateTime.now();
  }

}
