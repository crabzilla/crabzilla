package crabzilla.stacks.sql;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import crabzilla.UnitOfWork;
import crabzilla.Version;
import crabzilla.model.Command;
import crabzilla.model.Event;
import crabzilla.stack.EventRepository;
import crabzilla.stack.ProjectionData;
import crabzilla.stack.SnapshotData;
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

public class JdbiEventRepository implements EventRepository {

  static final Logger logger = LoggerFactory.getLogger(JdbiEventRepository.class);

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

  static final String UOW_ID = "uow_id";
  static final String UOW_EVENTS = "uow_events";
  static final String CMD_ID = "cmd_id";
  static final String CMD_DATA = "cmd_data";
  static final String AR_ID = "ar_id";
  static final String AR_NAME = "ar_name";
  static final String VERSION = "version";
  static final String INSERTED_ON = "inserted_on";
  static final String UOW_SEQ_NUMBER = "uow_seq_number";
  static final String NEW_VERSION = "new_version";
  static final String LAST_UPDATED_ON = "last_updated_on";
  static final String CURR_VERSION = "curr_version";

  private final String aggregateRootName;
  private final Gson gson;
  private final DBI dbi;

  private final TypeToken<List<Event>> listTypeToken = new TypeToken<List<Event>>() {
  };

  public JdbiEventRepository(@NonNull String aggregateRootName, @NonNull Gson gson, @NonNull DBI dbi) {
    this.aggregateRootName = aggregateRootName;
    this.gson = gson;
    this.dbi = dbi;
    this.dbi.registerColumnMapper(new LocalDateTimeMapper());

  }

  @Override
  public Optional<UnitOfWork> get(@NonNull UUID uowId) {

    final UnitOfWork uow = dbi
            .withHandle(h -> h.createQuery(getUowSql)
                    .bind(UOW_ID, uowId.toString())
                    .map(new UnitOfWorkMapper()).first()
            );

    return Optional.ofNullable(uow);

  }

  @Override
  public List<ProjectionData> getAllSince(long sinceUowSequence, int maxResultSize) {

    logger.debug("will load a maximum of {} units of work since sequence {}", maxResultSize, sinceUowSequence);

    final List<ProjectionData> projectionDataList = dbi
      .withHandle(new HandleCallback<List<ProjectionData>>() {

        final String sql = String.format("select uow_id, uow_seq_number, ar_id, uow_events " +
                        "from units_of_work where uow_seq_number > %d order by uow_seq_number limit %d",
                sinceUowSequence, maxResultSize);

        public List<ProjectionData> withHandle(Handle h) {
          return h.createQuery(sql)
                  .bind(UOW_SEQ_NUMBER, sinceUowSequence)
                  .map(new ProjectionDataMapper()).list();
        }
      }
    );

    logger.info("Found {} units of work since sequence {}", projectionDataList.size(), sinceUowSequence);
    return projectionDataList;

  }

  @Override
  public SnapshotData getAll(String id) {
    return getAllAfterVersion(id, new Version(0L));
  }

  @Override
  public SnapshotData getAllAfterVersion(@NonNull String id, @NonNull Version version) {

    logger.debug("will load {}", id);

    final List<SnapshotData> eventsListAsJson = dbi
            .withHandle(h -> h.createQuery(getAllAfterVersionSql)
                    .bind(AR_ID, id.toString())
                    .bind(AR_NAME, aggregateRootName)
                    .bind(VERSION, version.getValueAsLong())
                    .map(new SnapshtoDataMapper()).list()
            );

    logger.debug("found {} units of work for id {} and version > {}",
            eventsListAsJson.size(), id.toString(), version.getValueAsLong());

    // TODO use flatmap instead
    final ArrayList<Event> result = new ArrayList<>();
    Version finalVersion = new Version(0L);

    for (SnapshotData snapshotData : eventsListAsJson) {
      snapshotData.getEvents().forEach(e -> result.add(e));
      finalVersion = snapshotData.getVersion();
    }

    return new SnapshotData(finalVersion, result);

  }

