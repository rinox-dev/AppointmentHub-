package com.appointmenthub.model;

/**
 * Request body of {@code POST /api/appointments}.
 *
 * <p>Every field is declared as a nullable type so that missing properties can be
 * reported as validation errors instead of being silently defaulted (BR-01).</p>
 *
 * @param customerName    customer's name
 * @param customerEmail   customer's contact email
 * @param serviceId       requested service identifier
 * @param appointmentDate appointment date, formatted as {@code yyyy-MM-dd}
 * @param startTime       appointment start time, formatted as {@code HH:mm}
 */
public record CreateAppointmentRequest(
        String customerName,
        String customerEmail,
        Integer serviceId,
        String appointmentDate,
        String startTime) {
}
