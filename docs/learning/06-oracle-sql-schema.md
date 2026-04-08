# Oracle-Compatible SQL Schema in NexaBank

> **Paste into Ollama/Open WebUI** for AI-assisted learning on this topic.

## Why Oracle-Compatible DDL?
Most large banks run Oracle Database on-premises. This project uses PostgreSQL for local development (no license required) but the DDL is written in Oracle conventions so it will work with minimal changes on Oracle.

## Oracle Conventions Used

### 1. SEQUENCE Objects (not SERIAL/IDENTITY)
```sql
-- Oracle style (used in NexaBank):
CREATE SEQUENCE SEQ_ACCOUNT_ID START WITH 1000 INCREMENT BY 1 NO CACHE NO CYCLE;
CREATE TABLE ACCOUNTS (
    ID BIGINT DEFAULT NEXTVAL('SEQ_ACCOUNT_ID') PRIMARY KEY
);

-- PostgreSQL shortcut (NOT used — Oracle doesn't support this):
CREATE TABLE ACCOUNTS (ID BIGSERIAL PRIMARY KEY);
```

In JPA, this maps to:
```java
@SequenceGenerator(name = "account_seq", sequenceName = "SEQ_ACCOUNT_ID", allocationSize = 1)
```

### 2. NUMERIC(19,4) for Money
```sql
BALANCE NUMERIC(19,4)  -- Up to $999 trillion with 4 decimal places
```

**NEVER use FLOAT or DOUBLE for money** — floating-point precision loss causes rounding errors. `NUMERIC(19,4)` is exact. In Java, always use `BigDecimal` for monetary values.

### 3. UPPERCASE Table and Column Names
Oracle is case-sensitive with quoted identifiers. Using UPPERCASE without quotes ensures compatibility:
```sql
CREATE TABLE ACCOUNTS (       -- Oracle-safe
    CUSTOMER_ID BIGINT,       -- Oracle-safe
    accountType VARCHAR(20)   -- Would need quoting in Oracle
);
```

### 4. Named Constraints and Indexes
```sql
CONSTRAINT UQ_ACCOUNTS_NUMBER  UNIQUE (ACCOUNT_NUMBER),
CONSTRAINT FK_ACCOUNTS_CUSTOMER FOREIGN KEY (CUSTOMER_ID) REFERENCES CUSTOMERS(ID),
CONSTRAINT CK_ACCOUNTS_TYPE    CHECK (ACCOUNT_TYPE IN ('CHECKING', 'SAVINGS')),

CREATE INDEX IDX_ACCOUNTS_CUSTOMER ON ACCOUNTS(CUSTOMER_ID);
```

Named constraints allow meaningful error messages: "Constraint UQ_ACCOUNTS_NUMBER violated" is clearer than "unique_1234 violated".

## Flyway Migrations
**Files:** Each service has `src/main/resources/db/migration/V1__create_*_tables.sql`

Flyway runs migrations automatically on startup:
1. Checks `flyway_schema_history` table
2. Finds unapplied migrations (V1__, V2__, etc.)
3. Applies in order

**Key config in `account-service.yml`:**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Hibernate only VALIDATES against existing schema
  flyway:
    enabled: true         # Flyway OWNS the DDL
```

**Never use `ddl-auto: create` or `ddl-auto: create-drop` in production** — Hibernate would drop and recreate tables on restart.

## Schema Overview

| Service | Schema | Key Tables |
|---|---|---|
| account-service | nexabank_accounts | CUSTOMERS, ACCOUNTS |
| transaction-service | nexabank_transactions | TRANSACTIONS, LEDGER_ENTRIES |
| notification-service | nexabank_notifications | NOTIFICATIONS |
| loan-service | nexabank_loans | LOAN_APPLICATIONS |

## Double-Entry Bookkeeping (LEDGER_ENTRIES)
For every transfer of $500 from account 1001 to 1002:

```
TRANSACTIONS: 1 row  — TRANSFER $500, status=COMPLETED
LEDGER_ENTRIES: 2 rows —
  DEBIT  accountId=1001, amount=500, balanceAfter=1500
  CREDIT accountId=1002, amount=500, balanceAfter=2500
```

This is how real banking ledgers work — every money movement has a debit and credit entry.

## Interview Talking Points
- **Why not use SERIAL in PostgreSQL?** Not Oracle-compatible. SEQUENCE objects work identically in both databases.
- **Why BigDecimal in Java?** `double 0.1 + 0.2 = 0.30000000000000004` — precision loss. `BigDecimal` is exact.
- **What is Flyway?** Database schema version control — like Git for your database. Every schema change is a versioned migration file.
- **What is the N+1 select problem?** Calling `account.getCustomer()` in a loop triggers a DB query per account. Solved with `JOIN FETCH` in `AccountRepository.findByIdWithCustomer()`.

## Questions to Ask Your AI
- "What is the N+1 select problem and how does JOIN FETCH solve it?"
- "What is the difference between Flyway and Liquibase?"
- "How would you write a V2 Flyway migration to add a column to ACCOUNTS?"
- "Why is NUMERIC(19,4) safer than DOUBLE for monetary values?"
- "What does ddl-auto: validate do and why is it safer than create?"
