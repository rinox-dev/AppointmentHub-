package com.appointmenthub.ui;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appointmenthub.support.TestData;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * UI-002 / TC-019: the complete booking journey through the web interface
 * (FR-03, FR-12, NFR-04, AC-10).
 */
class BookAppointmentUiTest extends BaseUiTest {

    @Test
    @DisplayName("UI-002/TC-019: a booking made through the interface is confirmed and displayed")
    void bookingThroughTheInterfaceIsConfirmed() {
        LocalDate date = freeDate();
        String startTime = firstSlotFromApi(date);

        home.open()
                .selectService("General Consultation")
                .setDate(date)
                .clickCheckAvailability()
                .selectSlot(startTime)
                .fillCustomer("Ana Example", "ana@example.com")
                .submitBooking()
                .waitForSuccessMessage();

        String success = home.successMessage().orElseThrow();
        Matcher idMatcher = Pattern.compile("Appointment (\\d+) confirmed").matcher(success);
        assertAll(
                () -> assertTrue(idMatcher.find(), "the confirmation contains the appointment identifier: " + success),
                () -> assertTrue(success.contains(date.toString()), "the confirmation contains the date"),
                () -> assertTrue(success.contains(startTime), "the confirmation contains the start time"),
                () -> assertTrue(home.errorMessage().isEmpty(), "no error message is displayed"));

        int appointmentId = Integer.parseInt(idMatcher.group(1));
        home.waitForAppointment(appointmentId);

        assertAll(
                () -> assertEquals("CONFIRMED", home.appointmentStatus(appointmentId)),
                () -> assertEquals("Ana Example", home.appointmentCustomer(appointmentId)),
                () -> assertEquals(date.toString(), home.appointmentDate(appointmentId)),
                () -> assertEquals("CONFIRMED", api.get("/api/appointments/" + appointmentId).string("status"),
                        "the appointment also exists on the server"),
                () -> assertEquals("ana@example.com",
                        api.get("/api/appointments/" + appointmentId).string("customerEmail")));
    }

    @Test
    @DisplayName("UI-002: after a booking the slot is no longer offered by the interface")
    void bookedSlotDisappearsFromAvailability() {
        LocalDate date = freeDate();
        String startTime = firstSlotFromApi(date);

        home.open()
                .selectService("General Consultation")
                .setDate(date)
                .clickCheckAvailability()
                .selectSlot(startTime)
                .fillCustomer("Ana Example", "ana@example.com")
                .submitBooking()
                .waitForSuccessMessage();

        List<String> slotsAfterBooking = home.clickCheckAvailability().availableSlots();

        assertAll(
                () -> assertFalse(slotsAfterBooking.contains(startTime), "the booked slot is no longer offered"),
                () -> assertTrue(home.selectedSlotText().contains("No time slot selected"),
                        "the previous selection is cleared"));
    }

    @Test
    @DisplayName("UI-002: a second booking on the same page works after the form is reused")
    void theFormCanBeReusedForASecondBooking() {
        LocalDate date = freeDate();
        String firstSlot = firstSlotFromApi(date);

        home.open()
                .selectService("General Consultation")
                .setDate(date)
                .clickCheckAvailability()
                .selectSlot(firstSlot)
                .fillCustomer("Ana Example", "ana@example.com")
                .submitBooking()
                .waitForSuccessMessage();

        String secondSlot = home.clickCheckAvailability().availableSlots().get(0);
        home.selectSlot(secondSlot)
                .fillCustomer("Bruno Example", "bruno@example.com")
                .submitBooking()
                .waitForSuccessMessage();

        assertAll(
                () -> assertFalse(firstSlot.equals(secondSlot)),
                () -> assertTrue(home.successMessage().orElseThrow().contains(secondSlot)),
                () -> assertEquals(2, api.get("/api/appointments").jsonArray().size(),
                        "both bookings are registered on the server"));
    }

    private String firstSlotFromApi(LocalDate date) {
        return api.get("/api/availability?serviceId=" + TestData.ACTIVE_SERVICE_ID + "&date=" + date)
                .json().getAsJsonArray("slots").get(0).getAsJsonObject().get("startTime").getAsString();
    }
}
