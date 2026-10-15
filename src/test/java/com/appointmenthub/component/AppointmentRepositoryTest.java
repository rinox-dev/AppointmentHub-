package com.appointmenthub.component;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appointmenthub.model.Appointment;
import com.appointmenthub.model.AppointmentStatus;
import com.appointmenthub.store.AppointmentRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Component tests for the appointment storage (PDR section 5.3).
 *
 * <p>Verifies that the identifier sequence is never reused after a restart and
 * that appointments survive a restart when a store file is configured.</p>
 */
@Tag("component")
class AppointmentRepositoryTest {

    private static final LocalDate DATE = LocalDate.of(2027, 3, 1);

    private Appointment appointment(int id, AppointmentStatus status) {
        return new Appointment(id, "Ana Example", "ana@example.com", 1, DATE,
                LocalTime.of(10, 0), LocalTime.of(10, 30), status, Instant.parse("2027-02-10T08:00:00Z"));
    }

    @Test
    @DisplayName("NFR-03: appointments are persisted and reloaded from the store file")
    void appointmentsSurviveARestart(@TempDir Path tempDir) {
        Path storeFile = tempDir.resolve("appointments.json");

        AppointmentRepository first = new AppointmentRepository(storeFile);
        first.save(appointment(first.nextId(), AppointmentStatus.CONFIRMED));
        first.save(appointment(first.nextId(), AppointmentStatus.CANCELLED));

        AppointmentRepository reloaded = new AppointmentRepository(storeFile);

        assertAll(
                () -> assertEquals(2, reloaded.count()),
                () -> assertEquals(2, reloaded.findAll().size()),
                () -> assertEquals(AppointmentStatus.CANCELLED, reloaded.findById(1002).orElseThrow().status()),
                () -> assertTrue(Files.exists(storeFile)));
    }

    @Test
    @DisplayName("BR-11: identifiers continue after a restart and are never reused")
    void identifiersAreNotReusedAfterARestart(@TempDir Path tempDir) {
        Path storeFile = tempDir.resolve("appointments.json");

        AppointmentRepository first = new AppointmentRepository(storeFile);
        first.save(appointment(first.nextId(), AppointmentStatus.CONFIRMED));

        AppointmentRepository reloaded = new AppointmentRepository(storeFile);
        int nextId = reloaded.nextId();

        assertAll(
                () -> assertEquals(1002, nextId),
                () -> assertTrue(reloaded.findById(1001).isPresent()));
    }

    @Test
    @DisplayName("In-memory storage keeps no file and starts empty")
    void inMemoryStorageIsEmptyAndDoesNotCreateFiles(@TempDir Path tempDir) {
        AppointmentRepository repository = new AppointmentRepository(null);

        assertAll(
                () -> assertEquals(0, repository.count()),
                () -> assertFalse(repository.isPersistent()),
                () -> assertTrue(repository.findAll().isEmpty()),
                () -> assertEquals(1001, repository.nextId()));
    }

    @Test
    @DisplayName("The most recently created appointment is returned first")
    void appointmentsAreOrderedByMostRecentFirst(@TempDir Path tempDir) {
        AppointmentRepository repository = new AppointmentRepository(null);
        repository.save(appointment(repository.nextId(), AppointmentStatus.CONFIRMED));
        repository.save(appointment(repository.nextId(), AppointmentStatus.CONFIRMED));

        assertEquals(1002, repository.findAll().get(0).id());
    }
}
