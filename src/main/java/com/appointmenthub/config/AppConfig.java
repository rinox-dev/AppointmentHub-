package com.appointmenthub.config;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * Runtime configuration of AppointmentHub.
 *
 * <p>Values can be supplied through environment variables or command line arguments.
 * Command line arguments win over environment variables, which win over the defaults.
 * See {@code docs/configuration.md} for the documented list of settings.</p>
 *
 * <p>The time zone is explicit (BR-13): all "current date" and "current time"
 * decisions are taken in {@link #zoneId()}.</p>
 */
public final class AppConfig {

    public static final int DEFAULT_PORT = 8080;
    public static final String DEFAULT_TIME_ZONE = "UTC";
    public static final LocalTime DEFAULT_OPEN = LocalTime.of(9, 0);
    public static final LocalTime DEFAULT_CLOSE = LocalTime.of(17, 0);
    public static final int DEFAULT_SLOT_MINUTES = 30;

    /** Value of {@code APPOINTMENTHUB_STORE} that disables persistence. */
    public static final String MEMORY_STORE = "memory";

    private final int port;
    private final ZoneId zoneId;
    private final LocalTime businessOpen;
    private final LocalTime businessClose;
    private final int slotMinutes;
    private final Path storeFile;
    private final Clock clock;

    private AppConfig(Builder builder) {
        this.port = builder.port;
        this.zoneId = builder.zoneId;
        this.businessOpen = builder.businessOpen;
        this.businessClose = builder.businessClose;
        this.slotMinutes = builder.slotMinutes;
        this.storeFile = builder.storeFile;
        this.clock = builder.clock;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static AppConfig defaults() {
        return builder().build();
    }

    /**
     * Builds a configuration from environment variables and command line arguments.
     * Supported keys (env: {@code APPOINTMENTHUB_<KEY>}, args: {@code --<key>=<value>}):
     * port, timezone, open, close, slot-minutes, store.
     */
    public static AppConfig fromArgs(String[] args) {
        Map<String, String> values = new HashMap<>();
        for (String key : new String[] {"port", "timezone", "open", "close", "slot-minutes", "store"}) {
            String envName = "APPOINTMENTHUB_" + key.toUpperCase().replace('-', '_');
            String envValue = System.getenv(envName);
            if (envValue != null && !envValue.isBlank()) {
                values.put(key, envValue.trim());
            }
        }
        if (args != null) {
            for (String arg : args) {
                if (arg != null && arg.startsWith("--") && arg.contains("=")) {
                    int separator = arg.indexOf('=');
                    values.put(arg.substring(2, separator).trim(), arg.substring(separator + 1).trim());
                }
            }
        }

        Builder builder = builder();
        if (values.containsKey("port")) {
            builder.port(Integer.parseInt(values.get("port")));
        }
        if (values.containsKey("timezone")) {
            builder.zoneId(ZoneId.of(values.get("timezone")));
        }
        if (values.containsKey("open")) {
            builder.businessOpen(LocalTime.parse(values.get("open")));
        }
        if (values.containsKey("close")) {
            builder.businessClose(LocalTime.parse(values.get("close")));
        }
        if (values.containsKey("slot-minutes")) {
            builder.slotMinutes(Integer.parseInt(values.get("slot-minutes")));
        }
        if (values.containsKey("store")) {
            String store = values.get("store");
            builder.storeFile(MEMORY_STORE.equalsIgnoreCase(store) ? null : Path.of(store));
        }
        return builder.build();
    }

    public int port() {
        return port;
    }

    public ZoneId zoneId() {
        return zoneId;
    }

    public LocalTime businessOpen() {
        return businessOpen;
    }

    public LocalTime businessClose() {
        return businessClose;
    }

    public int slotMinutes() {
        return slotMinutes;
    }

    /** File used to persist appointments, or {@code null} when storage is in memory. */
    public Path storeFile() {
        return storeFile;
    }

    public Clock clock() {
        return clock;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        private int port = DEFAULT_PORT;
        private ZoneId zoneId = ZoneId.of(DEFAULT_TIME_ZONE);
        private LocalTime businessOpen = DEFAULT_OPEN;
        private LocalTime businessClose = DEFAULT_CLOSE;
        private int slotMinutes = DEFAULT_SLOT_MINUTES;
        private Path storeFile = Path.of("data", "appointments.json");
        private Clock clock;

        private Builder() {
        }

        private Builder(AppConfig base) {
            this.port = base.port;
            this.zoneId = base.zoneId;
            this.businessOpen = base.businessOpen;
            this.businessClose = base.businessClose;
            this.slotMinutes = base.slotMinutes;
            this.storeFile = base.storeFile;
            this.clock = base.clock;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder zoneId(ZoneId zoneId) {
            this.zoneId = zoneId;
            return this;
        }

        public Builder businessOpen(LocalTime open) {
            this.businessOpen = open;
            return this;
        }

        public Builder businessClose(LocalTime close) {
            this.businessClose = close;
            return this;
        }

        public Builder slotMinutes(int slotMinutes) {
            this.slotMinutes = slotMinutes;
            return this;
        }

        public Builder storeFile(Path storeFile) {
            this.storeFile = storeFile;
            return this;
        }

        /** Disables persistence; useful for tests and for repeatable demo runs. */
        public Builder storeInMemory() {
            this.storeFile = null;
            return this;
        }

        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public AppConfig build() {
            if (slotMinutes <= 0) {
                throw new IllegalArgumentException("slot-minutes must be a positive integer");
            }
            if (!businessClose.isAfter(businessOpen)) {
                throw new IllegalArgumentException("close time must be after open time");
            }
            if (clock == null) {
                clock = Clock.system(zoneId);
            }
            return new AppConfig(this);
        }
    }
}
