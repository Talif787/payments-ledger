-- Runs once on first initialization of the postgres volume, as the superuser.
-- The ledger database is created by POSTGRES_DB; this adds the others.
CREATE DATABASE reconciliation;
CREATE DATABASE fraud;
