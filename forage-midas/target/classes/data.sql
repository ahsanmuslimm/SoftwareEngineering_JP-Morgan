-- ─────────────────────────────────────────────────────────────────────────────
-- data.sql — Initial seed data for Midas Core H2 In-Memory Database
--
-- This file is automatically executed by Spring Boot on startup when
-- spring.jpa.hibernate.ddl-auto is set to create or create-drop.
--
-- These are the user accounts used throughout the 5 task test suites.
-- DO NOT modify user IDs — the test suites reference them by name.
-- ─────────────────────────────────────────────────────────────────────────────

INSERT INTO users (id, balance) VALUES ('waldorf',  1000.00);
INSERT INTO users (id, balance) VALUES ('wilbur',   1000.00);
INSERT INTO users (id, balance) VALUES ('samuel',   1000.00);
INSERT INTO users (id, balance) VALUES ('janet',    1000.00);
INSERT INTO users (id, balance) VALUES ('bill',     1000.00);
INSERT INTO users (id, balance) VALUES ('tim',      1000.00);
INSERT INTO users (id, balance) VALUES ('adam',     1000.00);
INSERT INTO users (id, balance) VALUES ('george',   1000.00);
INSERT INTO users (id, balance) VALUES ('michelle', 1000.00);
INSERT INTO users (id, balance) VALUES ('tyler',    1000.00);
