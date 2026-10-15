package com.appointmenthub.api;

import com.appointmenthub.config.AppConfig;
import com.appointmenthub.service.BookingService;
import com.appointmenthub.store.AppointmentRepository;
import com.appointmenthub.store.ServiceCatalog;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP server hosting both the web interface and the REST API
 * (PDR section 5.4).
 *
 * <p>Uses the HTTP server that ships with the JDK, so no application server has
 * to be installed or configured. Port {@code 0} lets the operating system pick a
 * free port, which is how the automated tests avoid clashing with a running
 * instance.</p>
 */
public final class AppointmentApiServer implements AutoCloseable {

    private static final int WORKER_THREADS = 8;

    private final AppConfig config;
    private final BookingService bookingService;
    private final HttpServer server;
    private final ExecutorService executor;
    private final int port;

    public AppointmentApiServer(AppConfig config) throws IOException {
        this.config = config;
        ServiceCatalog catalog = new ServiceCatalog();
        AppointmentRepository repository = new AppointmentRepository(config.storeFile());
        this.bookingService = new BookingService(config, catalog, repository);
        ApiRouter router = new ApiRouter(bookingService);
        StaticFileHandler staticFiles = new StaticFileHandler();

        this.server = HttpServer.create(new InetSocketAddress(config.port()), 0);
        this.port = server.getAddress().getPort();
        this.executor = Executors.newFixedThreadPool(WORKER_THREADS, runnable -> {
            Thread thread = new Thread(runnable, "appointmenthub-http");
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(executor);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path != null && path.startsWith("/api/")) {
                router.handle(exchange);
            } else {
                staticFiles.handle(exchange);
            }
        });
    }

    public void start() {
        server.start();
    }

    public int port() {
        return port;
    }

    public String baseUrl() {
        return "http://localhost:" + port;
    }

    public AppConfig config() {
        return config;
    }

    public BookingService bookingService() {
        return bookingService;
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }
}
