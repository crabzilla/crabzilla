\connect crabzilla ;

-- application test data

INSERT INTO subscriptions (name, sequence) values ('crabzilla.example1.customer.SimpleProjector', 0);
INSERT INTO subscriptions (name, sequence) values ('crabzilla.example1.customer.BadProjector', 0);
INSERT INTO subscriptions (name, sequence) values ('crabzilla.example1.customer.CustomProjector', 0);

CREATE TABLE customer_summary (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(100),
    is_active BOOLEAN NOT NULL
);
