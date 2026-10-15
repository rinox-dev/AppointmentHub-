package com.appointmenthub.error;

/**
 * Functional error codes returned by the REST API (PDR section 10.2, NFR-08).
 *
 * <p>Each code maps to exactly one HTTP status code, which keeps error responses
 * consistent and easy to assert in API tests.</p>
 */
public enum ErrorCode {

    /** Required field missing, blank, or semantically invalid. */
    VALIDATION_ERROR(400),
    /** No request body was supplied for an operation that requires one. */
    MISSING_BODY(400),
    /** The request body is not valid JSON for the expected contract. */
    MALFORMED_REQUEST(400),
    /** The date is not a valid {@code yyyy-MM-dd} value. */
    INVALID_DATE_FORMAT(400),
    /** The time is not a valid {@code HH:mm} value. */
    INVALID_TIME_FORMAT(400),
    /** The appointment date is before the system's current date (FR-10). */
    PAST_DATE(400),
    /** The requested time is outside the configured business hours (BR-05). */
    OUTSIDE_BUSINESS_HOURS(400),
    /** The requested time does not match a generated slot boundary. */
    SLOT_NOT_ALIGNED(400),
    /** No API resource matches the requested path. */
    NOT_FOUND(404),
    /** The requested service does not exist. */
    SERVICE_NOT_FOUND(404),
    /** The requested appointment does not exist (FR-13). */
    APPOINTMENT_NOT_FOUND(404),
    /** The HTTP method is not supported by the endpoint. */
    METHOD_NOT_ALLOWED(405),
    /** The time slot is already occupied (FR-08). */
    SLOT_NOT_AVAILABLE(409),
    /** The service exists but is not active (BR-03). */
    SERVICE_INACTIVE(409),
    /** The appointment was already cancelled (FR-14). */
    APPOINTMENT_ALREADY_CANCELLED(409),
    /** Unexpected server error; details are never exposed to the client. */
    INTERNAL_ERROR(500);

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
