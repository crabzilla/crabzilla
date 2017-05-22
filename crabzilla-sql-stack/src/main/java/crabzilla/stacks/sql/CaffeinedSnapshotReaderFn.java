package crabzilla.stacks.sql;


import com.github.benmanes.caffeine.cache.Cache;
import crabzilla.Version;
import crabzilla.model.AggregateRoot;
import crabzilla.model.Event;
import crabzilla.stack.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

@Slf4j
public class CaffeinedSnapshotReaderFn<A extends AggregateRoot> implements SnapshotReaderFn<A> {

	private static final Logger logger = LoggerFactory.getLogger(CaffeinedSnapshotReaderFn.class);

	final Snapshot<A> EMPTY_SNAPSHOT ;

  final Cache<String, Snapshot<A>> cache;
	final EventRepository dao;
  final Supplier<A> supplier;
  final Function<A, A> dependencyInjectionFn;
  final BiFunction<Event, A, A> stateTransitionFn;

	public CaffeinedSnapshotReaderFn(@NonNull Cache<String, Snapshot<A>> cache,
                                   @NonNull EventRepository dao,
                                   @NonNull Supplier<A> supplier,
                                   @NonNull Function<A, A> dependencyInjectionFn,
                                   @NonNull BiFunction<Event, A, A> stateTransitionFn) {
		this.EMPTY_SNAPSHOT = new Snapshot<>(supplier.get(), new Version(0L));
	  this.cache = cache;
		this.dao = dao;
		this.supplier = supplier;
		this.dependencyInjectionFn = dependencyInjectionFn;
		this.stateTransitionFn = stateTransitionFn;
	}

	@Override
	public Snapshot<A> getSnapshot(final String id) {

		requireNonNull(id);

		logger.debug("cache.get(id)", id);

    final AtomicBoolean wasDaoCalled = new AtomicBoolean(false);

    final Snapshot<A> cachedSnapshot = cache.get(id, s -> {
			logger.debug("cache.getInstance(id) does not contain anything for this id. Will have to search on dao", id);
			final SnapshotData dataFromDb = dao.getAll(id);
      wasDaoCalled.set(true);
			return createSnapshot(EMPTY_SNAPSHOT, dataFromDb.getVersion(), dataFromDb.getEvents());
		});

    if (wasDaoCalled.get()) {
      return cachedSnapshot;
    }

		logger.debug("id {} cached lastSnapshotData has version {}. will check if there any version beyond it",
						id, cachedSnapshot.getVersion());

		final SnapshotData nonCachedSnapshotData = dao.getAllAfterVersion(id, cachedSnapshot.getVersion());

		logger.debug("id {} found {} pending transactions. Last version is now {}",
						id, nonCachedSnapshotData.getEvents().size(), nonCachedSnapshotData.getVersion());

		final Version finalVersion = nonCachedSnapshotData.getEvents().isEmpty() ?
            cachedSnapshot.getVersion() : nonCachedSnapshotData.getVersion();

    return createSnapshot(cachedSnapshot, finalVersion, nonCachedSnapshotData.getEvents());

	}

	private Snapshot<A> createSnapshot(Snapshot<A> originalSnapshot, Version newVersion, List<Event> newEvents) {

    final StateTransitionsTracker<A> tracker = new StateTransitionsTracker<>(originalSnapshot.getInstance(),
            stateTransitionFn, dependencyInjectionFn);

    return new Snapshot<>(tracker.applyEvents(newEvents).currentState(), newVersion);
  }

}
