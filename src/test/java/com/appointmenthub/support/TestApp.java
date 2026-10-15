package com.appointmenthub.support;

import com.appointmenthub.api.AppointmentApiServer;
import com.appointmenthub.config.AppConfig;
import com.appointmenthub.service.BookingService;
import java.io.IOException;

/**
 * Starts AppointmentHub in-process for automated tests.
 *
 * <p>Every test class gets its own instance on an operating-system assigned port
 * and with in-memory storage, so tests do not depend on each other, on a
 * previously running application, or on data left behind by an earlier run
 * (risk R-05, NFR-10).</p>
 */
public final class TestApp implements AutoCloseable {

    private final AppointmentApiServer server;

    private TestApp(AppointmentApiServer server) {
        this.server = server;
    }

    /** Starts the application on a free port with a clean, in-memory store. */
    public static TestApp start() {
        return start(AppConfig.builder().storeInMemory().port(0).build());
    }

    public static TestApp start(AppConfig config) {
        try {
            AppointmentApiServer server = new AppointmentApiServer(config);
            server.start();
            return new TestApp(server);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to start AppointmentHub for the test run", e);
        }
    }

    public String baseUrl() {
        return server.baseUrl();
    }

    public BookingService bookingService() {
        return server.bookingService();
    }

    @Override
    public void close() {
        server.close();
    }
}
