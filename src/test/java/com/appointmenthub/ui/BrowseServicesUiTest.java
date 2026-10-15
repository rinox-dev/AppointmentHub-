package com.appointmenthub.ui;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * UI-001 / TC-001: the service catalog is displayed on the home page (FR-01).
 */
class BrowseServicesUiTest extends BaseUiTest {

    @Test
    @DisplayName("UI-001/TC-001: the home page lists the active services returned by the API")
    void homePageListsActiveServices() {
        home.open();

        List<String> displayed = home.serviceNames();
        List<String> expected = api.get("/api/services").jsonArray().asList().stream()
                .map(element -> element.getAsJsonObject().get("name").getAsString())
                .toList();

        assertAll(
                () -> assertFalse(displayed.isEmpty(), "at least one service is displayed"),
                () -> assertEquals(expected, displayed, "the interface shows exactly the active services"),
                () -> assertTrue(home.pageSource().contains("AppointmentHub")));
    }

    @Test
    @DisplayName("UI-001: inactive services are not offered for booking")
    void inactiveServicesAreNotDisplayed() {
        home.open();

        assertFalse(home.serviceNames().contains("Legacy Wellness Check"),
                "the inactive service is not bookable through the interface");
    }

    @Test
    @DisplayName("UI-001: selecting a service is confirmed to the user")
    void selectingServiceShowsConfirmation() {
        home.open().selectService("General Consultation");

        assertAll(
                () -> assertTrue(home.selectedServiceText().contains("General Consultation")),
                () -> assertTrue(home.selectedServiceText().contains("30 minutes")));
    }
}
