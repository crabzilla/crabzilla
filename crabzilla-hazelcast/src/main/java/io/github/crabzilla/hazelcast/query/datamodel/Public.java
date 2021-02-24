/*
 * This file is generated by jOOQ.
 */
package io.github.crabzilla.hazelcast.query.datamodel;


import io.github.crabzilla.hazelcast.query.datamodel.tables.HzProjections;

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

    private static final long serialVersionUID = 68483692;

    /**
     * The reference instance of <code>public</code>
     */
    public static final Public PUBLIC = new Public();

    /**
     * The table <code>public.hz_projections</code>.
     */
    public final HzProjections HZ_PROJECTIONS = HzProjections.HZ_PROJECTIONS;

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
            HzProjections.HZ_PROJECTIONS);
    }
}