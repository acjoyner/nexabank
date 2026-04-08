package com.nexabank.transaction.service;

import com.nexabank.transaction.model.Transaction;
import com.nexabank.transaction.model.TransactionStatus;
import com.nexabank.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * NDM (Connect:Direct) File Transfer Simulation.
 *
 * NDM (Network Data Mover / Sterling Connect:Direct) is a file transfer
 * protocol widely used in banking for batch file exchange between institutions.
 * Common use: EOD (end-of-day) transaction settlement files, ACH batches.
 *
 * This simulation:
 * 1. Generates a pipe-delimited flat file every night at 1 AM (cron)
 *    Format: HEADER | DETAIL records | TRAILER with control total
 * 2. Processes any inbound files at application startup (inbound processor)
 *
 * Real NDM integration would use Sterling Connect:Direct Java API or SFTP.
 *
 * See docs/learning/05-ndm-batch-file-transfer.md
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NdmFileService {

    private final TransactionRepository transactionRepository;

    @Value("${ndm.outbound-directory:./target/ndm-outbound}")
    private String outboundDirectory;

    @Value("${ndm.inbound-directory:./target/ndm-inbound}")
    private String inboundDirectory;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Nightly batch file generation — runs at 1 AM daily.
     * Writes completed transactions of the day to an outbound NDM file.
     */
    @Scheduled(cron = "${ndm.batch-cron:0 0 1 * * ?}")
    public void generateDailyBatchFile() {
        log.info("NDM: starting nightly batch file generation");
        try {
            Files.createDirectories(Paths.get(outboundDirectory));

            List<Transaction> completedToday = transactionRepository.findAll().stream()
                    .filter(t -> t.getStatus() == TransactionStatus.COMPLETED)
                    .toList();

            String filename = String.format("NEXABANK_TXN_%s_%s.dat",
                    LocalDate.now().format(DATE_FMT),
                    LocalDateTime.now().format(DATETIME_FMT));
            Path outFile = Paths.get(outboundDirectory, filename);

            try (BufferedWriter writer = Files.newBufferedWriter(outFile)) {
                // HEADER record
                writer.write(String.format("HDR|NEXABANK|%s|%04d|%s%n",
                        LocalDate.now().format(DATE_FMT),
                        completedToday.size(),
                        "NDM-BATCH-V1.0"));

                // DETAIL records — pipe-delimited
                for (Transaction t : completedToday) {
                    writer.write(String.format("DTL|%s|%s|%s|%s|%.4f|%s%n",
                            t.getReferenceNumber(),
                            t.getTransactionType().name(),
                            t.getSourceAccountId(),
                            t.getDestAccountId() != null ? t.getDestAccountId() : "N/A",
                            t.getAmount(),
                            t.getStatus().name()));
                }

                // TRAILER record — includes control total for reconciliation
                double controlTotal = completedToday.stream()
                        .mapToDouble(t -> t.getAmount().doubleValue()).sum();
                writer.write(String.format("TRL|%04d|%.4f|%s%n",
                        completedToday.size(), controlTotal,
                        LocalDateTime.now().format(DATETIME_FMT)));
            }

            log.info("NDM: batch file written — {} records, file: {}", completedToday.size(), filename);
        } catch (IOException e) {
            log.error("NDM: failed to generate batch file", e);
        }
    }

    /**
     * Inbound file processor — runs at application startup.
     * Scans inbound directory and processes any waiting NDM files.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void processInboundFiles() {
        try {
            Path inboundPath = Paths.get(inboundDirectory);
            Files.createDirectories(inboundPath);

            File[] files = inboundPath.toFile().listFiles(
                    f -> f.isFile() && f.getName().endsWith(".dat"));
            if (files == null || files.length == 0) {
                log.info("NDM: no inbound files to process");
                return;
            }

            for (File file : files) {
                log.info("NDM: processing inbound file: {}", file.getName());
                processInboundFile(file);
                // Move to processed subfolder after successful processing
                Path processed = inboundPath.resolve("processed");
                Files.createDirectories(processed);
                Files.move(file.toPath(), processed.resolve(file.getName()),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("NDM: error processing inbound files", e);
        }
    }

    private void processInboundFile(File file) throws IOException {
        int detailCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("DTL|")) {
                    detailCount++;
                    String[] parts = line.split("\\|");
                    log.info("NDM inbound record: ref={} type={} amount={}",
                            parts[1], parts[2], parts[5]);
                }
            }
        }
        log.info("NDM: processed {} detail records from {}", detailCount, file.getName());
    }
}
