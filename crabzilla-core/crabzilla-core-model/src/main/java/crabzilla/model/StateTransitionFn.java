package crabzilla.model;

import crabzilla.model.util.MultiMethod;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class StateTransitionFn<A extends AggregateRoot> {

  private final MultiMethod mm ;

  protected StateTransitionFn() {
    this.mm = MultiMethod.getMultiMethod(this.getClass(), "on");
  }

  public A on(Event event, A instance) {

    try {
      return ((A) mm.invoke(this, event, instance));
    } catch (Exception e) {
      throw new RuntimeException("Errors should never happen when applying events", e);
    }

  }

}
