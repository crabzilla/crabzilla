package crabzilla.model;

import crabzilla.util.MultiMethod;

import java.lang.reflect.InvocationTargetException;

public abstract class StateTransitionFn<A extends AggregateRoot> {

  private final MultiMethod mm ;

  protected StateTransitionFn() {
    this.mm = MultiMethod.getMultiMethod(this.getClass(), "on");
  }

  public A on(Event event, A instance) {

    try {
      return ((A) mm.invoke(this, event, instance));
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }

    return instance;

  }

}
