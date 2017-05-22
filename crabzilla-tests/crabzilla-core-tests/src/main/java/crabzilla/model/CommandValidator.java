package crabzilla.model;

import crabzilla.util.MultiMethod;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

public abstract class CommandValidator<A extends AggregateRoot> {

  private final MultiMethod mm ;

  protected CommandValidator(MultiMethod mm) {
    this.mm = mm;
  }

  public List<String> constraintViolations(Command command) {

    try {
      return ((List<String>) mm.invoke(this, command));
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }

    return Collections.emptyList();

  }

}
