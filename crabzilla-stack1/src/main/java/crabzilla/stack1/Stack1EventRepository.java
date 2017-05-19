package crabzilla.stack1;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import crabzilla.UnitOfWork;
import crabzilla.Version;
import crabzilla.model.Command;
import crabzilla.model.Event;
import crabzilla.stack.EventRepository;
import crabzilla.stack.ProjectionData;
import crabzilla.stack.SnapshotData;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.Tuple4;
import lombok.NonNull;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.tweak.ResultColumnMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.util.LongColumnMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class Stack1EventRepository implements EventRepository {

  static final Logger logger = LoggerFactory.getLogger(Stack1EventRepository.class);

  static final String getUowSql = "select * from units_of_work where uow_id = :uow_id ";

  static final String getAllAfterVersionSql = "select version, uow_events " +
                                                "from units_of_work where ar_id = :ar_id and ar_name = :ar_name " +
                                                " and version > :version " +
                                                "order by version";

  static final String selectAggRootSql =
          "select version from aggregate_roots where ar_id = :ar_id and ar_name = :ar_name";

  static final String insertAggRootSql = "insert into aggregate_roots (ar_id, ar_name, version, last_updated_on) " +
        "values (:ar_id, :ar_name, :new_version, :last_updated_on) ";

  static final String updateAggRootSql =
          "update aggregates_root set version_number = :new_version, last_updated_on = :last_updated_on " +
        "where ar_id = :ar_id and ar_name = :ar_name and version_number = :curr_version";

  static final String insertUowSql = "insert into units_of_work " +
        "(uow_id, uow_events, cmd_id, cmd_data, ar_id, ar_name, version, inserted_on) " +
        "values (:uow_id, :uow_events, :cmd_id, :cmd_data, :ar_id, :ar_name, :version, :inserted_on)";

  private final String aggregateRootName;
  private final Gson gson;
  private final DBI dbi;

  private final TypeToken<List<Event>> listTypeToken = new TypeToken<List<Event>>() {};

  public Stack1EventRepository(@NonNull String aggregateRootName, @NonNull Gson gson, @NonNull DBI dbi) {
    this.aggregateRootName = aggregateRootName;
    this.gson = gson;
    this.dbi = dbi;
    this.dbi.registerColumnMapper(new LocalDateTimeMapper());

  }

  @Override
  public Optional<UnitOfWork> get(@NonNull UUID uowId) {

    final Tuple4<String, String, Long, String> uowTuple = dbi
        .withHandle(h -> h.createQuery(getUowSql)
        .bind("uow_id", uowId.toString())
        .map(new UnitOfWorkMapper()).first()
      );

    final Command command = gson.fromJson(uowTuple._2(), Command.class);
    final List<Event> events = gson.fromJson(uowTuple._4(), listTypeToken.getType());
    final UnitOfWork uow = new UnitOfWork(UUID.fromString(uowTuple._1()), command,  new Version(uowTuple._3()), events);

    return Optional.of(uow);

  }

  @Override
  public List<ProjectionData> getAllSince(long sinceUowSequence, int maxResultSize) {

    logger.debug("will load a maximum of {} units of work since sequence {}", maxResultSize, sinceUowSequence);

    final List<Tuple4<String, Long, String, String>> eventsListAsJson = dbi
            .withHandle(new HandleCallback<List<Tuple4<String, Long, String, String>>>() {

                          final String sql = String.format("select uow_id, uow_seq_number, ar_id, uow_events " +
                                  "from units_of_work where uow_seq_number > %d order by uow_seq_number limit %d",
                                  sinceUowSequence, maxResultSize);

                          public List<Tuple4<String, Long, String, String>> withHandle(Handle h) {
                            return h.createQuery(sql)
                                    .bind("uow_seq_number", sinceUowSequence)
                                    .map(new ListOfEventsMapper()).list();
                          }
                        }
            );

    if (eventsListAsJson == null) {

      logger.info("Found none unit of work since sequence {}", sinceUowSequence);

      return new ArrayList<>();

    }

    logger.info("Found {} units of work since sequence {}", eventsListAsJson.size(), sinceUowSequence);

    final ArrayList<ProjectionData> result = new ArrayList<>();

    for (Tuple4<String, Long, String, String> tuple : eventsListAsJson) {
      final List<Event> events = gson.fromJson(tuple._4(), listTypeToken.getType());
      events.forEach(e ->
              result.add(new ProjectionData(tuple._1(), tuple._2(), tuple._3(), events)));
    }

    return result;

  }

  @Override
  public SnapshotData getAll(String id) {
    return getAllAfterVersion(id, new Version(0L));
  }

  @Override
  public SnapshotData getAllAfterVersion(@NonNull String id, @NonNull Version version) {

    logger.debug("will load {}", id);

    final List<Tuple2<Long, String>> eventsListAsJson = dbi
      .withHandle(h -> h.createQuery(getAllAfterVersionSql)
              .bind("ar_id", id.toString())
              .bind("version", version.getValueAsLong())
              .map(new EventsMapper()).list()
      );

    if (eventsListAsJson == null) {

      logger.debug("found none unit of work for id {} and version > {}",
              id.toString(), version.getValueAsLong());

      return new SnapshotData(Version.create(0), new ArrayList<>());

    }

    logger.debug("found {} units of work for id {} and version > {}",
            eventsListAsJson.size(), id.toString(), version.getValueAsLong());

    final ArrayList<Event> result = new ArrayList<>();
    Long finalVersion = 0L;

    for (Tuple2<Long, String> tuple : eventsListAsJson) {
      logger.debug("converting to List<Event> from {}", tuple);
      final List<Event> events = gson.fromJson(tuple._2(), listTypeToken.getType());
      logger.debug(events.toString());
      events.forEach(e -> result.add(e));
      finalVersion = tuple._1();
    }

    return new SnapshotData(new Version(finalVersion), result);

  }

  @Override
  public void append(final UnitOfWork unitOfWork) throws DbConcurrencyException {

    requireNonNull(unitOfWork);

    logger.debug("appending uow to units_of_work with id {}", unitOfWork.getTargetId());

    dbi.inTransaction(TransactionIsolationLevel.SERIALIZABLE, (conn, status) -> {

      final Long currentVersion = conn.createQuery(selectAggRootSql)
              .bind("ar_id", unitOfWork.getTargetId().getStringValue())
              .bind("ar_name", aggregateRootName)
              .map(LongColumnMapper.WRAPPER).first();

      newVersionIsCurrentVersionPlus1(unitOfWork, currentVersion);

              int result1;

      if (currentVersion == null) {

        result1 = conn.createStatement(insertAggRootSql)
                .bind("ar_id", unitOfWork.getTargetId().getStringValue())
                .bind("ar_name", aggregateRootName)
                .bind("new_version", unitOfWork.getVersion().getValueAsLong())
                .bind("last_updated_on", new Timestamp(Instant.now().getEpochSecond()))
                .execute();

      } else {

        result1 = conn.createStatement(updateAggRootSql)
                .bind("ar_id", unitOfWork.getTargetId().getStringValue())
                .bind("ar_name", aggregateRootName)
                .bind("new_version", unitOfWork.getVersion().getValueAsLong())
                .bind("curr_version", unitOfWork.getVersion().getValueAsLong() - 1)
                .bind("last_updated_on", new Timestamp(Instant.now().getEpochSecond()))
                .execute();
      }

      final String cmdAsJson = gson.toJson(unitOfWork.getCommand(), Command.class);
      final String eventsAsJson = gson.toJson(unitOfWork.getEvents(), listTypeToken.getType());

      int result2 = conn.createStatement(insertUowSql)
        .bind("uow_id", unitOfWork.getUnitOfWorkId().toString())
        .bind("uow_events", eventsAsJson)
        .bind("cmd_id", unitOfWork.getCommand().getCommandId().toString())
        .bind("cmd_data", cmdAsJson)
        .bind("ar_id", unitOfWork.getTargetId().getStringValue())
        .bind("ar_name", aggregateRootName)
        .bind("version", unitOfWork.getVersion().getValueAsLong())
        .bind("inserted_on", new Timestamp(Instant.now().getEpochSecond()))
        .execute();

      // TODO schedular commands emitidos aqui ?? SIM (e remove CommandScheduler)

//              uow.getEvents().stream()
//            .filter(event -> event instanceof CommandScheduling) // TODO tem que ter idempotency disto
//            .map(event -> (CommandScheduling) e)
//            .forEachOrdered(cs -> commandScheduler.schedule(commandId, cs));

      if (result1 + result2 == 2) {
        return true;
      }

      throw new DbConcurrencyException(
              String.format("id = [%s], current_version = %d, new_version = %d",
                      unitOfWork.getTargetId().getStringValue(),
                      currentVersion, unitOfWork.getVersion().getValueAsLong()));

    }

    );

  }

  private void newVersionIsCurrentVersionPlus1(UnitOfWork unitOfWork, Long currentVersion) throws DbConcurrencyException {
    if ((currentVersion == null ? 0 : currentVersion) != unitOfWork.getVersion().getValueAsLong() - 1) {
      throw new DbConcurrencyException(
              String.format("ar_id = [%s], current_version = %d, new_version = %d",
                      unitOfWork.getTargetId().getStringValue(),
                      currentVersion, unitOfWork.getVersion().getValueAsLong()));
    }
  }

}

