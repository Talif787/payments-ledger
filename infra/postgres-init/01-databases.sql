-- Runs once on first initialization of the postgres volume, as the superuser.
-- The ledger database is created by POSTGRES_DB; this adds the reconciliation one.
CREATE DATABASE reconciliation;
