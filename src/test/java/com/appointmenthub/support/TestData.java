package com.appointmenthub.support;

import com.google.gson.JsonObject;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Controlled test data (PDR sections 14.5 and 15.3).
 *
 * <p>Only fictional personal data is used (NFR-09), and dates are computed at
 * run time so that tests never depend on data left behind by a previous run.</p>
 */
public final class TestData {

    public static final String VALID_NAME = "Ana Example";
    public static final String VALID_EMAIL = "ana@example.com";
    public static final String SECOND_NAME = "Bruno Example";
    public static final String SECOND_EMAIL = "bruno@example.com";
    public static final int ACTIVE_SERVICE_ID = 1;
    public static final int SECOND_SERVICE_ID = 2;
    public static final int LONG_SERVICE_ID = 3;
    public static final int INACTIVE_SERVICE_ID = 5;
    public static final int UNKNOWN_SERVICE_ID = 99;
    public static final int UNKNOWN_APPOINTMENT_ID = 987654;

    private TestData() {
    }

    /**
     * A far future date that is unlikely to be used by another test or by manual
     * exploration, so runs stay independent from each other.
     */
    public static LocalDate freeDate() {
        return LocalDate.now(ZoneOffset.UTC).plusDays(90 + ThreadLocalRandom.current().nextInt(180));
    }

    public static LocalDate futureDate(int offsetDays) {
        return LocalDate.now(ZoneOffset.UTC).plusDays(offsetDays);
    }

    public static LocalDate pastDate() {
        return LocalDate.now(ZoneOffset.UTC).minusDays(1);
    }

    /** Builds a valid {@code POST /api/appointments} body. */
    public static String bookingBody(String name, String email, int serviceId, LocalDate date, String startTime) {
        JsonObject body = new JsonObject();
        body.addProperty("customerName", name);
        body.addProperty("customerEmail", email);
        body.addProperty("serviceId", serviceId);
        body.addProperty("appointmentDate", date.toString());
        body.addProperty("startTime", startTime);
        return body.toString();
    }

    public static String validBookingBody(LocalDate date, String startTime) {
        return bookingBody(VALID_NAME, VALID_EMAIL, ACTIVE_SERVICE_ID, date, startTime);
    }

    /** Same as {@link #validBookingBody} but without the named property. */
    public static String bookingBodyWithout(String omittedProperty, LocalDate date, String startTime) {
        JsonObject body = new JsonObject();
        if (!"customerName".equals(omittedProperty)) {
            body.addProperty("customerName", VALID_NAME);
        }
        if (!"customerEmail".equals(omittedProperty)) {
            body.addProperty("customerEmail", VALID_EMAIL);
        }
        if (!"serviceId".equals(omittedProperty)) {
            body.addProperty("serviceId", ACTIVE_SERVICE_ID);
        }
        if (!"appointmentDate".equals(omittedProperty)) {
            body.addProperty("appointmentDate", date.toString());
        }
        if (!"startTime".equals(omittedProperty)) {
            body.addProperty("startTime", startTime);
        }
        return body.toString();
    }
}
