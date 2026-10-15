package com.appointmenthub.support;

import com.google.gson.JsonArray;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base class for API (integration) tests.
 *
 * <p>Starts a clean AppointmentHub instance for the whole test class and stops it
 * afterwards. Each test works on its own data and does not depend on execution
 * order (PDR section 14.5).</p>
 */
public abstract class ApiTestBase {

    protected static TestApp app;
    protected static ApiClient api;

    @BeforeAll
    static void startApplication() {
        app = TestApp.start();
        api = new ApiClient(app.baseUrl());
    }

    @AfterAll
    static void stopApplication() {
        if (app != null) {
            app.close();
            app = null;
        }
    }

    /** First available start time for a service and date, or {@code null} when none is free. */
    protected static String firstAvailableSlot(int serviceId, LocalDate date) {
        ApiClient.Response response = api.get("/api/availability?serviceId=" + serviceId + "&date=" + date);
        if (!response.isSuccess()) {
            return null;
        }
        JsonArray slots = response.json().getAsJsonArray("slots");
        if (slots.isEmpty()) {
            return null;
        }
        return slots.get(0).getAsJsonObject().get("startTime").getAsString();
    }

    /** Number of available slots returned for a service and date. */
    protected static int availableSlotCount(int serviceId, LocalDate date) {
        ApiClient.Response response = api.get("/api/availability?serviceId=" + serviceId + "&date=" + date);
        return response.json().getAsJsonArray("slots").size();
    }
}
