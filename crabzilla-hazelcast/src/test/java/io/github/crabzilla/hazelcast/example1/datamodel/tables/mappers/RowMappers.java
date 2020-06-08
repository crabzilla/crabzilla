package io.github.crabzilla.hazelcast.example1.datamodel.tables.mappers;

import io.vertx.sqlclient.Row;
import java.util.function.Function;

public class RowMappers {

    private RowMappers(){}

    public static Function<Row,io.github.crabzilla.hazelcast.example1.datamodel.tables.pojos.CustomerSummary> getCustomerSummaryMapper() {
        return row -> {
            io.github.crabzilla.hazelcast.example1.datamodel.tables.pojos.CustomerSummary pojo = new io.github.crabzilla.hazelcast.example1.datamodel.tables.pojos.CustomerSummary();
            pojo.setId(row.getInteger("id"));
            pojo.setName(row.getString("name"));
            pojo.setIsActive(row.getBoolean("is_active"));
            return pojo;
        };
    }

}
