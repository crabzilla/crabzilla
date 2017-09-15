package io.github.crabzilla.example1.readmodel;

import lombok.Data;
import lombok.NoArgsConstructor;

//tag::readmodel[]
@Data
@NoArgsConstructor
public class CustomerSummary {

  private String id;
  private String name;
  private Boolean isActive;

  public CustomerSummary(String id, String name, boolean isActive) {
    this.id = id;
    this.name = name;
    this.isActive = isActive;
  }
}
//end::readmodel[]
