package crabzilla.model;

import lombok.NonNull;
import lombok.val;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StateTransitionsTracker<A extends Aggregate>  {

  final A originalInstance;
  final BiFunction<DomainEvent, A, A> applyEventsFn;
  final Function<A, A> dependencyInjectionFn;
  final List<StateTransition<A>> stateTransitions = new ArrayList<>();

  public StateTransitionsTracker(@NonNull A originalInstance,
                                 @NonNull BiFunction<DomainEvent, A, A> applyEventsFn,
                                 @NonNull Function<A, A> dependencyInjectionFn) {
    this.originalInstance = originalInstance;
    this.applyEventsFn = applyEventsFn;
    this.dependencyInjectionFn = dependencyInjectionFn;
  }

  private StateTransitionsTracker<A> applyEvents(@NonNull List<DomainEvent> events) {
    events.forEach(e -> {
      val newInstance = applyEventsFn.apply(e, currentState());
      stateTransitions.add(new StateTransition<>(newInstance, e));
    });
    return this;
  }

  public StateTransitionsTracker<A> applyEvents(@NonNull Function<A, List<DomainEvent>> function) {
    val newEvents = function.apply(currentState());
    return applyEvents(newEvents);
  }

  public List<DomainEvent> collectEvents() {
    return stateTransitions.stream().map(t -> t.afterThisEvent).collect(Collectors.toList());
  }

  public A currentState() {
    val current = isEmpty() ? originalInstance : stateTransitions.get(stateTransitions.size() - 1).newInstance;
    return  dependencyInjectionFn.apply(current);
  }

  public boolean isEmpty() {
    return stateTransitions.isEmpty();
  }

  class StateTransition<T extends Aggregate> {
    private final T newInstance;
    private final DomainEvent afterThisEvent;

    StateTransition(@NonNull T newInstance, @NonNull DomainEvent afterThisEvent) {
      this.newInstance = newInstance;
      this.afterThisEvent = afterThisEvent;
    }
  }
}
