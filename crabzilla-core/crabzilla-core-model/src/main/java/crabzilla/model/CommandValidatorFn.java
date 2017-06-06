package crabzilla.model;

import crabzilla.model.util.MultiMethod;

import java.util.List;

import static java.util.Collections.singletonList;

public abstract class CommandValidatorFn {

  private final MultiMethod mm ;

  public CommandValidatorFn() {
    this.mm = MultiMethod.getMultiMethod(this.getClass(), "validate");
  }

  @SuppressWarnings(value = "unchecked")
  public List<String> constraintViolations(Command command) {

    if (command == null) {
      return singletonList("Command cannot be null. May be it's json is invalid");
    }

    try {
      return (List<String>) mm.invoke(this, command);
    } catch (Exception e) {
      return singletonList(e.getMessage());
    }

  }

}
