package crabzilla.example1.readmodel;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CustomerSummary {
  String id;
  String name;
  Boolean idActive;
}

