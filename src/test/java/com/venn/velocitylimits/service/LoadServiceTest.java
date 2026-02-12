package com.venn.velocitylimits.service;

import com.venn.velocitylimits.model.LoadRequest;
import com.venn.velocitylimits.model.LoadResponse;
import com.venn.velocitylimits.repository.LoadTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class LoadServiceTest {

    @Autowired
    private LoadService loadService;

    @Autowired
    private LoadTransactionRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    private LoadRequest createRequest(String id, String customerId, String amount, String time) {
        LoadRequest req = new LoadRequest();
        req.setId(id);
        req.setCustomerId(customerId);
        req.setLoadAmount(amount);
        req.setTime(Instant.parse(time));
        return req;
    }

    @Test
    void testAcceptValidLoad() {
        LoadRequest req = createRequest("1", "100", "$1000.00", "2000-01-03T10:00:00Z");
        Optional<LoadResponse> resp = loadService.processLoad(req);

        assertTrue(resp.isPresent());
        assertTrue(resp.get().isAccepted());
    }

    @Test
    void testDuplicateLoadReturnsEmpty() {
        LoadRequest req = createRequest("1", "100", "$1000.00", "2000-01-03T10:00:00Z");
        loadService.processLoad(req);

        Optional<LoadResponse> dup = loadService.processLoad(req);
        assertTrue(dup.isEmpty());
    }

    @Test
    void testDailyAmountLimitExceeded() {
        // Load $4999 first (accepted)
        LoadRequest req1 = createRequest("1", "100", "$4999.00", "2000-01-03T10:00:00Z");
        Optional<LoadResponse> resp1 = loadService.processLoad(req1);
        assertTrue(resp1.isPresent());
        assertTrue(resp1.get().isAccepted());

        // Load $1.01 more — exceeds $5000 daily limit
        LoadRequest req2 = createRequest("2", "100", "$1.01", "2000-01-03T11:00:00Z");
        Optional<LoadResponse> resp2 = loadService.processLoad(req2);
        assertTrue(resp2.isPresent());
        assertFalse(resp2.get().isAccepted());
    }

    @Test
    void testDailyAmountExactlyAtLimit() {
        LoadRequest req1 = createRequest("1", "100", "$3000.00", "2000-01-03T10:00:00Z");
        loadService.processLoad(req1);

        // $3000 + $2000 = $5000 exactly — should be accepted
        LoadRequest req2 = createRequest("2", "100", "$2000.00", "2000-01-03T11:00:00Z");
        Optional<LoadResponse> resp2 = loadService.processLoad(req2);
        assertTrue(resp2.isPresent());
        assertTrue(resp2.get().isAccepted());
    }

    @Test
    void testDailyCountLimitExceeded() {
        // 3 loads accepted
        loadService.processLoad(createRequest("1", "100", "$100.00", "2000-01-03T10:00:00Z"));
        loadService.processLoad(createRequest("2", "100", "$100.00", "2000-01-03T11:00:00Z"));
        loadService.processLoad(createRequest("3", "100", "$100.00", "2000-01-03T12:00:00Z"));

        // 4th load should be declined
        Optional<LoadResponse> resp = loadService.processLoad(
                createRequest("4", "100", "$100.00", "2000-01-03T13:00:00Z"));
        assertTrue(resp.isPresent());
        assertFalse(resp.get().isAccepted());
    }

    @Test
    void testWeeklyAmountLimitExceeded() {
        // Monday to Friday, $4000/day = $20000 total over 5 days (3 loads/day)
        // Mon
        loadService.processLoad(createRequest("1", "100", "$1500.00", "2000-01-03T08:00:00Z"));
        loadService.processLoad(createRequest("2", "100", "$1500.00", "2000-01-03T09:00:00Z"));
        loadService.processLoad(createRequest("3", "100", "$1000.00", "2000-01-03T10:00:00Z"));
        // Tue
        loadService.processLoad(createRequest("4", "100", "$1500.00", "2000-01-04T08:00:00Z"));
        loadService.processLoad(createRequest("5", "100", "$1500.00", "2000-01-04T09:00:00Z"));
        loadService.processLoad(createRequest("6", "100", "$1000.00", "2000-01-04T10:00:00Z"));
        // Wed
        loadService.processLoad(createRequest("7", "100", "$1500.00", "2000-01-05T08:00:00Z"));
        loadService.processLoad(createRequest("8", "100", "$1500.00", "2000-01-05T09:00:00Z"));
        loadService.processLoad(createRequest("9", "100", "$1000.00", "2000-01-05T10:00:00Z"));
        // Thu
        loadService.processLoad(createRequest("10", "100", "$1500.00", "2000-01-06T08:00:00Z"));
        loadService.processLoad(createRequest("11", "100", "$1500.00", "2000-01-06T09:00:00Z"));
        loadService.processLoad(createRequest("12", "100", "$1000.00", "2000-01-06T10:00:00Z"));
        // Fri
        loadService.processLoad(createRequest("13", "100", "$1500.00", "2000-01-07T08:00:00Z"));
        loadService.processLoad(createRequest("14", "100", "$1500.00", "2000-01-07T09:00:00Z"));
        loadService.processLoad(createRequest("15", "100", "$1000.00", "2000-01-07T10:00:00Z"));

        // Sat — $1 more would exceed $20000 weekly limit
        Optional<LoadResponse> resp = loadService.processLoad(
                createRequest("16", "100", "$1.00", "2000-01-08T08:00:00Z"));
        assertTrue(resp.isPresent());
        assertFalse(resp.get().isAccepted());
    }

    @Test
    void testLoadOnNewDayResetsDailyLimits() {
        // 3 loads on day 1
        loadService.processLoad(createRequest("1", "100", "$100.00", "2000-01-03T10:00:00Z"));
        loadService.processLoad(createRequest("2", "100", "$100.00", "2000-01-03T11:00:00Z"));
        loadService.processLoad(createRequest("3", "100", "$100.00", "2000-01-03T12:00:00Z"));

        // New day — should be accepted
        Optional<LoadResponse> resp = loadService.processLoad(
                createRequest("4", "100", "$100.00", "2000-01-04T10:00:00Z"));
        assertTrue(resp.isPresent());
        assertTrue(resp.get().isAccepted());
    }

    @Test
    void testLoadOnNewWeekResetsWeeklyLimits() {
        // Fill week 1 (Mon Jan 3 to Sun Jan 9, 2000)
        loadService.processLoad(createRequest("1", "100", "$5000.00", "2000-01-03T08:00:00Z"));
        loadService.processLoad(createRequest("4", "100", "$5000.00", "2000-01-04T08:00:00Z"));
        loadService.processLoad(createRequest("7", "100", "$5000.00", "2000-01-05T08:00:00Z"));
        loadService.processLoad(createRequest("10", "100", "$5000.00", "2000-01-06T08:00:00Z"));

        // Next Monday (new week) — should be accepted
        Optional<LoadResponse> resp = loadService.processLoad(
                createRequest("16", "100", "$1000.00", "2000-01-10T08:00:00Z"));
        assertTrue(resp.isPresent());
        assertTrue(resp.get().isAccepted());
    }

    @Test
    void testSingleLoadExceedsDailyLimit() {
        // Single load > $5000 should be declined
        LoadRequest req = createRequest("1", "100", "$5000.01", "2000-01-03T10:00:00Z");
        Optional<LoadResponse> resp = loadService.processLoad(req);
        assertTrue(resp.isPresent());
        assertFalse(resp.get().isAccepted());
    }

    @Test
    void testDeclinedLoadsDoNotCountTowardLimits() {
        // Decline a large load
        loadService.processLoad(createRequest("1", "100", "$6000.00", "2000-01-03T10:00:00Z"));

        // A valid load should still be accepted (the declined one shouldn't count)
        Optional<LoadResponse> resp = loadService.processLoad(
                createRequest("2", "100", "$4000.00", "2000-01-03T11:00:00Z"));
        assertTrue(resp.isPresent());
        assertTrue(resp.get().isAccepted());
    }

}
