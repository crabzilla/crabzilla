package crabzilla.stack;

import crabzilla.model.AggregateRoot;
import crabzilla.model.Event;
import lombok.extern.slf4j.Slf4j;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.BiFunction;

@Slf4j
public abstract class AbstractStateTransitionFn<A extends AggregateRoot> implements BiFunction<Event, A, A> {

  static final String METHOD_NAME = "on";
  final MethodHandles.Lookup lookup = MethodHandles.lookup();

  @SuppressWarnings(value = "unchecked")
  public A apply(Event event, A instance) {

    final MethodType methodType =
            MethodType.methodType(instance.getClass(), new Class<?>[] {event.getClass(), instance.getClass()});

    try {
      final MethodHandle methodHandle = lookup.bind(this, METHOD_NAME, methodType);
      return (A) methodHandle.invokeWithArguments(event, instance);
    } catch (Throwable e) {
      throw new RuntimeException("Errors should never happen when applying events", e);
    }

  }

}
