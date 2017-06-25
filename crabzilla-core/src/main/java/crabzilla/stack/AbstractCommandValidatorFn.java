package crabzilla.stack;

import crabzilla.model.Command;
import crabzilla.model.util.MultiMethod;

import java.util.List;
import java.util.function.Function;

import static java.util.Collections.singletonList;

public abstract class AbstractCommandValidatorFn implements Function<Command, List<String>> {

  private final MultiMethod mm ;

  public AbstractCommandValidatorFn() {
    this.mm = MultiMethod.getMultiMethod(this.getClass(), "validate");
  }

  @SuppressWarnings(value = "unchecked")
  public List<String> apply(Command command) {

    if (command == null) {
      return singletonList("Command cannot be null.");
    }

    try {
      return (List<String>) mm.invoke(this, command);
    } catch (Exception e) {
      return singletonList(e.getMessage());
    }

  }

}
