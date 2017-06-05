package crabzilla.stack.vertx.verticles;

import com.github.benmanes.caffeine.cache.Cache;
import crabzilla.model.*;
import crabzilla.model.util.Either;
import crabzilla.stack.EventRepository;
import crabzilla.stack.SnapshotReaderFn;
import crabzilla.stack.vertx.CommandExecution;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;

import static crabzilla.model.util.Eithers.getLeft;
import static crabzilla.model.util.Eithers.getRight;
import static crabzilla.stack.util.StringHelper.commandHandlerId;
import static crabzilla.stack.vertx.CommandExecution.*;

@Slf4j
public class CommandHandlerVerticle<A extends AggregateRoot> extends AbstractVerticle {

  final Class<A> aggregateRootClass;
  final SnapshotReaderFn<A> snapshotReaderFn;
  final CommandHandlerFn<A> cmdHandler;
  final CommandValidatorFn validatorFn;
  final Cache<String, Snapshot<A>> cache;

  final EventRepository eventRepository;
  final Vertx vertx;
  final CircuitBreaker circuitBreaker;

  @Inject
  public CommandHandlerVerticle(@NonNull final Class<A> aggregateRootClass,
                                @NonNull final SnapshotReaderFn<A> snapshotReaderFn,
                                @NonNull final CommandHandlerFn<A> cmdHandler,
                                @NonNull final CommandValidatorFn validatorFn,
                                @NonNull final EventRepository eventRepository,
                                @NonNull final Cache<String, Snapshot<A>> cache,
                                @NonNull final Vertx vertx,
                                @NonNull @Named("cmd-handler") final CircuitBreaker circuitBreaker) {
    this.aggregateRootClass = aggregateRootClass;
    this.snapshotReaderFn = snapshotReaderFn;
    this.cmdHandler = cmdHandler;
    this.validatorFn = validatorFn;
    this.eventRepository = eventRepository;
    this.cache = cache;
    this.vertx = vertx;
    this.circuitBreaker = circuitBreaker;
  }

  @Override
  public void start() throws Exception {

    vertx.eventBus().consumer(commandHandlerId(aggregateRootClass), msgHandler());

  }

  Handler<Message<Command>> msgHandler() {

    return (Message<Command> msg) -> {

      vertx.executeBlocking((Future<CommandExecution> future) -> {

        val command = msg.body();

        log.info("received a command {}", command);

        val constraints = validatorFn.constraintViolations(command);

        if (!constraints.isEmpty()) {
          future.complete(VALIDATION_ERROR(command.getCommandId(), constraints));
          return;
        }

        circuitBreaker.fallback(throwable -> {

          log.error("Fallback for command " + command.getCommandId(), throwable);
          return FALLBACK(command.getCommandId()); })

        .execute(cmdHandler(command))

        .setHandler(resultHandler(msg));

      }, false, resultHandler(msg));

    };
  }

  Handler<Future<CommandExecution>> cmdHandler(final Command command) {

    return future -> {

      val snapshotDataMsg = snapshotReaderFn.getSnapshotMessage(command.getTargetId().getStringValue());

      if (snapshotDataMsg.hasNewSnapshot()) {
        cache.put(command.getCommandId().toString(), snapshotDataMsg.getSnapshot());
      }

      final Either<Exception, Optional<UnitOfWork>> either = cmdHandler.handle(command, snapshotDataMsg.getSnapshot());

      val optException = getLeft(either);

      if (optException.isPresent()) {
        log.error("Business logic error for command " + command.getCommandId(), optException.get());
        future.complete(BUSINESS_ERROR(command.getCommandId()));
        return;
      }

      val optUnitOfWork = getRight(either);

      if (!optUnitOfWork.get().isPresent()) {
        future.complete(UNKNOWN_COMMAND(command.getCommandId()));
        return;
      }

      val uow = optUnitOfWork.get().get();
      val uowSequence = eventRepository.append(uow);
      val cmdHandleResp = SUCCESS(uow, uowSequence);

      future.complete(cmdHandleResp);

    };

  }

  Handler<AsyncResult<CommandExecution>> resultHandler(Message<Command> msg) {

    return (AsyncResult<CommandExecution> resultHandler) -> {

      if (resultHandler.succeeded()) {

        val resp = resultHandler.result();
        log.info("success: {}", resp);
        msg.reply(resp);

      } else {

        log.error("error cause: {}", resultHandler.cause());
        log.error("error message: {}", resultHandler.cause().getMessage());
        resultHandler.cause().printStackTrace();
        // TODO customize conform commandResult
        msg.fail(400, resultHandler.cause().getMessage());
      }

    };
  }
}
