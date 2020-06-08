/*
 * This file is generated by jOOQ.
 */
package io.github.crabzilla.hazelcast.query.datamodel.tables.records;


import io.github.crabzilla.hazelcast.query.datamodel.tables.HzProjections;
import io.github.crabzilla.hazelcast.query.datamodel.tables.interfaces.IHzProjections;
import io.github.jklingsporn.vertx.jooq.shared.internal.VertxPojo;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class HzProjectionsRecord extends UpdatableRecordImpl<HzProjectionsRecord> implements VertxPojo, Record3<String, String, Long>, IHzProjections {

    private static final long serialVersionUID = -1759484722;

    /**
     * Setter for <code>public.hz_projections.entityid</code>.
     */
    @Override
    public HzProjectionsRecord setEntityid(String value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>public.hz_projections.entityid</code>.
     */
    @Override
    public String getEntityid() {
        return (String) get(0);
    }

    /**
     * Setter for <code>public.hz_projections.consumerid</code>.
     */
    @Override
    public HzProjectionsRecord setConsumerid(String value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>public.hz_projections.consumerid</code>.
     */
    @Override
    public String getConsumerid() {
        return (String) get(1);
    }

    /**
     * Setter for <code>public.hz_projections.sequence</code>.
     */
    @Override
    public HzProjectionsRecord setSequence(Long value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>public.hz_projections.sequence</code>.
     */
    @Override
    public Long getSequence() {
        return (Long) get(2);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<String> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record3 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row3<String, String, Long> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    @Override
    public Row3<String, String, Long> valuesRow() {
        return (Row3) super.valuesRow();
    }

    @Override
    public Field<String> field1() {
        return HzProjections.HZ_PROJECTIONS.ENTITYID;
    }

    @Override
    public Field<String> field2() {
        return HzProjections.HZ_PROJECTIONS.CONSUMERID;
    }

    @Override
    public Field<Long> field3() {
        return HzProjections.HZ_PROJECTIONS.SEQUENCE;
    }

    @Override
    public String component1() {
        return getEntityid();
    }

    @Override
    public String component2() {
        return getConsumerid();
    }

    @Override
    public Long component3() {
        return getSequence();
    }

    @Override
    public String value1() {
        return getEntityid();
    }

    @Override
    public String value2() {
        return getConsumerid();
    }

    @Override
    public Long value3() {
        return getSequence();
    }

    @Override
    public HzProjectionsRecord value1(String value) {
        setEntityid(value);
        return this;
    }

    @Override
    public HzProjectionsRecord value2(String value) {
        setConsumerid(value);
        return this;
    }

    @Override
    public HzProjectionsRecord value3(Long value) {
        setSequence(value);
        return this;
    }

    @Override
    public HzProjectionsRecord values(String value1, String value2, Long value3) {
        value1(value1);
        value2(value2);
        value3(value3);
        return this;
    }

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    @Override
    public void from(IHzProjections from) {
        setEntityid(from.getEntityid());
        setConsumerid(from.getConsumerid());
        setSequence(from.getSequence());
    }

    @Override
    public <E extends IHzProjections> E into(E into) {
        into.from(this);
        return into;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached HzProjectionsRecord
     */
    public HzProjectionsRecord() {
        super(HzProjections.HZ_PROJECTIONS);
    }

    /**
     * Create a detached, initialised HzProjectionsRecord
     */
    public HzProjectionsRecord(String entityid, String consumerid, Long sequence) {
        super(HzProjections.HZ_PROJECTIONS);

        set(0, entityid);
        set(1, consumerid);
        set(2, sequence);
    }

    public HzProjectionsRecord(io.vertx.core.json.JsonObject json) {
        this();
        fromJson(json);
    }
}
