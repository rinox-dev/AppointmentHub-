package com.appointmenthub.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Response body of {@code GET /api/availability?serviceId={id}&date={date}}.
 *
 * @param serviceId       requested service
 * @param serviceName     name of the requested service
 * @param date            requested date
 * @param durationMinutes duration of the service, used to build the slots
 * @param slots           available time slots for the requested date
 */
public record AvailabilityResponse(
        int serviceId,
        String serviceName,
        LocalDate date,
        int durationMinutes,
        List<TimeSlot> slots) {
}
