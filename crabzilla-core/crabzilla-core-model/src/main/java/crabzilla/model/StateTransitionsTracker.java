package crabzilla.model;

import lombok.NonNull;
import lombok.val;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class StateTransitionsTracker<A extends AggregateRoot> {

  final A originalInstance;
  final BiFunction<Event, A, A> applyEventsFn;
  final Function<A, A> dependencyInjectionFn;
  final List<StateTransition<A>> stateTransitions = new ArrayList<>();

  public StateTransitionsTracker(@NonNull A originalInstance,
                                 @NonNull BiFunction<Event, A, A> applyEventsFn,
                                 @NonNull Function<A, A> dependencyInjectionFn) {
    this.originalInstance = originalInstance;
    this.applyEventsFn = applyEventsFn;
    this.dependencyInjectionFn = dependencyInjectionFn;
  }

  public StateTransitionsTracker<A> applyEvents(@NonNull List<Event> events) {
    events.forEach(e -> {
      val newInstance = applyEventsFn.apply(e, currentState());
      stateTransitions.add(new StateTransition<>(newInstance, e));
    });
    return this;
  }

  public StateTransitionsTracker<A> applyEvents(@NonNull Function<A, List<Event>> function) {
    val newEvents = function.apply(currentState());
    return applyEvents(newEvents);
  }

  public List<Event> collectEvents() {
    return stateTransitions.stream().map(t -> t.afterThisEvent).collect(Collectors.toList());
  }

  public A currentState() {
    val current = stateTransitions.size() == 0 ?
            originalInstance : stateTransitions.get(stateTransitions.size() - 1).newInstance;
    System.out.println("*** before injection = " + current);
    val injectedCurrent = dependencyInjectionFn.apply(current);
    System.out.println("*** after injection = " + current);
    return injectedCurrent ;
  }

  public boolean isEmpty() {
    return stateTransitions.isEmpty();
  }

  class StateTransition<T extends AggregateRoot> {
    private final T newInstance;
    private final Event afterThisEvent;

    StateTransition(T newInstance, Event afterThisEvent) {
      requireNonNull(newInstance);
      requireNonNull(afterThisEvent);
      this.newInstance = newInstance;
      this.afterThisEvent = afterThisEvent;
    }
  }
}
