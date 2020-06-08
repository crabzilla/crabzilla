package io.github.crabzilla.hazelcast.query.datamodel.tables.mappers;

import io.vertx.sqlclient.Row;
import java.util.function.Function;

public class RowMappers {

    private RowMappers(){}

    public static Function<Row,io.github.crabzilla.hazelcast.query.datamodel.tables.pojos.HzProjections> getHzProjectionsMapper() {
        return row -> {
            io.github.crabzilla.hazelcast.query.datamodel.tables.pojos.HzProjections pojo = new io.github.crabzilla.hazelcast.query.datamodel.tables.pojos.HzProjections();
            pojo.setEntityid(row.getString("entityid"));
            pojo.setConsumerid(row.getString("consumerid"));
            pojo.setSequence(row.getLong("sequence"));
            return pojo;
        };
    }

}
