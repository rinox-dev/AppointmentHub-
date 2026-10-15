package com.appointmenthub.ui;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appointmenthub.support.TestData;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * UI-003 / TC-004: required field validation in the interface (FR-09, FR-12).
 *
 * <p>The interface must explain the problem and must not create an appointment,
 * and the API must reject the same data if the interface validation is bypassed
 * (NFR-07).</p>
 */
class BookingValidationUiTest extends BaseUiTest {

    private LocalDate prepareFormWithFirstSlot() {
        LocalDate date = freeDate();
        String startTime = api.get("/api/availability?serviceId=" + TestData.ACTIVE_SERVICE_ID + "&date=" + date)
                .json().getAsJsonArray("slots").get(0).getAsJsonObject().get("startTime").getAsString();

        home.open()
                .selectService("General Consultation")
                .setDate(date)
                .clickCheckAvailability()
                .selectSlot(startTime);
        return date;
    }

    @Test
    @DisplayName("UI-003/TC-004: submitting the form without a name shows a validation message")
    void bookingWithoutNameShowsValidationMessage() {
        prepareFormWithFirstSlot();
        int appointmentsBefore = api.get("/api/appointments").jsonArray().size();

        home.fillCustomer("", "ana@example.com").submitBooking().waitForErrorMessage();

        assertAll(
                () -> assertTrue(home.errorMessage().orElseThrow().contains("name"),
                        "the message explains that the name is required"),
                () -> assertTrue(home.successMessage().isEmpty(), "no success message is displayed"),
                () -> assertEquals(appointmentsBefore, api.get("/api/appointments").jsonArray().size(),
                        "no appointment is created"));
    }

    @Test
    @DisplayName("UI-003: submitting the form without an email shows a validation message")
    void bookingWithoutEmailShowsValidationMessage() {
        prepareFormWithFirstSlot();

        home.fillCustomer("Ana Example", "").submitBooking().waitForErrorMessage();

        assertAll(
                () -> assertTrue(home.errorMessage().orElseThrow().contains("email")),
                () -> assertTrue(home.successMessage().isEmpty()));
    }

    @Test
    @DisplayName("UI-003/BR-02: a malformed email is reported to the user")
    void malformedEmailIsReported() {
        prepareFormWithFirstSlot();

        home.fillCustomer("Ana Example", "ana.example.com").submitBooking().waitForErrorMessage();

        assertAll(
                () -> assertTrue(home.errorMessage().orElseThrow().contains("valid email address")),
                () -> assertTrue(home.successMessage().isEmpty()));
    }

    @Test
    @DisplayName("UI-003: a booking without a selected time slot is rejected by the interface")
    void bookingWithoutSlotIsRejected() {
        LocalDate date = freeDate();
        int appointmentsBefore = api.get("/api/appointments").jsonArray().size();

        home.open()
                .selectService("General Consultation")
                .setDate(date)
                .fillCustomer("Ana Example", "ana@example.com")
                .submitBooking()
                .waitForErrorMessage();

        assertAll(
                () -> assertTrue(home.errorMessage().orElseThrow().contains("time slot")),
                () -> assertFalse(home.successMessage().isPresent()),
                () -> assertEquals(appointmentsBefore, api.get("/api/appointments").jsonArray().size()));
    }

    @Test
    @DisplayName("NFR-07: the API rejects invalid data even when the interface is bypassed")
    void apiValidatesDataIndependentlyOfTheInterface() {
        int appointmentsBefore = api.get("/api/appointments").jsonArray().size();

        int status = api.post("/api/appointments",
                TestData.bookingBody("", "not-an-email", TestData.ACTIVE_SERVICE_ID, freeDate(), "10:00")).status();

        assertAll(
                () -> assertEquals(400, status),
                () -> assertEquals(appointmentsBefore, api.get("/api/appointments").jsonArray().size()));
    }
}
