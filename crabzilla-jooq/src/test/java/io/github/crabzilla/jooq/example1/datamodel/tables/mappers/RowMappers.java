package io.github.crabzilla.jooq.example1.datamodel.tables.mappers;

import io.github.crabzilla.jooq.example1.datamodel.tables.pojos.CustomerSummary;
import io.vertx.sqlclient.Row;

import java.util.function.Function;

public class RowMappers {

    private RowMappers(){}

    public static Function<Row, CustomerSummary> getCustomerSummaryMapper() {
        return row -> {
            CustomerSummary pojo = new CustomerSummary();
            pojo.setId(row.getInteger("id"));
            pojo.setName(row.getString("name"));
            pojo.setIsActive(row.getBoolean("is_active"));
            return pojo;
        };
    }

}
