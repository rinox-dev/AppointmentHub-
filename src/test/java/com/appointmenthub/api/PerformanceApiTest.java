package com.appointmenthub.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appointmenthub.support.ApiClient;
import com.appointmenthub.support.ApiTestBase;
import com.appointmenthub.support.TestData;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Basic performance check of the routine API operations (NFR-02).
 *
 * <p>Documented test conditions: the application runs on the same machine as the
 * test client with in-memory storage, the sample size is 30 requests per
 * endpoint, and a single-threaded client sends the requests sequentially. The
 * threshold is the acceptance target of the PDR: at least 95% of the requests
 * must answer within 2 seconds. These results characterise this environment only
 * and are not a load test (risk R-08).</p>
 */
@Tag("api")
class PerformanceApiTest extends ApiTestBase {

    private static final int SAMPLE_SIZE = 30;
    private static final long THRESHOLD_MILLIS = 2000;

    @Test
    @DisplayName("NFR-02: 95% of the services queries answer within 2 seconds")
    void servicesQueriesAreFast() {
        List<Long> samples = new ArrayList<>();
        for (int index = 0; index < SAMPLE_SIZE; index++) {
            samples.add(api.get("/api/services").elapsedMillis());
        }
        report("GET /api/services", samples);
        assertTrue(percentile(samples, 95) <= THRESHOLD_MILLIS, "p95 must stay within the threshold");
    }

    @Test
    @DisplayName("NFR-02: 95% of the availability queries answer within 2 seconds")
    void availabilityQueriesAreFast() {
        LocalDate date = TestData.freeDate();
        String path = "/api/availability?serviceId=1&date=" + date;
        List<Long> samples = new ArrayList<>();
        for (int index = 0; index < SAMPLE_SIZE; index++) {
            ApiClient.Response response = api.get(path);
            assertEquals(200, response.status());
            samples.add(response.elapsedMillis());
        }
        report("GET /api/availability", samples);
        assertTrue(percentile(samples, 95) <= THRESHOLD_MILLIS, "p95 must stay within the threshold");
    }

    @Test
    @DisplayName("NFR-02: 95% of the booking requests answer within 2 seconds")
    void bookingRequestsAreFast() {
        List<Long> samples = new ArrayList<>();
        for (int index = 0; index < SAMPLE_SIZE; index++) {
            LocalDate date = TestData.freeDate();
            String startTime = firstAvailableSlot(TestData.ACTIVE_SERVICE_ID, date);
            ApiClient.Response response = api.post("/api/appointments", TestData.validBookingBody(date, startTime));
            assertEquals(201, response.status());
            samples.add(response.elapsedMillis());
        }
        report("POST /api/appointments", samples);
        assertTrue(percentile(samples, 95) <= THRESHOLD_MILLIS, "p95 must stay within the threshold");
    }

    /** Simple nearest-rank percentile, enough for a sample of this size. */
    private long percentile(List<Long> samples, int percentile) {
        List<Long> sorted = new ArrayList<>(samples);
        Collections.sort(sorted);
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    private void report(String operation, List<Long> samples) {
        long average = Math.round(samples.stream().mapToLong(Long::longValue).average().orElse(0));
        System.out.printf("NFR-02 evidence | %-24s samples=%d avg=%dms p95=%dms max=%dms%n",
                operation, samples.size(), average, percentile(samples, 95), Collections.max(samples));
    }
}
