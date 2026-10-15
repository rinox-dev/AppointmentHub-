package com.appointmenthub.model;

/**
 * A bookable service (PDR section 6.3).
 *
 * @param id              unique service identifier
 * @param name            service name
 * @param durationMinutes appointment duration; must be a positive integer
 * @param active          whether the service can be booked
 */
public record Service(int id, String name, int durationMinutes, boolean active) {
}