class EventsMapper implements ResultSetMapper<Tuple2<Long, String>> {
  @Override
  public Tuple2<Long, String> map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
    return Tuple.of(resultSet.getLong("version"), resultSet.getString("uow_events"));
  }
}

class ListOfEventsMapper implements ResultSetMapper<Tuple4<String, Long, String, String>> {
  @Override
  public Tuple4<String, Long, String, String> map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
    return Tuple.of(resultSet.getString("uow_id"),
                    resultSet.getLong("uow_seq_number"),
                    resultSet.getString("ar_id"),
                    resultSet.getString("uow_events"));
  }
}

class UnitOfWorkMapper implements ResultSetMapper<Tuple4<String, String, Long, String>> {
  @Override
  public Tuple4<String, String, Long, String> map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
    return Tuple.of(resultSet.getString("uow_id"),
            resultSet.getString("cmd_data"),
            resultSet.getLong("version"),
            resultSet.getString("uow_events"));

  }
}

class LocalDateTimeMapper implements ResultColumnMapper<LocalDateTime> {

  @Override
  public LocalDateTime mapColumn(ResultSet r, String columnLabel, StatementContext ctx) throws SQLException {
    final Timestamp timestamp = r.getTimestamp(columnLabel);
    if (timestamp == null) {
      return null;
    }
    return timestamp.toLocalDateTime();
  }

  @Override
  public LocalDateTime mapColumn(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
    final Timestamp timestamp = r.getTimestamp(columnNumber);
    if (timestamp == null) {
      return null;
    }
    return timestamp.toLocalDateTime();
  }
}