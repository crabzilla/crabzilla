package io.github.crabzilla.vertx.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.github.crabzilla.core.Command;
import io.github.crabzilla.core.DomainEvent;
import io.github.crabzilla.core.entity.EntityCommand;
import io.github.crabzilla.core.entity.EntityUnitOfWork;
import io.github.crabzilla.core.entity.SnapshotData;
import io.github.crabzilla.core.entity.Version;
import io.github.crabzilla.core.exceptions.DbConcurrencyException;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.SQLRowStream;
import io.vertx.ext.sql.UpdateResult;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.github.crabzilla.vertx.helpers.VertxSqlHelper.*;
import static io.vertx.core.json.Json.mapper;
import static org.slf4j.LoggerFactory.getLogger;

public class EntityUnitOfWorkRepository {

  static Logger log = getLogger(EntityUnitOfWorkRepository.class);

  private static final String UOW_ID = "uow_id";
  private static final String UOW_EVENTS = "uow_events";
  private static final String CMD_DATA = "cmd_data";
  private static final String VERSION = "version";

  private final String aggregateRootName;
  private final JDBCClient client;

  private final TypeReference<List<DomainEvent>> eventsListTpe = new TypeReference<List<DomainEvent>>() {
  };

  private final String SELECT_UOW_BY_CMD_ID = "select * from units_of_work where cmd_id =? ";
  private final String SELECT_UOW_BY_UOW_ID = "select * from units_of_work where uow_id =? ";

  public EntityUnitOfWorkRepository(Class<?> aggregateRootName, JDBCClient client) {
    this.aggregateRootName = aggregateRootName.getSimpleName();
    this.client = client;
  }

  void getUowByCmdId(final UUID cmdId, final Future<EntityUnitOfWork> uowFuture) {

    get(SELECT_UOW_BY_CMD_ID, cmdId, uowFuture);

  }

  void getUowByUowId(final UUID uowId, final Future<EntityUnitOfWork> uowFuture) {

    get(SELECT_UOW_BY_UOW_ID, uowId, uowFuture);

  }

  void get(String querie, final UUID id, final Future<EntityUnitOfWork> uowFuture) {

    JsonArray params = new JsonArray().add(id.toString());

    client.getConnection(getConn -> {
      if (getConn.failed()) {
        uowFuture.fail(getConn.cause());
        return;
      }

      SQLConnection sqlConn = getConn.result();
      Future<ResultSet> resultSetFuture = Future.future();
      queryWithParams(sqlConn, querie, params, resultSetFuture);

      resultSetFuture.setHandler(resultSetAsyncResult -> {
        if (resultSetAsyncResult.failed()) {
          uowFuture.fail(resultSetAsyncResult.cause());
          return;
        }
        ResultSet rs = resultSetAsyncResult.result();
        List<JsonObject> rows = rs.getRows();
        if (rows.size() == 0) {
          uowFuture.complete(null);
        } else {
          for (JsonObject row : rows) {
            EntityCommand command = Json.decodeValue(row.getString(CMD_DATA), EntityCommand.class);
            final List<DomainEvent> events = readEvents(row.getString(UOW_EVENTS));
            EntityUnitOfWork uow = new EntityUnitOfWork(UUID.fromString(row.getString(UOW_ID)), command,
                    new Version(row.getLong(VERSION)), events);
            uowFuture.complete(uow);
          }
        }
        sqlConn.close(done -> {
          if (done.failed()) {
            log.error("when closing sql connection", done.cause());
          }
        });
      });
    });
  }

  void selectAfterVersion(final String id, final Version version,
                          final Future<SnapshotData> selectAfterVersionFuture) {

    log.info("will load id [{}] after version [{}]", id, version.getValueAsLong());

    String SELECT_AFTER_VERSION = "select uow_events, version from units_of_work " +
            " where ar_id = ? " +
            "   and ar_name = ? " +
            "   and version > ? " +
            " order by version ";

    JsonArray params = new JsonArray().add(id).add(aggregateRootName).add(version.getValueAsLong());

    client.getConnection(getConn -> {
      if (getConn.failed()) {
        selectAfterVersionFuture.fail(getConn.cause());
        return;
      }

      SQLConnection sqlConn = getConn.result();
      Future<SQLRowStream> streamFuture = Future.future();
      queryStreamWithParams(sqlConn, SELECT_AFTER_VERSION, params, streamFuture);

      streamFuture.setHandler(ar -> {

        if (ar.failed()) {
          selectAfterVersionFuture.fail(ar.cause());
          return;
        }

        SQLRowStream stream = ar.result();
        ArrayList<SnapshotData> list = new ArrayList<SnapshotData>();
        stream
                .resultSetClosedHandler(v -> {
                  // will ask to restart the stream with the new result set if any
                  stream.moreResults();
                })
                .handler(row -> {

                  List<DomainEvent> events = readEvents(row.getString(0));
                  SnapshotData snapshotData = new SnapshotData(new Version(row.getLong(1)), events);
                  list.add(snapshotData);
                }).endHandler(event -> {

          log.info("found {} units of work for id {} and version > {}",
                  list.size(), id, version.getValueAsLong());

          Version finalVersion = list.size() == 0 ? new Version(0) :
                  list.get(list.size() - 1).getVersion();

          final List<DomainEvent> flatMappedToEvents = list.stream()
                  .flatMap(sd -> sd.getEvents().stream()).collect(Collectors.toList());

          selectAfterVersionFuture.complete(new SnapshotData(finalVersion, flatMappedToEvents));

          sqlConn.close(done -> {

            if (done.failed()) {
              log.error("when closing sql connection", done.cause());
            }

          });

        });

      });

    });
  }

