/*
 * This file is generated by jOOQ.
 */
package io.github.crabzilla.pgc.jooq.example1.datamodel.tables.interfaces;


import io.github.jklingsporn.vertx.jooq.shared.UnexpectedJsonValueType;
import io.github.jklingsporn.vertx.jooq.shared.internal.VertxPojo;

import java.io.Serializable;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public interface ICustomerSummary extends VertxPojo, Serializable {

    /**
     * Setter for <code>public.customer_summary.id</code>.
     */
    public ICustomerSummary setId(Integer value);

    /**
     * Getter for <code>public.customer_summary.id</code>.
     */
    public Integer getId();

    /**
     * Setter for <code>public.customer_summary.name</code>.
     */
    public ICustomerSummary setName(String value);

    /**
     * Getter for <code>public.customer_summary.name</code>.
     */
    public String getName();

    /**
     * Setter for <code>public.customer_summary.is_active</code>.
     */
    public ICustomerSummary setIsActive(Boolean value);

    /**
     * Getter for <code>public.customer_summary.is_active</code>.
     */
    public Boolean getIsActive();

    // -------------------------------------------------------------------------
    // FROM and INTO
    // -------------------------------------------------------------------------

    /**
     * Load data from another generated Record/POJO implementing the common interface ICustomerSummary
     */
    public void from(ICustomerSummary from);

    /**
     * Copy data into another generated Record/POJO implementing the common interface ICustomerSummary
     */
    public <E extends ICustomerSummary> E into(E into);

    @Override
    public default ICustomerSummary fromJson(io.vertx.core.json.JsonObject json) {
        try {
            setId(json.getInteger("id"));
        } catch (java.lang.ClassCastException e) {
            throw new UnexpectedJsonValueType("id","java.lang.Integer",e);
        }
        try {
            setName(json.getString("name"));
        } catch (java.lang.ClassCastException e) {
            throw new UnexpectedJsonValueType("name","java.lang.String",e);
        }
        try {
            setIsActive(json.getBoolean("is_active"));
        } catch (java.lang.ClassCastException e) {
            throw new UnexpectedJsonValueType("is_active","java.lang.Boolean",e);
        }
        return this;
    }


    @Override
    public default io.vertx.core.json.JsonObject toJson() {
        io.vertx.core.json.JsonObject json = new io.vertx.core.json.JsonObject();
        json.put("id",getId());
        json.put("name",getName());
        json.put("is_active",getIsActive());
        return json;
    }

}
