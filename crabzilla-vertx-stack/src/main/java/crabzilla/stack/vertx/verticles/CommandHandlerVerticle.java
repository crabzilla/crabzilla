package crabzilla.stack.vertx.verticles;

import com.github.benmanes.caffeine.cache.Cache;
import crabzilla.model.*;
import crabzilla.model.util.Either;
import crabzilla.stack.EventRepository;
import crabzilla.stack.SnapshotReaderFn;
import crabzilla.stack.vertx.CommandHandlingResponse;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Named;

import static crabzilla.model.util.Eithers.getLeft;
import static crabzilla.model.util.Eithers.getRight;
import static crabzilla.stack.util.StringHelper.commandHandlerId;
import static crabzilla.stack.util.StringHelper.eventsHandlerId;
import static crabzilla.stack.vertx.CommandHandlingResponse.*;

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

  final DeliveryOptions uowOptions = new DeliveryOptions().setCodecName("UnitOfWorkCodec");

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

    vertx.eventBus().consumer(commandHandlerId(aggregateRootClass), (Message<Command> request) -> {

      vertx.executeBlocking((Future<CommandHandlingResponse> uowFuture1) -> {

        val command = request.body();

        log.info("received a command {}", command);

        val constraints = validatorFn.constraintViolations(command);

        if (!constraints.isEmpty()) {
          uowFuture1.complete(VALIDATION_ERROR(command.getCommandId(), constraints));
          return ;
        }

        circuitBreaker.fallback(throwable -> {
                          log.error("Fallback for command " + command.getCommandId(), throwable);
                          return FALLBACK(command.getCommandId());
                    }).execute(uowFuture2 -> {

          val snapshotDataMsg = snapshotReaderFn.getSnapshotMessage(command.getTargetId().getStringValue());

          if (snapshotDataMsg.hasNewSnapshot()) {
            cache.put(command.getCommandId().toString(), snapshotDataMsg.getSnapshot());
          }

          final Either<Exception, UnitOfWork> either = cmdHandler.handle(command, snapshotDataMsg.getSnapshot());

          val optException = getLeft(either);

          if (optException.isPresent()) {
            log.error("Business logic error for command " + command.getCommandId(), optException.get());
            uowFuture2.complete(BUSINESS_ERROR(command.getCommandId()));
            return ;
          }

          val optUnitOfWork = getRight(either);

          if (!optUnitOfWork.isPresent()) {
            uowFuture2.complete(UNKNOWN_COMMAND(command.getCommandId()));
            return ;
          }

          val uow = optUnitOfWork.get();

          eventRepository.append(uow);

          uowFuture2.complete(SUCCESS(uow));

        }).setHandler( (AsyncResult<Object> ar2) -> {

          if (ar2.succeeded()) {
            log.info("success: {}", ar2.result());
            // TODO to use another circuit breaker for events publishing
            val resp = (CommandHandlingResponse) ar2.result();
            if (resp.getResult().equals(RESULT.SUCCESS)) {
              vertx.eventBus().publish(eventsHandlerId("example1"), resp.getUnitOfWork().get(), uowOptions);
            }
            request.reply(ar2.result());
          }
          if (ar2.failed()) {
            log.info("error cause: {}", ar2.cause());
            log.info("error message: {}", ar2.cause().getMessage());
            ar2.cause().printStackTrace();
            request.fail(400, ar2.cause().getMessage());
          }

        });

      }, ar1 -> {

        if (ar1.succeeded()) {
          log.info("success: {}", ar1.result());
          request.reply(ar1.result());
        }
        if (ar1.failed()) {
          log.info("error cause: {}", ar1.cause());
          log.info("error message: {}", ar1.cause().getMessage());
          ar1.cause().printStackTrace();
          request.fail(400, ar1.cause().getMessage());
        }

      });

    });

  }

}
