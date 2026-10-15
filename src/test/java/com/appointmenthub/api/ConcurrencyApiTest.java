package com.appointmenthub.api;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.appointmenthub.support.ApiClient;
import com.appointmenthub.support.ApiTestBase;
import com.appointmenthub.support.TestData;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Concurrency test for the availability rule (NFR-03, BR-08).
 *
 * <p>Simulates several customers confirming the same time slot at the same
 * moment. Exactly one request may succeed; the others must be rejected with the
 * documented conflict response.</p>
 */
@Tag("api")
class ConcurrencyApiTest extends ApiTestBase {

    @Test
    @DisplayName("NFR-03: parallel requests for the same slot produce exactly one appointment")
    void parallelRequestsForTheSameSlotProduceOneAppointment() throws Exception {
        LocalDate date = TestData.freeDate();
        String startTime = firstAvailableSlot(TestData.ACTIVE_SERVICE_ID, date);
        String body = TestData.validBookingBody(date, startTime);
        int attempts = 10;

        ExecutorService pool = Executors.newFixedThreadPool(5);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<ApiClient.Response>> responses = new ArrayList<>();
        for (int index = 0; index < attempts; index++) {
            responses.add(pool.submit(() -> {
                startGate.await();
                return api.post("/api/appointments", body);
            }));
        }
        startGate.countDown();

        int created = 0;
        int conflicts = 0;
        for (Future<ApiClient.Response> future : responses) {
            ApiClient.Response response = future.get(20, TimeUnit.SECONDS);
            int status = response.status();
            if (status == 201) {
                created++;
            } else {
                assertEquals(409, status, "every rejected request uses the documented conflict status");
                assertEquals("SLOT_NOT_AVAILABLE", response.errorCode());
                conflicts++;
            }
        }
        pool.shutdownNow();

        int stored = api.get("/api/appointments?customerEmail=" + TestData.VALID_EMAIL).jsonArray().size();
        int createdCount = created;
        int conflictCount = conflicts;
        int storedCount = stored;

        assertAll(
                () -> assertEquals(1, createdCount, "exactly one request creates the appointment"),
                () -> assertEquals(attempts - 1, conflictCount),
                () -> assertEquals(1, storedCount, "only one appointment occupies the slot"));
    }
}
