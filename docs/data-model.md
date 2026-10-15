# AppointmentHub — data model

Version 1.0 · Implemented in `com.appointmenthub.model` and `com.appointmenthub.store`

## 1. Appointment

| Field | Java type | JSON representation | Description |
|---|---|---|---|
| `id` | `int` | number | Unique identifier, first value 1001, never reused |
| `customerName` | `String` | string | Customer name, 2–100 characters after trimming |
| `customerEmail` | `String` | string | Contact email, validated with the documented rule (BR-02) |
| `serviceId` | `int` | number | Requested service |
| `appointmentDate` | `LocalDate` | `"yyyy-MM-dd"` | Appointment date in the configured time zone |
| `startTime` | `LocalTime` | `"HH:mm"` | Start time, aligned to a slot boundary |
| `endTime` | `LocalTime` | `"HH:mm"` | `startTime` + service duration |
| `status` | `AppointmentStatus` | `"CONFIRMED"` / `"CANCELLED"` | Occupies or no longer occupies the slot |
| `createdAt` | `Instant` | ISO-8601 UTC | Creation timestamp |

`Appointment` is an immutable record; cancelling produces a copy with the new
status, which keeps the state transition explicit.

### Statuses and transitions

```text
   create                cancel
      |                     |
      v                     v
 CONFIRMED  -------------> CANCELLED
      ^                     |
      |                     +-- cancel again  -> 409 APPOINTMENT_ALREADY_CANCELLED
      |                     +-- retrieve      -> 200 with status CANCELLED
      +-- no transition back from CANCELLED
```

## 2. Service

| Field | Java type | JSON representation | Description |
|---|---|---|---|
| `id` | `int` | number | Unique service identifier |
| `name` | `String` | string | Service name |
| `durationMinutes` | `int` | number | Appointment duration, positive |
| `active` | `boolean` | boolean | Only active services can be booked |

Preconfigured catalog:

| id | name | durationMinutes | active |
|---|---|---|---|
| 1 | General Consultation | 30 | true |
| 2 | Dental Cleaning | 30 | true |
| 3 | Physiotherapy Session | 60 | true |
| 4 | Nutrition Assessment | 45 | true |
| 5 | Legacy Wellness Check | 30 | false (exists to verify BR-03) |

## 3. Availability

Availability is not stored: it is derived from the catalog, the configuration and
the stored appointments.

```text
for each aligned start time between opening and closing:
    end = start + service.durationMinutes
    keep the slot when end <= closing time
                     and no CONFIRMED appointment overlaps [start, end)
                     and (date is in the future or start is after the current time)
```

## 4. Storage

| Aspect | Decision |
|---|---|
| Default | JSON file `data/appointments.json`, rewritten atomically after every change |
| Tests and demos | In-memory storage (`--store=memory` or `APPOINTMENTHUB_STORE=memory`), no file, clean state on every start |
| Identifier sequence | Held in memory and restored from the highest stored identifier when the file is loaded, so identifiers are never reused |
| Concurrency | All read and write operations are synchronised; the availability check and the creation happen inside one lock (NFR-03) |
| Rationale | The first release needs persistence and verifiability, not a database; the PDR allows "a simple storage solution suitable for development" |

## 5. Relationships

```text
Service 1 ---- * Appointment        (serviceId)
Appointment 1 -- 1 TimeSlot         (derived from date + startTime + duration)
Resource 1 ---- * Appointment       (single shared resource, BR-08)
```

The single-resource assumption means an appointment blocks its interval for every
service. It is documented in `business-rules.md` and verified by the conflict
scenarios.
