package crabzilla.stack.vertx;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectWriter;
import crabzilla.model.Command;
import crabzilla.model.Event;
import crabzilla.model.UnitOfWork;
import crabzilla.model.Version;
import crabzilla.stack.EventRepository;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import lombok.NonNull;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static crabzilla.stack.vertx.VertxSqlHelper.*;

public class VertxEventRepository implements EventRepository {

  static final Logger logger = LoggerFactory.getLogger(VertxEventRepository.class);

  static final String UOW_ID = "uow_id";
  static final String UOW_EVENTS = "uow_events";
  static final String CMD_DATA = "cmd_data";
  static final String VERSION = "version";

  private final String aggregateRootName;
  private final JDBCClient client;

  private final TypeReference<List<Event>> eventsListTpe =  new TypeReference<List<Event>>() {};

  public VertxEventRepository(@NonNull String aggregateRootName, @NonNull JDBCClient client) {
    this.aggregateRootName = aggregateRootName;
    this.client = client;
  }

  @Override
  public Optional<UnitOfWork> get(@NonNull final UUID uowId) {

    val result = new AtomicReference<Optional<UnitOfWork>>(Optional.empty());
    val SELECT_UOW_BY_ID = "select * from units_of_work where uow_id =? ";
    val params = new JsonArray().add(uowId.toString());

    client.getConnection(getConn -> {
      if (getConn.succeeded()) {
        val sqlConn = getConn.result();
        sqlConn.queryWithParams(SELECT_UOW_BY_ID, params, res2 -> {
          if (res2.succeeded()) {
            val rs = res2.result();
            val rows = rs.getRows();
            for (JsonObject row : rows) {
              val command = Json.decodeValue(row.getString(CMD_DATA), Command.class);
              final List<Event> events = readEvents(row.getString(UOW_EVENTS));
              val uow = new UnitOfWork(UUID.fromString(row.getString(UOW_ID)), command,
                      new Version(row.getLong(VERSION)), events);
              result.set(Optional.of(uow));
            }
          }
        }).setAutoCommit(false, setCommit -> {
          if (setCommit.succeeded()) {
            logger.info("commit success");
          } else {
            logger.error("commit error");
          }
        }).close();
      } else {
        logger.error("Decide what to do"); // TODO
        // Failed to get connection - deal with it
      }
    });

    return result.get();

  }

  @Override
  public Optional<SnapshotData> getAll(@NonNull final String id) {
    return getAllAfterVersion(id, new Version(0L));
  }

  @Override
  public Optional<SnapshotData> getAllAfterVersion(@NonNull final String id, @NonNull final Version version) {

    logger.info("will load {}", id);

    val SELECT_AFTER_VERSION = "select version, uow_events from units_of_work " +
            " where ar_id = ? and ar_name = ? " +
            "   and version > ? " +
            " order by version";

    val list = new ArrayList<SnapshotData>(); // TODO Why using query stream and populating a List ??!!
    val params = new JsonArray().add(id).add(aggregateRootName).add(version.getValueAsLong());

    client.getConnection(getConn -> {
      if (getConn.succeeded()) {
        val sqlConn = getConn.result();
        sqlConn.queryStreamWithParams(SELECT_AFTER_VERSION, params, stream -> {
          if (stream.succeeded()) {
            stream.result().handler(row -> {
              val events = readEvents(row.getString(0));
              val snapshotData = new SnapshotData(new Version(row.getLong(1)), events);
              list.add(snapshotData);
            });
          }
        }).setAutoCommit(false, setCommit -> {
          if (setCommit.succeeded()) {
            logger.info("commit success");
          } else {
            logger.error("commit error");
          }
        }).close();
      } else {
        logger.error("Decide what to do"); // TODO
        // Failed to get connection - deal with it
      }
    });

    logger.info("found {} units of work for id {} and version > {}",
            list.size(), id, version.getValueAsLong());

    if (list.isEmpty()) {
      return Optional.empty();
    }

    val finalVersion = list.get(list.size() - 1).getVersion();

    final List<Event> flatMappedToEvents = list.stream()
            .flatMap(sd -> sd.getEvents().stream()).collect(Collectors.toList());

    val result = new SnapshotData(finalVersion, flatMappedToEvents);

    return Optional.of(result);

  }

