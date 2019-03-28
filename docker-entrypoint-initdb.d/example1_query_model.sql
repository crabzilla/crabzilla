CREATE DATABASE example1_read OWNER user1;

\connect example1_read ;

CREATE TABLE projections (
    name VARCHAR(36) NOT NULL,
    last_uow INTEGER ,
    PRIMARY KEY (name)
    )
    ;

CREATE TABLE customer_summary (
    id INTEGER NOT NULL,
    name VARCHAR(36) NOT NULL,
    is_active BOOLEAN NOT NULL,
    PRIMARY KEY (id)
    )
