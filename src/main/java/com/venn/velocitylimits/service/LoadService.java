package com.venn.velocitylimits.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.venn.velocitylimits.model.*;
import com.venn.velocitylimits.repository.LoadTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

@Service
public class LoadService {

    private static final Logger log = LoggerFactory.getLogger(LoadService.class);
    private static final BigDecimal DAILY_AMOUNT_LIMIT = new BigDecimal("5000");
    private static final BigDecimal WEEKLY_AMOUNT_LIMIT = new BigDecimal("20000");
    private static final long DAILY_COUNT_LIMIT = 3;

    private final LoadTransactionRepository repository;
    private final ObjectMapper objectMapper;
    private final Path outputPath;

    public LoadService(LoadTransactionRepository repository,
                       ObjectMapper objectMapper,
                       @Value("${velocity-limits.output-dir:data}") String outputDir) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.outputPath = Paths.get(outputDir, "output.txt");
    }

    @Transactional
    public Optional<LoadResponse> processLoad(LoadRequest request) {
        log.info("Load received: id={}, customer_id={}, amount={}, time={}",
                request.getId(), request.getCustomerId(), request.getLoadAmount(), request.getTime());

        LoadTransactionId txId = new LoadTransactionId(request.getId(), request.getCustomerId());

        // 1. Duplicate check
        if (repository.existsById(txId)) {
            log.info("Load declined: id={}, customer_id={}, reason=duplicate",
                    request.getId(), request.getCustomerId());
            return Optional.empty();
        }

        // 2. Parse amount
        BigDecimal amount = parseAmount(request.getLoadAmount());

        // 3. Calculate day boundaries (UTC)
        Instant time = request.getTime();
        LocalDate day = time.atZone(ZoneOffset.UTC).toLocalDate();
        Instant dayStart = day.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        // 4. Calculate week boundaries (Monday 00:00 to next Monday 00:00 UTC)
        LocalDate monday = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Instant weekStart = monday.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant weekEnd = monday.plusDays(7).atStartOfDay(ZoneOffset.UTC).toInstant();

        boolean accepted = true;
        String declineReason = null;

        // 5. Daily count check (>= 3 means decline)
        long dailyCount = repository.countAcceptedByCustomerIdAndTimeBetween(
                request.getCustomerId(), dayStart, dayEnd);
        if (dailyCount >= DAILY_COUNT_LIMIT) {
            accepted = false;
            declineReason = String.format("daily_count_limit, count=%d, limit=%d", dailyCount, DAILY_COUNT_LIMIT);
        }

        // 6. Daily amount check
        if (accepted) {
            BigDecimal dailySum = repository.sumAcceptedAmountByCustomerIdAndTimeBetween(
                    request.getCustomerId(), dayStart, dayEnd);
            if (dailySum.add(amount).compareTo(DAILY_AMOUNT_LIMIT) > 0) {
                accepted = false;
                declineReason = String.format("daily_amount_limit, current_sum=%s, load=%s, limit=%s",
                        dailySum, amount, DAILY_AMOUNT_LIMIT);
            }
        }

        // 7. Weekly amount check
        if (accepted) {
            BigDecimal weeklySum = repository.sumAcceptedAmountByCustomerIdAndTimeBetween(
                    request.getCustomerId(), weekStart, weekEnd);
            if (weeklySum.add(amount).compareTo(WEEKLY_AMOUNT_LIMIT) > 0) {
                accepted = false;
                declineReason = String.format("weekly_amount_limit, current_sum=%s, load=%s, limit=%s",
                        weeklySum, amount, WEEKLY_AMOUNT_LIMIT);
            }
        }

        // 9. Persist
        LoadTransaction tx = new LoadTransaction(
                request.getId(), request.getCustomerId(), amount, time, accepted);
        repository.save(tx);

        if (accepted) {
            log.info("Load accepted: id={}, customer_id={}, amount={}",
                    request.getId(), request.getCustomerId(), amount);
        } else {
            log.info("Load declined: id={}, customer_id={}, reason={}",
                    request.getId(), request.getCustomerId(), declineReason);
        }

        // 10. Write response to file and return
        LoadResponse response = new LoadResponse(request.getId(), request.getCustomerId(), accepted);
        writeResponseToFile(response);
        return Optional.of(response);
    }

    private void writeResponseToFile(LoadResponse response) {
        try {
            Files.createDirectories(outputPath.getParent());
            String line = objectMapper.writeValueAsString(response) + System.lineSeparator();
            Files.writeString(outputPath, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("Failed to write response to {}", outputPath, e);
        }
    }

    private BigDecimal parseAmount(String amountStr) {
        String cleaned = amountStr.replace("$", "").replace(",", "");
        return new BigDecimal(cleaned);
    }
}
