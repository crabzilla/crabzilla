create database example1_read ;

use example1_read ;

DROP TABLE if exists events_channels ;

CREATE TABLE events_channels (
    channel_name VARCHAR(36) NOT NULL,
    uow_last_seq BIGINT ,
    PRIMARY KEY (channel_name)
    )
    ;

DROP TABLE if exists customer_summary ;

CREATE TABLE customer_summary (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(36) NOT NULL,
    is_active BOOLEAN NOT NULL,
    PRIMARY KEY (id)
    )
