CREATE DATABASE example1_read OWNER user1;

\connect example1_read ;

CREATE TABLE events_channels (
    channel_name VARCHAR(36) NOT NULL,
    uow_last_seq INTEGER ,
    PRIMARY KEY (channel_name)
    )
    ;

CREATE TABLE customer_summary (
    id INTEGER NOT NULL,
    name VARCHAR(36) NOT NULL,
    is_active BOOLEAN NOT NULL,
    PRIMARY KEY (id)
    )
