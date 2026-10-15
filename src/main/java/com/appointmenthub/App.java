package com.appointmenthub;

import com.appointmenthub.api.AppointmentApiServer;
import com.appointmenthub.config.AppConfig;
import com.appointmenthub.json.Json;

/**
 * Entry point of AppointmentHub.
 *
 * <p>Starts the web interface and the REST API on the configured port:</p>
 * <pre>
 *   java -jar target/appointmenthub.jar
 *   java -jar target/appointmenthub.jar --port=9090 --store=memory
 * </pre>
 */
public final class App {

    private App() {
    }

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.fromArgs(args);
        AppointmentApiServer server = new AppointmentApiServer(config);
        server.start();

        System.out.println("AppointmentHub is running");
        System.out.println("  Web interface  : " + server.baseUrl() + "/");
        System.out.println("  REST API       : " + server.baseUrl() + "/api/services");
        System.out.println("  Time zone      : " + config.zoneId() + " (current date "
                + java.time.LocalDate.now(config.clock()) + ")");
        System.out.println("  Business hours : " + config.businessOpen() + "-" + config.businessClose()
                + " every " + config.slotMinutes() + " minutes");
        System.out.println("  Storage        : " + (config.storeFile() == null
                ? "in memory (not persisted)"
                : config.storeFile().toAbsolutePath()));
        System.out.println("  JSON formats   : date=" + Json.DATE_PATTERN + ", time=" + Json.TIME_PATTERN);
        System.out.println("Press Ctrl+C to stop.");

        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "appointmenthub-shutdown"));
        Thread.currentThread().join();
    }
}
