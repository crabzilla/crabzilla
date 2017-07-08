package crabzilla.model;

import lombok.NonNull;
import lombok.val;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StateTransitionsTracker<A extends AggregateRoot>  {

  final A originalInstance;
  final BiFunction<Event, A, A> applyEventsFn;
  final Function<A, A> dependencyInjectionFn;
  final List<StateTransition<A>> stateTransitions = new ArrayList<>();

  public StateTransitionsTracker(A originalInstance,
                                 BiFunction<Event, A, A> applyEventsFn,
                                 Function<A, A> dependencyInjectionFn) {
    this.originalInstance = originalInstance;
    this.applyEventsFn = applyEventsFn;
    this.dependencyInjectionFn = dependencyInjectionFn;
  }

  private StateTransitionsTracker<A> applyEvents(@NonNull List<Event> events) {
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
    val current = isEmpty() ? originalInstance : stateTransitions.get(stateTransitions.size() - 1).newInstance;
    return  dependencyInjectionFn.apply(current);
  }

  public boolean isEmpty() {
    return stateTransitions.isEmpty();
  }

  class StateTransition<T extends AggregateRoot> {
    private final T newInstance;
    private final Event afterThisEvent;

    StateTransition(@NonNull T newInstance, @NonNull Event afterThisEvent) {
      this.newInstance = newInstance;
      this.afterThisEvent = afterThisEvent;
    }
  }
}
