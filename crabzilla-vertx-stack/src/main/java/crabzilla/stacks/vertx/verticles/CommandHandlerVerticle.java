package crabzilla.stacks.vertx.verticles;

import com.github.benmanes.caffeine.cache.Cache;
import crabzilla.UnitOfWork;
import crabzilla.model.AggregateRoot;
import crabzilla.model.Command;
import crabzilla.model.CommandHandlerFn;
import crabzilla.model.CommandValidatorFn;
import crabzilla.stack.EventRepository;
import crabzilla.stack.Snapshot;
import crabzilla.stack.SnapshotMessage;
import crabzilla.stack.SnapshotReaderFn;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import java.util.Optional;

import static crabzilla.util.StringHelper.commandHandlerId;

@Slf4j
public class CommandHandlerVerticle<A extends AggregateRoot> extends AbstractVerticle {

  final Class<A> aggregateRootClass;
  final SnapshotReaderFn<A> snapshotReaderFn;
  final CommandHandlerFn<A> handler;
  final CommandValidatorFn validatorFn;
  final EventRepository eventRepository;
  final Cache<String, Snapshot<A>> cache;
  final Vertx vertx;

  @Inject
  public CommandHandlerVerticle(@NonNull final Class<A> aggregateRootClass,
                                @NonNull final SnapshotReaderFn<A> snapshotReaderFn,
                                @NonNull final CommandHandlerFn<A> handler,
                                @NonNull final CommandValidatorFn validatorFn,
                                @NonNull final EventRepository eventRepository,
                                @NonNull final Cache<String, Snapshot<A>> cache,
                                @NonNull final Vertx vertx) {
    this.aggregateRootClass = aggregateRootClass;
    this.snapshotReaderFn = snapshotReaderFn;
    this.handler = handler;
    this.validatorFn = validatorFn;
    this.eventRepository = eventRepository;
    this.cache = cache;
    this.vertx = vertx;
  }

  @Override
  public void start() throws Exception {

    vertx.eventBus().consumer(commandHandlerId(aggregateRootClass), (Message<Command> request) -> {

      vertx.executeBlocking((Future<UnitOfWork> future) -> {

        log.info("received a command {}", request.body());

        final Command command = request.body();

        val constraint = validatorFn.constraintViolation(command);

        if (constraint.isPresent()) {
          throw new IllegalArgumentException(constraint.get());
        }

        final SnapshotMessage<A> snapshotDataMsg = snapshotReaderFn.getSnapshotMessage(command.getTargetId().getStringValue());
        final Snapshot<A> snapshot = snapshotDataMsg.getSnapshot();

        if (ifSnapshotIsNotFromCache(snapshotDataMsg)) {
          // then populate the cache
          cache.put(command.getCommandId().toString(), snapshotDataMsg.getSnapshot());
        }

        final Optional<UnitOfWork> unitOfWork = handler.handle(command, snapshot);

        if (unitOfWork.isPresent()) {
          eventRepository.append(unitOfWork.get());
          vertx.eventBus().publish("events-projection",  unitOfWork.get());
        }

        future.complete(unitOfWork.orElse(null));

      }, result -> {

        if (result.succeeded()) {
          log.info("success: {}", result.result());
          request.reply(result.result());
        }
        if (result.failed()) {
          log.info("error: {}", result.cause().getMessage());
          request.reply(result.cause().getMessage());
        }

      });

    });

  }

  /**
   * Test if snapshot isn't from cache
   * @param snapshotDataMsg
   * @return boolean
  */
  private boolean ifSnapshotIsNotFromCache(SnapshotMessage<A> snapshotDataMsg) {
    return !snapshotDataMsg.getLoadedFromEnum().equals(SnapshotMessage.LoadedFromEnum.FROM_CACHE);
  }

}
