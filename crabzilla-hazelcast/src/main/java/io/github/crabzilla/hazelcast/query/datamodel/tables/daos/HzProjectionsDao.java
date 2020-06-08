/*
 * This file is generated by jOOQ.
 */
package io.github.crabzilla.hazelcast.query.datamodel.tables.daos;


import io.github.crabzilla.hazelcast.query.datamodel.tables.HzProjections;
import io.github.crabzilla.hazelcast.query.datamodel.tables.records.HzProjectionsRecord;
import io.github.jklingsporn.vertx.jooq.shared.reactive.AbstractReactiveVertxDAO;

import java.util.Collection;

import org.jooq.Configuration;


import java.util.List;
import io.vertx.core.Future;
import io.github.jklingsporn.vertx.jooq.classic.reactivepg.ReactiveClassicQueryExecutor;
/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class HzProjectionsDao extends AbstractReactiveVertxDAO<HzProjectionsRecord, io.github.crabzilla.hazelcast.query.datamodel.tables.pojos.HzProjections, String, Future<List<io.github.crabzilla.hazelcast.query.datamodel.tables.pojos.HzProjections>>, Future<io.github.crabzilla.hazelcast.query.datamodel.tables.pojos.HzProjections>, Future<Integer>, Future<String>> implements io.github.jklingsporn.vertx.jooq.classic.VertxDAO<HzProjectionsRecord,io.github.crabzilla.hazelcast.query.datamodel.tables.pojos.HzProjections,String> {

    /**
     * @param configuration Used for rendering, so only SQLDialect must be set and must be one of the POSTGREs types.
     * @param delegate A configured AsyncSQLClient that is used for query execution
     */
    public HzProjectionsDao(Configuration configuration, io.vertx.sqlclient.SqlClient delegate) {
        super(HzProjections.HZ_PROJECTIONS, io.github.crabzilla.hazelcast.query.datamodel.tables.pojos.HzProjections.class, new ReactiveClassicQueryExecutor<HzProjectionsRecord,io.github.crabzilla.hazelcast.query.datamodel.tables.pojos.HzProjections,String>(configuration,delegate,io.github.crabzilla.hazelcast.query.datamodel.tables.mappers.RowMappers.getHzProjectionsMapper()));
    }

    @Override
    protected String getId(io.github.crabzilla.hazelcast.query.datamodel.tables.pojos.HzProjections object) {
        return object.getConsumerid();
    }

    /**
     * Find records that have <code>entityid IN (values)</code> asynchronously
     */
    public Future<List<io.github.crabzilla.hazelcast.query.datamodel.tables.pojos.HzProjections>> findManyByEntityid(Collection<String> values) {
        return findManyByCondition(HzProjections.HZ_PROJECTIONS.ENTITYID.in(values));
    }

    /**
     * Find records that have <code>sequence IN (values)</code> asynchronously
     */
    public Future<List<io.github.crabzilla.hazelcast.query.datamodel.tables.pojos.HzProjections>> findManyBySequence(Collection<Long> values) {
        return findManyByCondition(HzProjections.HZ_PROJECTIONS.SEQUENCE.in(values));
    }

    @Override
    public ReactiveClassicQueryExecutor<HzProjectionsRecord,io.github.crabzilla.hazelcast.query.datamodel.tables.pojos.HzProjections,String> queryExecutor(){
        return (ReactiveClassicQueryExecutor<HzProjectionsRecord,io.github.crabzilla.hazelcast.query.datamodel.tables.pojos.HzProjections,String>) super.queryExecutor();
    }
}
