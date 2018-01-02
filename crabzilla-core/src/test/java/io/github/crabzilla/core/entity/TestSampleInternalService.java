package io.github.crabzilla.core.entity;

import io.github.crabzilla.example1.SampleInternalService;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

public class TestSampleInternalService implements SampleInternalService {
  @NotNull
  @Override
  public UUID uuid() {
    return UUID.randomUUID();
  }

  @NotNull
  @Override
  public Instant now() {
    return Instant.now();
  }
}
