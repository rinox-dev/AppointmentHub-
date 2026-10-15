package com.appointmenthub.component;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appointmenthub.config.AppConfig;
import com.appointmenthub.error.ApiException;
import com.appointmenthub.error.ErrorCode;
import com.appointmenthub.model.Appointment;
import com.appointmenthub.model.AppointmentStatus;
import com.appointmenthub.model.AvailabilityResponse;
import com.appointmenthub.model.CreateAppointmentRequest;
import com.appointmenthub.model.Service;
import com.appointmenthub.model.TimeSlot;
import com.appointmenthub.service.BookingService;
import com.appointmenthub.store.AppointmentRepository;
import com.appointmenthub.store.ServiceCatalog;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Component tests for the business rules (PDR sections 9, 11.3 and 12).
 *
 * <p>The clock is fixed so that date boundaries can be verified deterministically:
 * the test "current date" is 2027-02-10, 08:00 UTC, i.e. before business hours.</p>
 */
@Tag("component")
class BookingServiceTest {

    private static final ZoneId ZONE = ZoneId.of("UTC");
    private static final Instant BEFORE_OPENING = Instant.parse("2027-02-10T08:00:00Z");
    private static final Instant DURING_DAY = Instant.parse("2027-02-10T12:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2027, 2, 10);

    private BookingService service;

    @BeforeEach
    void setUp() {
        service = newService(BEFORE_OPENING);
    }

    private BookingService newService(Instant now) {
        AppConfig config = AppConfig.builder()
                .storeInMemory()
                .zoneId(ZONE)
                .clock(Clock.fixed(now, ZONE))
                .build();
        return new BookingService(config, new ServiceCatalog(), new AppointmentRepository(null));
    }

    private CreateAppointmentRequest request(String name, String email, int serviceId, LocalDate date, String start) {
        return new CreateAppointmentRequest(name, email, serviceId, date.toString(), start);
    }

    private CreateAppointmentRequest validRequest(LocalDate date, String start) {
        return request("Ana Example", "ana@example.com", 1, date, start);
    }

    private ApiException errorFrom(CreateAppointmentRequest request) {
        return assertThrows(ApiException.class, () -> service.create(request));
    }

    private int availableSlotCount(int serviceId, LocalDate date) {
        return service.availability(String.valueOf(serviceId), date.toString()).slots().size();
    }

    @Test
    @DisplayName("FR-01: only active services are returned unless explicitly requested")
    void servicesAreFilteredByActiveFlag() {
        List<Service> active = service.services(false);
        List<Service> all = service.services(true);

        assertAll(
                () -> assertEquals(4, active.size(), "four active services are preconfigured"),
                () -> assertEquals(5, all.size(), "the catalog also holds one inactive service"),
                () -> assertTrue(active.stream().allMatch(Service::active)),
                () -> assertTrue(all.stream().anyMatch(s -> !s.active())));
    }

    @Test
    @DisplayName("FR-02/BR-05/BR-06: availability is aligned to 30 minutes and stays inside business hours")
    void availabilityGeneratesAlignedSlotsWithinBusinessHours() {
        AvailabilityResponse response = service.availability("1", "2027-02-11");
        List<TimeSlot> slots = response.slots();

        assertAll(
                () -> assertEquals(16, slots.size(), "09:00 to 16:30 in 30 minute steps"),
                () -> assertEquals(LocalTime.of(9, 0), slots.get(0).startTime()),
                () -> assertEquals(LocalTime.of(16, 30), slots.get(slots.size() - 1).startTime()),
                () -> assertEquals(30, response.durationMinutes()),
                () -> assertTrue(slots.stream().allMatch(slot ->
                        !slot.startTime().isBefore(LocalTime.of(9, 0))
                                && !slot.endTime().isAfter(LocalTime.of(17, 0)))),
                () -> assertTrue(slots.stream().allMatch(slot ->
                        slot.endTime().equals(slot.startTime().plusMinutes(30)))));
    }

    @Test
    @DisplayName("BR-06: a 60 minute service produces fewer slots and longer intervals")
    void availabilityUsesServiceDuration() {
        List<TimeSlot> slots = service.availability("3", "2027-02-11").slots();

        assertAll(
                () -> assertEquals(15, slots.size(), "09:00 to 16:00 in 30 minute steps"),
                () -> assertTrue(slots.stream().allMatch(slot ->
                        slot.endTime().equals(slot.startTime().plusMinutes(60)))));

        TimeSlot first = slots.get(0);
        Appointment appointment = service.create(request("Ana Example", "ana@example.com", 3, TODAY.plusDays(1),
                first.startTime().toString()));
        assertAll(
                () -> assertEquals(LocalTime.of(9, 0), appointment.startTime()),
                () -> assertEquals(LocalTime.of(10, 0), appointment.endTime(), "end time derives from the service"));
    }

    @Test
    @DisplayName("FR-02: availability for a past date returns an empty list")
    void availabilityForPastDateIsEmpty() {
        assertTrue(service.availability("1", TODAY.minusDays(1).toString()).slots().isEmpty());
    }

    @Test
    @DisplayName("FR-03/FR-04/BR-11: a valid booking is confirmed and receives a unique identifier")
    void createReturnsConfirmedAppointmentWithUniqueIdentifier() {
        Appointment first = service.create(validRequest(TODAY.plusDays(1), "10:00"));
        Appointment second = service.create(validRequest(TODAY.plusDays(1), "11:00"));

        assertAll(
                () -> assertEquals(AppointmentStatus.CONFIRMED, first.status()),
                () -> assertEquals(1001, first.id(), "identifiers start at 1001"),
                () -> assertNotEquals(first.id(), second.id()),
                () -> assertEquals(LocalTime.of(10, 0), first.startTime()),
                () -> assertEquals(LocalTime.of(10, 30), first.endTime()),
                () -> assertEquals(TODAY.plusDays(1), first.appointmentDate()),
                () -> assertEquals(Instant.parse("2027-02-10T08:00:00Z"), first.createdAt(),
                        "createdAt uses the configured clock"),
                () -> assertEquals(2, service.storedAppointmentCount()));
    }

    @ParameterizedTest(name = "FR-09: booking without {0} is rejected")
    @CsvSource({"customerName", "customerEmail", "serviceId", "appointmentDate", "startTime"})
    void missingRequiredFieldsAreRejected(String omittedProperty) {
        CreateAppointmentRequest complete = validRequest(TODAY.plusDays(1), "10:00");
        CreateAppointmentRequest incomplete = switch (omittedProperty) {
            case "customerName" -> new CreateAppointmentRequest(null, complete.customerEmail(),
                    complete.serviceId(), complete.appointmentDate(), complete.startTime());
            case "customerEmail" -> new CreateAppointmentRequest(complete.customerName(), null,
                    complete.serviceId(), complete.appointmentDate(), complete.startTime());
            case "serviceId" -> new CreateAppointmentRequest(complete.customerName(), complete.customerEmail(),
                    null, complete.appointmentDate(), complete.startTime());
            case "appointmentDate" -> new CreateAppointmentRequest(complete.customerName(), complete.customerEmail(),
                    complete.serviceId(), null, complete.startTime());
            default -> new CreateAppointmentRequest(complete.customerName(), complete.customerEmail(),
                    complete.serviceId(), complete.appointmentDate(), null);
        };

        ApiException error = errorFrom(incomplete);

        assertAll(
                () -> assertEquals(ErrorCode.VALIDATION_ERROR, error.errorCode()),
                () -> assertEquals(400, error.httpStatus()),
                () -> assertTrue(error.getMessage().contains(omittedProperty),
                        "the message names the offending field: " + error.getMessage()));
    }

    @Test
    @DisplayName("FR-09: blank required values are rejected like missing ones")
    void blankRequiredValuesAreRejected() {
        ApiException nameError = errorFrom(request("   ", "ana@example.com", 1, TODAY.plusDays(1), "10:00"));
        ApiException emailError = errorFrom(request("Ana Example", "  ", 1, TODAY.plusDays(1), "10:00"));
        ApiException dateError = errorFrom(new CreateAppointmentRequest("Ana Example", "ana@example.com", 1, "", "10:00"));

        assertAll(
                () -> assertEquals(ErrorCode.VALIDATION_ERROR, nameError.errorCode()),
                () -> assertEquals(ErrorCode.VALIDATION_ERROR, emailError.errorCode()),
                () -> assertEquals(ErrorCode.VALIDATION_ERROR, dateError.errorCode()),
                () -> assertEquals(0, service.storedAppointmentCount()));
    }

    @ParameterizedTest(name = "BR-02: email \"{0}\" is rejected")
    @CsvSource({"ana.example.com", "ana@", "@example.com", "ana@com", "ana @example.com", "ana@@example.com"})
    void malformedEmailsAreRejected(String email) {
        ApiException error = errorFrom(request("Ana Example", email, 1, TODAY.plusDays(1), "10:00"));
        assertEquals(ErrorCode.VALIDATION_ERROR, error.errorCode());
    }

    @ParameterizedTest(name = "BR-02: email \"{0}\" is accepted")
    @CsvSource({"ana@example.com", "ana.maria+qa@example.co.uk", "a_b-c%d@example-hub.org"})
    void validEmailsAreAccepted(String email) {
        Appointment appointment = service.create(request("Ana Example", email, 1, TODAY.plusDays(1), "10:00"));
        assertEquals(email, appointment.customerEmail());
    }

    @Test
    @DisplayName("FR-10/TC-007: booking for a past date is rejected")
    void pastDateIsRejected() {
        ApiException error = errorFrom(validRequest(TODAY.minusDays(1), "10:00"));

        assertAll(
                () -> assertEquals(ErrorCode.PAST_DATE, error.errorCode()),
                () -> assertEquals(400, error.httpStatus()),
                () -> assertEquals(0, service.storedAppointmentCount(), "nothing is stored"));
    }

    @Test
    @DisplayName("BR-04 (BVA): the current date is accepted for a slot that has not started yet")
    void currentDateIsAcceptedForAFutureSlot() {
        Appointment appointment = service.create(validRequest(TODAY, "09:00"));
        assertEquals(TODAY, appointment.appointmentDate());
    }

    @Test
    @DisplayName("BR-04/BR-07 (BVA): the current date is rejected for a slot that has already started")
    void currentDateIsRejectedForAnElapsedSlot() {
        BookingService middayService = newService(DURING_DAY);

        ApiException error = assertThrows(ApiException.class,
                () -> middayService.create(validRequest(TODAY, "11:00")));

        assertAll(
                () -> assertEquals(ErrorCode.SLOT_NOT_AVAILABLE, error.errorCode()),
                () -> assertNotEquals(ErrorCode.PAST_DATE, error.errorCode(),
                        "the date itself is valid, only the time has passed"),
                () -> assertTrue(middayService.availability("1", TODAY.toString()).slots().stream()
                                .noneMatch(slot -> slot.startTime().equals(LocalTime.of(11, 0))),
                        "elapsed slots are not offered as available"),
                () -> assertTrue(middayService.availability("1", TODAY.toString()).slots().stream()
                                .allMatch(slot -> slot.startTime().isAfter(LocalTime.of(12, 0)))));
    }

    @ParameterizedTest(name = "BR-05: start time {0} is outside business hours")
    @CsvSource({"08:30", "07:00", "17:00", "16:45"})
    void outsideBusinessHoursIsRejected(String startTime) {
        ApiException error = errorFrom(validRequest(TODAY.plusDays(1), startTime));
        assertEquals(ErrorCode.OUTSIDE_BUSINESS_HOURS, error.errorCode());
    }

    @Test
    @DisplayName("BR-05/BR-06: the last valid slot depends on the service duration")
    void lastSlotDependsOnServiceDuration() {
        Appointment lastThirtyMinuteSlot = service.create(validRequest(TODAY.plusDays(1), "16:30"));
        ApiException tooLateForSixtyMinutes = errorFrom(
                request("Ana Example", "ana@example.com", 3, TODAY.plusDays(1), "16:30"));
        Appointment lastSixtyMinuteSlot = service.create(
                request("Bruno Example", "bruno@example.com", 3, TODAY.plusDays(2), "16:00"));

        assertAll(
                () -> assertEquals(LocalTime.of(17, 0), lastThirtyMinuteSlot.endTime()),
                () -> assertEquals(ErrorCode.OUTSIDE_BUSINESS_HOURS, tooLateForSixtyMinutes.errorCode()),
                () -> assertEquals(LocalTime.of(17, 0), lastSixtyMinuteSlot.endTime()));
    }

    @Test
    @DisplayName("BR-05: a start time that is not aligned to a slot boundary is rejected")
    void misalignedStartTimeIsRejected() {
        ApiException error = errorFrom(validRequest(TODAY.plusDays(1), "10:15"));

        assertAll(
                () -> assertEquals(ErrorCode.SLOT_NOT_ALIGNED, error.errorCode()),
                () -> assertEquals(400, error.httpStatus()));
    }

    @Test
    @DisplayName("FR-08/TC-008/BR-08: an occupied slot is rejected for the same and for another service")
    void occupiedSlotIsRejected() {
        service.create(validRequest(TODAY.plusDays(1), "10:00"));

        ApiException sameService = errorFrom(validRequest(TODAY.plusDays(1), "10:00"));
        ApiException otherService = errorFrom(request("Bruno Example", "bruno@example.com", 2, TODAY.plusDays(1), "10:00"));

        assertAll(
                () -> assertEquals(ErrorCode.SLOT_NOT_AVAILABLE, sameService.errorCode()),
                () -> assertEquals(409, sameService.httpStatus()),
                () -> assertEquals(ErrorCode.SLOT_NOT_AVAILABLE, otherService.errorCode()),
                () -> assertEquals(1, service.storedAppointmentCount(), "no duplicate is created"),
                () -> assertEquals(15, availableSlotCount(1, TODAY.plusDays(1))),
                () -> assertTrue(service.availability("1", TODAY.plusDays(1).toString()).slots().stream()
                        .noneMatch(slot -> slot.startTime().equals(LocalTime.of(10, 0)))));
    }

    @Test
    @DisplayName("BR-06/BR-08: a 60 minute appointment blocks the overlapping 30 minute slots")
    void overlappingAppointmentBlocksOverlappingSlots() {
        service.create(request("Ana Example", "ana@example.com", 3, TODAY.plusDays(1), "10:00"));

        ApiException overlap = errorFrom(validRequest(TODAY.plusDays(1), "10:30"));
        Appointment adjacent = service.create(validRequest(TODAY.plusDays(1), "11:00"));

        assertAll(
                () -> assertEquals(ErrorCode.SLOT_NOT_AVAILABLE, overlap.errorCode()),
                () -> assertEquals(LocalTime.of(11, 0), adjacent.startTime(), "11:00 starts exactly when the long slot ends"));
    }

    @Test
    @DisplayName("TC-016: booking with a nonexistent service is rejected")
    void unknownServiceIsRejected() {
        ApiException error = errorFrom(request("Ana Example", "ana@example.com", 99, TODAY.plusDays(1), "10:00"));

        assertAll(
                () -> assertEquals(ErrorCode.SERVICE_NOT_FOUND, error.errorCode()),
                () -> assertEquals(404, error.httpStatus()));
    }

    @Test
    @DisplayName("BR-03: booking with an inactive service is rejected")
    void inactiveServiceIsRejected() {
        ApiException error = errorFrom(request("Ana Example", "ana@example.com", 5, TODAY.plusDays(1), "10:00"));

        assertAll(
                () -> assertEquals(ErrorCode.SERVICE_INACTIVE, error.errorCode()),
                () -> assertEquals(409, error.httpStatus()));
    }

    @Test
    @DisplayName("FR-07/FR-11/TC-011/TC-014: cancelling releases the slot so it can be booked again")
    void cancellingReleasesTheSlot() {
        Appointment appointment = service.create(validRequest(TODAY.plusDays(1), "10:00"));
        int slotsWhileConfirmed = availableSlotCount(1, TODAY.plusDays(1));

        Appointment cancelled = service.cancel(appointment.id());
        int slotsAfterCancellation = availableSlotCount(1, TODAY.plusDays(1));
        Appointment rebooked = service.create(request("Bruno Example", "bruno@example.com", 2, TODAY.plusDays(1), "10:00"));

        assertAll(
                () -> assertEquals(AppointmentStatus.CANCELLED, cancelled.status()),
                () -> assertEquals(AppointmentStatus.CANCELLED, service.findById(appointment.id()).status(),
                        "the state change is persisted"),
                () -> assertEquals(slotsWhileConfirmed + 1, slotsAfterCancellation),
                () -> assertEquals(LocalTime.of(10, 0), rebooked.startTime()),
                () -> assertEquals(AppointmentStatus.CONFIRMED, rebooked.status()));
    }

    @Test
    @DisplayName("FR-14/TC-012: an appointment cannot be cancelled twice")
    void cancellingTwiceIsRejected() {
        Appointment appointment = service.create(validRequest(TODAY.plusDays(1), "10:00"));
        service.cancel(appointment.id());

        ApiException error = assertThrows(ApiException.class, () -> service.cancel(appointment.id()));

        assertAll(
                () -> assertEquals(ErrorCode.APPOINTMENT_ALREADY_CANCELLED, error.errorCode()),
                () -> assertEquals(409, error.httpStatus()),
                () -> assertEquals(AppointmentStatus.CANCELLED, service.findById(appointment.id()).status(),
                        "the appointment keeps its cancelled status"));
    }

    @Test
    @DisplayName("FR-13/TC-013: cancelling a nonexistent appointment is rejected")
    void cancellingUnknownAppointmentIsRejected() {
        ApiException error = assertThrows(ApiException.class, () -> service.cancel(987654));

        assertAll(
                () -> assertEquals(ErrorCode.APPOINTMENT_NOT_FOUND, error.errorCode()),
                () -> assertEquals(404, error.httpStatus()));
    }

    @Test
    @DisplayName("FR-05: retrieving a nonexistent appointment is rejected")
    void retrievingUnknownAppointmentIsRejected() {
        ApiException error = assertThrows(ApiException.class, () -> service.findById(987654));
        assertEquals(ErrorCode.APPOINTMENT_NOT_FOUND, error.errorCode());
    }

    @Test
    @DisplayName("FR-06: registered appointments can be filtered by customer email")
    void appointmentsCanBeFilteredByEmail() {
        service.create(validRequest(TODAY.plusDays(1), "10:00"));
        service.create(request("Bruno Example", "bruno@example.com", 1, TODAY.plusDays(1), "11:00"));

        assertAll(
                () -> assertEquals(2, service.findAll(null).size()),
                () -> assertEquals(1, service.findAll("ana@example.com").size()),
                () -> assertEquals(1, service.findAll("ANA@EXAMPLE.COM").size(), "the filter ignores case"),
                () -> assertEquals(0, service.findAll("nobody@example.com").size()));
    }

    @Test
    @DisplayName("BR-07/NFR-03: concurrent requests for the same slot produce exactly one appointment")
    void concurrentBookingsForTheSameSlotProduceOneAppointment() throws Exception {
        int attempts = 12;
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(6);
        java.util.List<java.util.concurrent.Future<ErrorCode>> results = new java.util.ArrayList<>();
        java.util.concurrent.CountDownLatch startGate = new java.util.concurrent.CountDownLatch(1);

        for (int i = 0; i < attempts; i++) {
            results.add(pool.submit(() -> {
                startGate.await();
                try {
                    service.create(validRequest(TODAY.plusDays(1), "10:00"));
                    return null;
                } catch (ApiException e) {
                    return e.errorCode();
                }
            }));
        }
        startGate.countDown();

        int confirmed = 0;
        int conflicts = 0;
        for (java.util.concurrent.Future<ErrorCode> result : results) {
            ErrorCode code = result.get();
            if (code == null) {
                confirmed++;
            } else {
                assertEquals(ErrorCode.SLOT_NOT_AVAILABLE, code);
                conflicts++;
            }
        }
        pool.shutdownNow();

        assertEquals(1, confirmed, "exactly one request may win the slot");
        assertEquals(attempts - 1, conflicts);
        assertEquals(1, service.storedAppointmentCount());
    }
}
