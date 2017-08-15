package crabzilla.example1.readmodel;

import lombok.Data;

@Data
public class CustomerSummary {

  String id;
  String name;
  Boolean isActive;

  public CustomerSummary() { // TODO @Data should create this, right?
  }

  public CustomerSummary(String id, String name, boolean isActive) {
    this.id = id;
    this.name = name;
    this.isActive = isActive;
  }

}

