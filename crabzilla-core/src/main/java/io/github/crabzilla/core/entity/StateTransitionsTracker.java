package io.github.crabzilla.core.entity;

import io.github.crabzilla.core.DomainEvent;
import lombok.NonNull;
import lombok.val;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StateTransitionsTracker<A extends Entity>  {

  private final Snapshot<A> originalSnapshot;
  private final BiFunction<DomainEvent, A, A> applyEventsFn;
  private final List<StateTransition<A>> stateTransitions = new ArrayList<>();

  public StateTransitionsTracker(@NonNull Snapshot<A> originalSnapshot,
                                 @NonNull BiFunction<DomainEvent, A, A> applyEventsFn) {
    this.originalSnapshot = originalSnapshot;
    this.applyEventsFn = applyEventsFn;
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
    val current = isEmpty() ? originalSnapshot.getInstance() :
                              stateTransitions.get(stateTransitions.size() - 1).newInstance;
    return current;
  }

  public boolean isEmpty() {
    return stateTransitions.isEmpty();
  }

  public EntityUnitOfWork unitOfWorkFor(EntityCommand command) {
    return EntityUnitOfWork.unitOfWork(command, originalSnapshot.nextVersion(), collectEvents());
  }

  class StateTransition<T extends Entity> {
    private final T newInstance;
    private final DomainEvent afterThisEvent;

    StateTransition(@NonNull T newInstance, @NonNull DomainEvent afterThisEvent) {
      this.newInstance = newInstance;
      this.afterThisEvent = afterThisEvent;
    }
  }
}
