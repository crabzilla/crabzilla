package io.github.crabzilla.model;

import java.io.Serializable;

public interface Aggregate extends Serializable {

  EntityId getId();

}