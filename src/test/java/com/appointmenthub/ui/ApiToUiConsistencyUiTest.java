package com.appointmenthub.ui;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.appointmenthub.support.TestData;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TC-015 / NFR-04: appointments created or cancelled through the API are shown
 * correctly by the web interface, and the other way round (AC-10).
 */
class ApiToUiConsistencyUiTest extends BaseUiTest {

    private String firstSlotFromApi(LocalDate date) {
        return api.get("/api/availability?serviceId=" + TestData.ACTIVE_SERVICE_ID + "&date=" + date)
                .json().getAsJsonArray("slots").get(0).getAsJsonObject().get("startTime").getAsString();
    }

    @Test
    @DisplayName("TC-015: an appointment created through the API is displayed with matching details")
    void appointmentCreatedThroughApiIsDisplayed() {
        LocalDate date = freeDate();
        String startTime = firstSlotFromApi(date);
        int appointmentId = bookViaApi(date, startTime);

        home.open().waitForAppointment(appointmentId);

        assertAll(
                () -> assertEquals(TestData.SECOND_NAME, home.appointmentCustomer(appointmentId)),
                () -> assertEquals(date.toString(), home.appointmentDate(appointmentId)),
                () -> assertEquals(startTime + " - " + endTimeOf(startTime), home.appointmentTime(appointmentId)),
                () -> assertEquals("CONFIRMED", home.appointmentStatus(appointmentId)));
    }

    @Test
    @DisplayName("TC-015: a cancellation performed through the API is reflected in the interface")
    void cancellationThroughApiIsReflectedInTheInterface() {
        LocalDate date = freeDate();
        int appointmentId = bookViaApi(date, firstSlotFromApi(date));
        home.open().waitForAppointment(appointmentId);

        api.delete("/api/appointments/" + appointmentId);
        home.clickRefreshAppointments().waitForStatus(appointmentId, "CANCELLED");

        assertAll(
                () -> assertEquals("CANCELLED", home.appointmentStatus(appointmentId)),
                () -> assertEquals("CANCELLED", api.get("/api/appointments/" + appointmentId).string("status")));
    }

    @Test
    @DisplayName("TC-015: a booking made through the interface is visible through the API")
    void bookingThroughTheInterfaceIsVisibleThroughTheApi() {
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

        var appointments = api.get("/api/appointments?customerEmail=ana@example.com").jsonArray();

        assertAll(
                () -> assertEquals(1, appointments.size()),
                () -> assertEquals(date.toString(), appointments.get(0).getAsJsonObject().get("appointmentDate").getAsString()),
                () -> assertEquals(startTime, appointments.get(0).getAsJsonObject().get("startTime").getAsString()));
    }

    private String endTimeOf(String startTime) {
        int hour = Integer.parseInt(startTime.substring(0, 2));
        int minute = Integer.parseInt(startTime.substring(3));
        int total = hour * 60 + minute + 30;
        return String.format("%02d:%02d", total / 60, total % 60);
    }
}
