/*
 * This file is generated by jOOQ.
 */
package io.github.crabzilla.jooq.example1.datamodel;


import io.github.crabzilla.jooq.example1.datamodel.tables.CustomerSummary;
import io.github.crabzilla.jooq.example1.datamodel.tables.records.CustomerSummaryRecord;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.Internal;


/**
 * A class modelling foreign key relationships and constraints of tables of
 * the <code>public</code> schema.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // IDENTITY definitions
    // -------------------------------------------------------------------------


    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<CustomerSummaryRecord> CUSTOMER_SUMMARY_PKEY = UniqueKeys0.CUSTOMER_SUMMARY_PKEY;

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------


    // -------------------------------------------------------------------------
    // [#1459] distribute members to avoid static initialisers > 64kb
    // -------------------------------------------------------------------------

    private static class UniqueKeys0 {
        public static final UniqueKey<CustomerSummaryRecord> CUSTOMER_SUMMARY_PKEY = Internal.createUniqueKey(CustomerSummary.CUSTOMER_SUMMARY, "customer_summary_pkey", new TableField[] { CustomerSummary.CUSTOMER_SUMMARY.ID }, true);
    }
}