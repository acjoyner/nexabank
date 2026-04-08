-- =============================================================================
-- NexaBank Platform — PostgreSQL Database Initialization
-- Mounted at /docker-entrypoint-initdb.d/ and executed once on first boot
-- =============================================================================
-- The postgres container creates the default 'nexabank' database.
-- Each microservice connects to its own dedicated database.
-- This script creates those databases and grants the nexabank user access.
-- =============================================================================

-- account-service database
CREATE DATABASE nexabank_accounts
    WITH OWNER nexabank
    ENCODING 'UTF8'
    LC_COLLATE 'en_US.utf8'
    LC_CTYPE 'en_US.utf8';

-- transaction-service database
CREATE DATABASE nexabank_transactions
    WITH OWNER nexabank
    ENCODING 'UTF8'
    LC_COLLATE 'en_US.utf8'
    LC_CTYPE 'en_US.utf8';

-- notification-service database
CREATE DATABASE nexabank_notifications
    WITH OWNER nexabank
    ENCODING 'UTF8'
    LC_COLLATE 'en_US.utf8'
    LC_CTYPE 'en_US.utf8';

-- loan-service database
CREATE DATABASE nexabank_loans
    WITH OWNER nexabank
    ENCODING 'UTF8'
    LC_COLLATE 'en_US.utf8'
    LC_CTYPE 'en_US.utf8';

-- =============================================================================
-- Grant connection privileges (nexabank user owns all databases so this is
-- redundant but explicit — matches Oracle GRANT CONNECT pattern)
-- =============================================================================
GRANT ALL PRIVILEGES ON DATABASE nexabank_accounts     TO nexabank;
GRANT ALL PRIVILEGES ON DATABASE nexabank_transactions TO nexabank;
GRANT ALL PRIVILEGES ON DATABASE nexabank_notifications TO nexabank;
GRANT ALL PRIVILEGES ON DATABASE nexabank_loans        TO nexabank;

-- =============================================================================
-- Notes:
-- Flyway migrations in each service create their own tables on first startup:
--   nexabank_accounts     → V1__create_account_tables.sql
--   nexabank_transactions → V1__create_transaction_tables.sql
--   nexabank_notifications → V1__create_notification_tables.sql
--   nexabank_loans        → V1__create_loan_tables.sql
--
-- spring.jpa.hibernate.ddl-auto=validate in each service config — Flyway owns
-- schema creation, Hibernate only validates.
-- =============================================================================
