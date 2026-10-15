package com.appointmenthub.model;

import java.time.LocalTime;

/**
 * An available time interval returned by {@code GET /api/availability}.
 *
 * @param startTime slot start time, formatted as {@code HH:mm}
 * @param endTime   slot end time, formatted as {@code HH:mm}
 */
public record TimeSlot(LocalTime startTime, LocalTime endTime) {
}
