/*
 * This file is generated by jOOQ.
 */
package io.github.crabzilla.pgc.jooq.example1.datamodel;


import io.github.crabzilla.pgc.jooq.example1.datamodel.tables.CustomerSummary;

import java.util.Arrays;
import java.util.List;

import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Public extends SchemaImpl {

    private static final long serialVersionUID = -445398042;

    /**
     * The reference instance of <code>public</code>
     */
    public static final Public PUBLIC = new Public();

    /**
     * The table <code>public.customer_summary</code>.
     */
    public final CustomerSummary CUSTOMER_SUMMARY = CustomerSummary.CUSTOMER_SUMMARY;

    /**
     * No further instances allowed
     */
    private Public() {
        super("public", null);
    }


    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Table<?>> getTables() {
        return Arrays.<Table<?>>asList(
            CustomerSummary.CUSTOMER_SUMMARY);
    }
}
