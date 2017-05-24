package crabzilla.stacks.vertx.verticles;

import com.github.benmanes.caffeine.cache.Cache;
import crabzilla.model.*;
import crabzilla.model.util.Either;
import crabzilla.stack.EventRepository;
import crabzilla.stack.SnapshotReaderFn;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;

import static crabzilla.model.util.Eithers.getLeft;
import static crabzilla.model.util.Eithers.getRight;
import static crabzilla.stack.util.StringHelper.commandHandlerId;

@Slf4j
public class CommandHandlerVerticle<A extends AggregateRoot> extends AbstractVerticle {

  final Class<A> aggregateRootClass;
  final SnapshotReaderFn<A> snapshotReaderFn;
  final CommandHandlerFn<A> cmdHandler;
  final CommandValidatorFn validatorFn;
  final Cache<String, Snapshot<A>> cache;

  final EventRepository eventRepository;
  final Vertx vertx;

  @Inject
  public CommandHandlerVerticle(@NonNull final Class<A> aggregateRootClass,
                                @NonNull final SnapshotReaderFn<A> snapshotReaderFn,
                                @NonNull final CommandHandlerFn<A> cmdHandler,
                                @NonNull final CommandValidatorFn validatorFn,
                                @NonNull final EventRepository eventRepository,
                                @NonNull final Cache<String, Snapshot<A>> cache,
                                @NonNull final Vertx vertx) {
    this.aggregateRootClass = aggregateRootClass;
    this.snapshotReaderFn = snapshotReaderFn;
    this.cmdHandler = cmdHandler;
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

        val command = request.body();
        val constraint = validatorFn.constraintViolation(command);

        if (constraint.isPresent()) {
          throw new IllegalArgumentException(constraint.get());
        }

        val snapshotDataMsg = snapshotReaderFn.getSnapshotMessage(command.getTargetId().getStringValue());

        if (snapshotDataMsg.hasNewSnapshot()) {
          // then populate the cache
          cache.put(command.getCommandId().toString(), snapshotDataMsg.getSnapshot());
        }

        final Either<Exception, UnitOfWork> either = cmdHandler.handle(command, snapshotDataMsg.getSnapshot());

        val optException = getLeft(either);

        if (optException.isPresent()) {
          throw new RuntimeException(optException.get().getMessage(), optException.get());
        }

        val optUnitOfWork = getRight(either);

        if (optUnitOfWork.isPresent()) {

          eventRepository.append(optUnitOfWork.get());

          val options = new DeliveryOptions().setCodecName("CommandCodec");

          vertx.eventBus().send("events-projection", optUnitOfWork.get(), options, asyncResult -> {

            log.info("Successful events-projection? {}", asyncResult.succeeded());

            if (asyncResult.succeeded()) {
              log.info("Result: {}", asyncResult.result().body());
            } else {
              // TODO on error send msg to events pooler
              log.info("Cause: {}", asyncResult.cause());
              log.info("Message: {}", asyncResult.cause().getMessage());
            }

          });

        }

        future.complete(optUnitOfWork.orElse(null));

      }, result -> {

        if (result.succeeded()) {
          log.info("success: {}", result.result());
          request.reply(result.result());
        }
        if (result.failed()) {
          log.info("error: {}", result.cause().getMessage());
          result.cause().printStackTrace();
          request.fail(400, result.cause().getMessage());
        }

      });

    });

  }

}
