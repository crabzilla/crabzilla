package crabzilla.example1.readmodel;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CustomerSummary {

  String id;
  String name;
  Boolean isActive;

  public CustomerSummary(String id, String name, boolean isActive) {
    this.id = id;
    this.name = name;
    this.isActive = isActive;
  }

}

