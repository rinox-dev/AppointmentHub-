package com.appointmenthub.model;

/**
 * Appointment lifecycle status (PDR section 6.2).
 *
 * <p>Only two statuses exist in the first release:
 * {@link #CONFIRMED} (occupies the time slot) and {@link #CANCELLED}
 * (no longer occupies the time slot).</p>
 */
public enum AppointmentStatus {
    CONFIRMED,
    CANCELLED
}
