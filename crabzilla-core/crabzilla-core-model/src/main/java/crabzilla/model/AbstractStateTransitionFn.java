package crabzilla.model;

import crabzilla.model.util.MultiMethod;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BiFunction;

@Slf4j
public abstract class AbstractStateTransitionFn<A extends AggregateRoot> implements BiFunction<Event, A, A> {

  private final MultiMethod mm ;

  protected AbstractStateTransitionFn() {
    this.mm = MultiMethod.getMultiMethod(this.getClass(), "on");
  }

  @SuppressWarnings(value = "unchecked")
  public A apply(Event event, A instance) {

    try {
      return ((A) mm.invoke(this, event, instance));
    } catch (Exception e) {
      throw new RuntimeException("Errors should never happen when applying events", e);
    }

  }

}
