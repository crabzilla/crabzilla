package crabzilla.stack.vertx;

import crabzilla.model.AggregateRoot;
import crabzilla.model.Command;
import crabzilla.model.Snapshot;
import crabzilla.model.UnitOfWork;
import crabzilla.model.util.Either;
import crabzilla.stack.EventRepository;
import crabzilla.stack.model.SnapshotMessage;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.shareddata.LocalMap;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static crabzilla.model.util.Eithers.getLeft;
import static crabzilla.model.util.Eithers.getRight;
import static crabzilla.stack.vertx.util.StringHelper.commandHandlerId;
import static java.util.Collections.singletonList;

@Slf4j
public class CommandHandlerVerticle<A extends AggregateRoot> extends AbstractVerticle {

  final Class<A> aggregateRootClass;
  final Function<String, SnapshotMessage<A>> snapshotReaderFn;
  final BiFunction<Command, Snapshot<A>, Either<Exception, Optional<UnitOfWork>>> cmdHandler;
  final Function<Command, List<String>> validatorFn;
  final LocalMap<String, ShareableSnapshot<A>> cache;

  final EventRepository eventRepository;
  final Vertx vertx;
  final CircuitBreaker circuitBreaker;

  @Inject
  public CommandHandlerVerticle(@NonNull final Class<A> aggregateRootClass,
                                @NonNull final Function<String, SnapshotMessage<A>> snapshotReaderFn,
                                @NonNull final BiFunction<Command, Snapshot<A>, Either<Exception, Optional<UnitOfWork>>> cmdHandler,
                                @NonNull final Function<Command, List<String>> validatorFn,
                                @NonNull final EventRepository eventRepository,
                                @NonNull final LocalMap<String, ShareableSnapshot<A>> cache,
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

        if (command==null) {
          future.complete(CommandExecution.VALIDATION_ERROR(singletonList("Command cannot be null. Check if JSON payload is valid.")));
          return;
        }

        log.info("received a command {}", command);

        val constraints = validatorFn.apply(command);

        if (!constraints.isEmpty()) {
          future.complete(CommandExecution.VALIDATION_ERROR(command.getCommandId(), constraints));
          return;
        }

        circuitBreaker.fallback(throwable -> {

          log.error("Fallback for command " + command.getCommandId(), throwable);
          return CommandExecution.FALLBACK(command.getCommandId()); })

        .execute(cmdHandler(command))

        .setHandler(resultHandler(msg));

      }, false, resultHandler(msg));

    };
  }

  Handler<Future<CommandExecution>> cmdHandler(final Command command) {

    return future -> {

      // TODO this call should be a Future ?
      val snapshotDataMsg = snapshotReaderFn.apply(command.getTargetId().getStringValue());

      if (snapshotDataMsg.hasNewSnapshot()) {
        cache.put(command.getCommandId().toString(), new ShareableSnapshot<>(snapshotDataMsg.getSnapshot()));
      }

      final Either<Exception, Optional<UnitOfWork>> either = cmdHandler.apply(command, snapshotDataMsg.getSnapshot());

      val optException = getLeft(either);

      if (optException.isPresent()) {
        log.error("Business logic error for command " + command.getCommandId(), optException.get());
        future.complete(CommandExecution.BUSINESS_ERROR(command.getCommandId()));
        return;
      }

      val optUnitOfWork = getRight(either);

      if (!optUnitOfWork.get().isPresent()) {
        future.complete(CommandExecution.UNKNOWN_COMMAND(command.getCommandId()));
        return;
      }

      val uow = optUnitOfWork.get().get();
      val uowSequence = eventRepository.append(uow);
      val cmdHandleResp = CommandExecution.SUCCESS(uow, uowSequence);

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