  @Override
  public Long append(@NonNull final UnitOfWork unitOfWork) {

    final AtomicReference<Optional<Long>> currentVersion =
            new AtomicReference<>(Optional.empty());

    final AtomicReference<Long> uowSequence = new AtomicReference<>(0L);

    val SELECT_CURRENT_VERSION =
            "select max(version) from units_of_work where ar_id = ? and ar_name = ? group by ar_id";

    val INSERT_UOW = "insert into units_of_work " +
            "(uow_id, uow_events, cmd_id, cmd_data, ar_id, ar_name, version) " +
            "values (?, ?, ?, ?, ?, ?, ?)";


    client.getConnection(conn -> {

      if (conn.failed()) {
        System.err.println(conn.cause().getMessage());
        return;
      }

      // start a transaction
      startTx(conn.result(), beginTrans -> {

        // check current version

        val params1 = new JsonArray()
                .add(unitOfWork.getUnitOfWorkId().toString())
                .add(aggregateRootName);

        queryWithParams(conn.result(), SELECT_CURRENT_VERSION, params1, rs -> {
          for (JsonObject row : rs.getRows()) {
            currentVersion.set(Optional.of(row.getLong(VERSION)));
          }
        });

        newVersionIsCurrentVersionPlus1(unitOfWork, currentVersion.get().orElse(0L));

        //insert

        val cmdAsJson = writeValueAsString(Json.mapper.writerFor(Command.class), unitOfWork.getCommand());
        val eventsAsJson = writeValueAsString(Json.mapper.writerFor(eventsListTpe), unitOfWork.getEvents());

        val params2 = new JsonArray()
                .add(unitOfWork.getUnitOfWorkId().toString())
                .add(eventsAsJson)
                .add(unitOfWork.getCommand().getCommandId().toString())
                .add(cmdAsJson)
                .add(unitOfWork.targetId().getStringValue())
                .add(aggregateRootName)
                .add(unitOfWork.getVersion().getValueAsLong());

        updateWithParams(conn.result(), INSERT_UOW, params2, updateResult -> {
          uowSequence.set(updateResult.getKeys().getLong(0));
        });

        // commit data
        commitTx(conn.result(), commitTrans -> {

            // and close the connection
            conn.result().close(done -> {
              if (done.failed()) {
                throw new RuntimeException(done.cause());
              }
            });
          });

      });

    });

    // TODO decide about to save scheduled commands here
    ////              uow.collectEvents().stream()
    ////            .filter(event -> event instanceof CommandSchedulingEvent) // TODO idempotency
    ////            .map(event -> (CommandSchedulingEvent) e)
    ////            .forEachOrdered(cs -> commandScheduler.schedule(commandId, cs));

    return uowSequence.get();

  }

  private void newVersionIsCurrentVersionPlus1(UnitOfWork unitOfWork, Long currentVersion) throws EventRepository.DbConcurrencyException {
    if ((currentVersion == null ? 0 : currentVersion) != unitOfWork.getVersion().getValueAsLong() - 1) {
      throw new EventRepository.DbConcurrencyException(
              String.format("ar_id = [%s], current_version = %d, new_version = %d",
                      unitOfWork.targetId().getStringValue(),
                      currentVersion, unitOfWork.getVersion().getValueAsLong()));
    }

  }

  String writeValueAsString(ObjectWriter writer, Object obj) {
    try {
      return writer.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("When writing to JSON", e);
    }
  }

  List<Event> readEvents(String eventsAsJson) {
    try {
      return Json.mapper.readerFor(eventsListTpe).readValue(eventsAsJson);
    } catch (IOException e) {
      throw new RuntimeException("When reading events list from JSON", e);
    }
  }

}