package crabzilla.model;

import crabzilla.model.util.MultiMethod;

import java.util.Collections;
import java.util.List;

public abstract class CommandValidatorFn {

  private final MultiMethod mm ;

  public CommandValidatorFn() {
    this.mm = MultiMethod.getMultiMethod(this.getClass(), "validate");
  }

  @SuppressWarnings(value = "unchecked")
  public List<String> constraintViolations(Command command) {

    try {
      return (List<String>) mm.invoke(this, command);
    } catch (Exception e) {
      return Collections.singletonList(e.getMessage());
    }

  }

}
