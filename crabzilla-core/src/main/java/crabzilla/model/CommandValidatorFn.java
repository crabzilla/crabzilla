package crabzilla.model;

import crabzilla.util.MultiMethod;

import java.util.Optional;

public abstract class CommandValidatorFn {

  private final MultiMethod mm ;

  public CommandValidatorFn() {
    this.mm = MultiMethod.getMultiMethod(this.getClass(), "validate");
  }

  public Optional<String> constraintViolation(Command command) {

    try {
      mm.invoke(this, command);
      return Optional.empty();
    } catch (Exception e) {
      return Optional.of(e.getMessage()); // TODO single or list ?
    }

  }

}