  void append(final EntityUnitOfWork unitOfWork, Future<Long> appendFuture) {

    String SELECT_CURRENT_VERSION =
            "select max(version) as last_version from units_of_work where ar_id = ? and ar_name = ? ";

    String INSERT_UOW = "insert into units_of_work " +
            "(uow_id, uow_events, cmd_id, cmd_data, ar_id, ar_name, version) " +
            "values (?, ?, ?, ?, ?, ?, ?)";


    client.getConnection(conn -> {

      if (conn.failed()) {
        appendFuture.fail(conn.cause());
        return;
      }

      SQLConnection sqlConn = conn.result();
      Future<Void> startTxFuture = Future.future();

      // start a transaction
      startTx(sqlConn, startTxFuture);

      startTxFuture.setHandler(startTxAsyncResult -> {
        if (startTxAsyncResult.failed()) {
          appendFuture.fail(startTxAsyncResult.cause());
          return;
        }

        // check current version  // TODO also check if command was not already processed given the commandId
        JsonArray params1 = new JsonArray()
                .add(unitOfWork.targetId().stringValue())
                .add(aggregateRootName);

        Future<ResultSet> resultSetFuture = Future.future();
        queryWithParams(sqlConn, SELECT_CURRENT_VERSION, params1, resultSetFuture);
        resultSetFuture.setHandler(asyncResultResultSet -> {

          if (asyncResultResultSet.failed()) {
            appendFuture.fail(asyncResultResultSet.cause());
            return;
          }

          ResultSet rs = asyncResultResultSet.result();
          Long currentVersion = rs.getRows().get(0).getLong("last_version");
          currentVersion = currentVersion == null ? 0L : currentVersion;

          log.info("Found version  {}", currentVersion);

          // apply optimistic locking
          if (currentVersion != unitOfWork.getVersion().getValueAsLong() - 1) {

            DbConcurrencyException error = new DbConcurrencyException(
                    String.format("ar_id = [%s], current_version = %d, new_version = %d",
                            unitOfWork.targetId().stringValue(),
                            currentVersion, unitOfWork.getVersion().getValueAsLong()));

            appendFuture.fail(error);

            // and close the connection
            sqlConn.close(done -> {
              if (done.failed()) {
                log.error("when closing sql connection", done.cause());
              }
            });

            return;
          }

          // if version is OK, then insert
          final String cmdAsJson = commandToJson(unitOfWork.getCommand());
          final String eventsListAsJson = listOfEventsToJson(unitOfWork.getEvents());

          JsonArray params2 = new JsonArray()
                  .add(unitOfWork.getUnitOfWorkId().toString())
                  .add(eventsListAsJson)
                  .add(unitOfWork.getCommand().getCommandId().toString())
                  .add(cmdAsJson)
                  .add(unitOfWork.targetId().stringValue())
                  .add(aggregateRootName)
                  .add(unitOfWork.getVersion().getValueAsLong());

          Future<UpdateResult> updateResultFuture = Future.future();
          updateWithParams(sqlConn, INSERT_UOW, params2, updateResultFuture);

          updateResultFuture.setHandler(asyncResultUpdateResult -> {
            if (asyncResultUpdateResult.failed()) {
              appendFuture.fail(asyncResultUpdateResult.cause());
              return;
            }

            UpdateResult updateResult = asyncResultUpdateResult.result();
            Future<Void> commitFuture = Future.future();

            // commit data
            commitTx(sqlConn, commitFuture);

            commitFuture.setHandler(commitAsyncResult -> {

              if (commitAsyncResult.failed()) {
                appendFuture.fail(commitAsyncResult.cause());
                return;
              }

              appendFuture.complete(updateResult.getKeys().getLong(0));

              // and close the connection
              sqlConn.close(done -> {
                if (done.failed()) {
                  log.error("when closing sql connection", done.cause());
                }
              });
            });

          });

        });

      });

    });

  }

  private String commandToJson(Command command) {
    try {
      String cmdAsJson = mapper.writerFor(Command.class).writeValueAsString(command);
      log.info("commandToJson {}", cmdAsJson);
      return cmdAsJson;
    } catch (JsonProcessingException e) {
      throw new RuntimeException("When writing commandToJson", e);
    }
  }

  private String listOfEventsToJson(List<DomainEvent> events) {
    try {
      String cmdAsJson = mapper.writerFor(eventsListTpe).writeValueAsString(events);
      log.info("listOfEventsToJson {}", cmdAsJson);
      return cmdAsJson;
    } catch (JsonProcessingException e) {
      throw new RuntimeException("When writing listOfEventsToJson", e);
    }
  }

  private List<DomainEvent> readEvents(String eventsAsJson) {
    try {
      log.info("eventsAsJson {}", eventsAsJson);
      return mapper.readerFor(eventsListTpe).readValue(eventsAsJson);
    } catch (IOException e) {
      throw new RuntimeException("When reading events list from JSON", e);
    }
  }

}