  @Override
  public void append(final UnitOfWork unitOfWork) throws DbConcurrencyException {

    requireNonNull(unitOfWork);

    logger.debug("appending uow to units_of_work with id {}", unitOfWork.getTargetId());

    dbi.inTransaction(TransactionIsolationLevel.SERIALIZABLE, (conn, status) -> {

              // TODO check if the command was not handled already

              final Long currentVersion = conn.createQuery(selectAggRootSql)
                      .bind(AR_ID, unitOfWork.getTargetId().getStringValue())
                      .bind(AR_NAME, aggregateRootName)
                      .map(LongColumnMapper.WRAPPER).first();

              newVersionIsCurrentVersionPlus1(unitOfWork, currentVersion);

              int result1;

              if (currentVersion == null) {

                result1 = conn.createStatement(insertAggRootSql)
                        .bind(AR_ID, unitOfWork.getTargetId().getStringValue())
                        .bind(AR_NAME, aggregateRootName)
                        .bind(NEW_VERSION, unitOfWork.getVersion().getValueAsLong())
                        .bind(LAST_UPDATED_ON, new Timestamp(Instant.now().getEpochSecond()))
                        .execute();

              } else {

                result1 = conn.createStatement(updateAggRootSql)
                        .bind(AR_ID, unitOfWork.getTargetId().getStringValue())
                        .bind(AR_NAME, aggregateRootName)
                        .bind(NEW_VERSION, unitOfWork.getVersion().getValueAsLong())
                        .bind(CURR_VERSION, unitOfWork.getVersion().getValueAsLong() - 1)
                        .bind(LAST_UPDATED_ON, new Timestamp(Instant.now().getEpochSecond()))
                        .execute();
              }

              final String cmdAsJson = gson.toJson(unitOfWork.getCommand(), Command.class);
              final String eventsAsJson = gson.toJson(unitOfWork.getEvents(), listTypeToken.getType());

              int result2 = conn.createStatement(insertUowSql)
                      .bind(UOW_ID, unitOfWork.getUnitOfWorkId().toString())
                      .bind(UOW_EVENTS, eventsAsJson)
                      .bind(CMD_ID, unitOfWork.getCommand().getCommandId().toString())
                      .bind(CMD_DATA, cmdAsJson)
                      .bind(AR_ID, unitOfWork.getTargetId().getStringValue())
                      .bind(AR_NAME, aggregateRootName)
                      .bind(VERSION, unitOfWork.getVersion().getValueAsLong())
                      .bind(INSERTED_ON, new Timestamp(Instant.now().getEpochSecond()))
                      .execute();

              // TODO schedular commands emitidos aqui ?? SIM (e remove CommandScheduler)

//              uow.collectEvents().stream()
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


  class SnapshtoDataMapper implements ResultSetMapper<SnapshotData> {
    @Override
    public SnapshotData map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
      final List<Event> events = gson.fromJson(resultSet.getString(UOW_EVENTS), listTypeToken.getType());
      return new SnapshotData(new Version(resultSet.getLong(VERSION)), events);
    }
  }

  class ProjectionDataMapper implements ResultSetMapper<ProjectionData> {
    @Override
    public ProjectionData map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
      final List<Event> events = gson.fromJson(resultSet.getString(UOW_EVENTS), listTypeToken.getType());
      return new ProjectionData(resultSet.getString(UOW_ID),
              resultSet.getLong(UOW_SEQ_NUMBER),
              resultSet.getString(AR_ID),
              events
              );
    }
  }

  class UnitOfWorkMapper implements ResultSetMapper<UnitOfWork> {
    @Override
    public UnitOfWork map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
      final Command command = gson.fromJson(resultSet.getString(CMD_DATA), Command.class);
      final List<Event> events = gson.fromJson(resultSet.getString(UOW_EVENTS), listTypeToken.getType());

      return new UnitOfWork(UUID.fromString(resultSet.getString(UOW_ID)), command,
              new Version(resultSet.getLong(VERSION)),
              events);

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

}