package com.appointmenthub.store;

import com.appointmenthub.json.Json;
import com.appointmenthub.model.Appointment;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Appointment storage (PDR section 5.3).
 *
 * <p>Appointments are held in memory and, when a store file is configured,
 * written to a JSON file after every change so the data survives a restart.
 * A store file of {@code null} keeps everything in memory, which is what the
 * automated tests use to guarantee a clean, repeatable state.</p>
 *
 * <p>Every method is synchronised: the identifier sequence and the file
 * snapshot must stay consistent when requests are handled concurrently.</p>
 */
public final class AppointmentRepository {

    /** First identifier handed out; keeps example identifiers from the PDR (1001). */
    public static final int FIRST_ID = 1000;

    private final Path storeFile;
    private final Gson gson = Json.gson();
    private final Map<Integer, Appointment> appointments = new LinkedHashMap<>();
    private int lastId = FIRST_ID;

    public AppointmentRepository(Path storeFile) {
        this.storeFile = storeFile;
        load();
    }

    /** All appointments, most recently created first. */
    public synchronized List<Appointment> findAll() {
        List<Appointment> result = new ArrayList<>(appointments.values());
        result.sort(Comparator.comparingInt(Appointment::id).reversed());
        return result;
    }

    public synchronized Optional<Appointment> findById(int id) {
        return Optional.ofNullable(appointments.get(id));
    }

    /** Returns the next unused identifier (FR-04, BR-11). */
    public synchronized int nextId() {
        return ++lastId;
    }

    public synchronized Appointment save(Appointment appointment) {
        appointments.put(appointment.id(), appointment);
        persist();
        return appointment;
    }

    public synchronized int count() {
        return appointments.size();
    }

    /** Removes every appointment; used by tests and by the demo reset option. */
    public synchronized void deleteAll() {
        appointments.clear();
        persist();
    }

    public boolean isPersistent() {
        return storeFile != null;
    }

    private void load() {
        if (storeFile == null || !Files.isRegularFile(storeFile)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(storeFile, StandardCharsets.UTF_8)) {
            Appointment[] stored = gson.fromJson(reader, Appointment[].class);
            if (stored != null) {
                for (Appointment appointment : stored) {
                    appointments.put(appointment.id(), appointment);
                    lastId = Math.max(lastId, appointment.id());
                }
            }
        } catch (IOException | JsonParseException e) {
            throw new IllegalStateException("Unable to read the appointment store " + storeFile, e);
        }
    }

    private void persist() {
        if (storeFile == null) {
            return;
        }
        try {
            Path parent = storeFile.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tempFile = Files.createTempFile(parent, "appointments", ".json.tmp");
            try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                gson.toJson(appointments.values(), writer);
            }
            Files.move(tempFile, storeFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write the appointment store " + storeFile, e);
        }
    }
}
