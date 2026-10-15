package com.appointmenthub.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Appointment entity (PDR section 6.1).
 *
 * <p>Serialised field order matches the documented REST contract:
 * id, customerName, customerEmail, serviceId, appointmentDate, startTime,
 * endTime, status, createdAt.</p>
 *
 * <p>Instances are immutable; {@link #withStatus(AppointmentStatus)} returns a
 * copy, which keeps the state transition in {@code CONFIRMED -> CANCELLED}
 * explicit and easy to verify.</p>
 */
public record Appointment(
        int id,
        String customerName,
        String customerEmail,
        int serviceId,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime,
        AppointmentStatus status,
        Instant createdAt) {

    public Appointment withStatus(AppointmentStatus newStatus) {
        return new Appointment(id, customerName, customerEmail, serviceId,
                appointmentDate, startTime, endTime, newStatus, createdAt);
    }

    /** True when this appointment's interval overlaps the given interval. */
    public boolean overlaps(LocalTime otherStart, LocalTime otherEnd) {
        return startTime.isBefore(otherEnd) && endTime.isAfter(otherStart);
    }
}
