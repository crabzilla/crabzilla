CREATE DATABASE example1_read OWNER user1;

\connect example1_read ;

CREATE TABLE customer_summary (
    id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL,
    PRIMARY KEY (id)
);

