package io.github.crabzilla.core.example1.readmodel;

import lombok.Value;

@Value
public class CustomerSummary {
  String id;
  String name;
  Boolean isActive;
}
