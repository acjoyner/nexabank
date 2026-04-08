# NDM (Connect:Direct) Batch File Transfer in NexaBank

> **Paste into Ollama/Open WebUI** for AI-assisted learning on this topic.

## What Is NDM?
NDM stands for **Network Data Mover** — a product line from Sterling (now IBM) for managed file transfer between systems. The main product is **Sterling Connect:Direct** (formerly NDM).

**In banking it's used for:**
- End-of-day transaction settlement files to clearing houses (ACH, SWIFT)
- Interbank batch transfers
- Regulatory reporting file submissions
- Mainframe-to-distributed-system file exchange

## Real NDM vs. This Simulation
A real NDM implementation requires:
- Licensed IBM Sterling Connect:Direct software
- Network connectivity to partner institutions
- Sterling Java API (proprietary)

This project **simulates NDM behavior** using Spring's scheduling and Java file I/O — demonstrating your understanding of the concept without requiring licensed software.

## Key Code

### NDM Simulation
**File:** `services/transaction-service/src/main/java/com/nexabank/transaction/service/NdmFileService.java`

**Outbound (nightly batch generation):**
```java
@Scheduled(cron = "0 0 1 * * ?")  // Every day at 1 AM
public void generateDailyBatchFile() {
    // Generates: NEXABANK_TXN_20241215_010000.dat
    // Format: HDR|NEXABANK|20241215|0042|NDM-BATCH-V1.0
    //         DTL|{UUID}|TRANSFER|1001|1002|500.0000|COMPLETED
    //         TRL|0042|25000.0000|20241215010012
}
```

**File Format (pipe-delimited flat file):**
```
HDR|NEXABANK|20241215|0042|NDM-BATCH-V1.0
DTL|a1b2c3d4-...|DEPOSIT|1001|N/A|1000.0000|COMPLETED
DTL|e5f6g7h8-...|TRANSFER|1001|1002|500.0000|COMPLETED
TRL|0042|1500.0000|20241215010012
```

- **HDR** — Header: sender, date, record count, version
- **DTL** — Detail: one line per transaction
- **TRL** — Trailer: record count + control total (for reconciliation)

**The control total in the trailer is critical in banking** — the receiving institution verifies that the sum of amounts in DTL records matches the TRL total. Mismatches trigger alerts.

**Inbound processing:**
```java
@EventListener(ApplicationReadyEvent.class)
public void processInboundFiles() {
    // Scan target/ndm-inbound/ at startup
    // Parse each .dat file
    // Move processed files to target/ndm-inbound/processed/
}
```

## Running the NDM Simulation

```bash
# Trigger batch file immediately (instead of waiting for 1 AM):
# Change cron in config-server:
# ndm.batch-cron: "0 * * * * ?"  # every minute (for testing)

# Check output:
ls services/transaction-service/target/ndm-outbound/

# Place test inbound file:
mkdir -p services/transaction-service/target/ndm-inbound
echo "HDR|PARTNER|20241215|0001|NDM-BATCH-V1.0
DTL|test-ref-001|DEPOSIT|2001|N/A|250.0000|COMPLETED
TRL|0001|250.0000|20241215010000" > services/transaction-service/target/ndm-inbound/TEST.dat
# Restart service — it will process the inbound file on startup
```

## Interview Talking Points
- **What is NDM?** IBM Sterling Connect:Direct for managed file transfer — used in banking for batch settlement files between institutions.
- **Why use NDM instead of REST APIs?** Legacy mainframe systems often can't expose REST APIs. NDM handles large file volumes with built-in checkpointing, restart, and encryption. It's also required by some regulatory frameworks.
- **What's a control total?** The sum of monetary values in a batch file used for reconciliation. Both sender and receiver must agree on the total before processing.
- **Inbound vs. outbound?** Outbound = sending settlement files to clearinghouses. Inbound = receiving files from other banks (e.g., incoming ACH transactions).
- **How does this relate to NDM in real banking?** Pattern is identical — generate file, trigger transfer, parse received file. Only the transfer mechanism changes (Connect:Direct API vs. scheduled file scan).

## Questions to Ask Your AI
- "What is the difference between NDM/Connect:Direct and SFTP for file transfer?"
- "Why is the control total important in banking batch files?"
- "How would you add checksum validation to the inbound file processor?"
- "What is ACH and how does NDM relate to it?"
- "How would you implement error handling if an inbound NDM file is malformed?"
