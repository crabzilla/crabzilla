/*
 * This file is generated by jOOQ.
*/
package example1.datamodel.tables.records;


import example1.datamodel.tables.EventsChannels;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Row2;
import org.jooq.impl.UpdatableRecordImpl;

import javax.annotation.Generated;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.9.2"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class EventsChannelsRecord extends UpdatableRecordImpl<EventsChannelsRecord> implements Record2<String, Long> {

    private static final long serialVersionUID = -1195446895;

    /**
     * Setter for <code>example1db.events_channels.channel_name</code>.
     */
    public EventsChannelsRecord setChannelName(String value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>example1db.events_channels.channel_name</code>.
     */
    public String getChannelName() {
        return (String) get(0);
    }

    /**
     * Setter for <code>example1db.events_channels.uow_last_seq</code>.
     */
    public EventsChannelsRecord setUowLastSeq(Long value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>example1db.events_channels.uow_last_seq</code>.
     */
    public Long getUowLastSeq() {
        return (Long) get(1);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Record1<String> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record2 type implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Row2<String, Long> fieldsRow() {
        return (Row2) super.fieldsRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Row2<String, Long> valuesRow() {
        return (Row2) super.valuesRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field1() {
        return EventsChannels.EVENTS_CHANNELS.CHANNEL_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Long> field2() {
        return EventsChannels.EVENTS_CHANNELS.UOW_LAST_SEQ;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value1() {
        return getChannelName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long value2() {
        return getUowLastSeq();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventsChannelsRecord value1(String value) {
        setChannelName(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventsChannelsRecord value2(Long value) {
        setUowLastSeq(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EventsChannelsRecord values(String value1, Long value2) {
        value1(value1);
        value2(value2);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached EventsChannelsRecord
     */
    public EventsChannelsRecord() {
        super(EventsChannels.EVENTS_CHANNELS);
    }

    /**
     * Create a detached, initialised EventsChannelsRecord
     */
    public EventsChannelsRecord(String channelName, Long uowLastSeq) {
        super(EventsChannels.EVENTS_CHANNELS);

        set(0, channelName);
        set(1, uowLastSeq);
    }
}
