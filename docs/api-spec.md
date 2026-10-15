# AppointmentHub — REST API specification

Version 1.0 · Applies to the implementation in `src/main/java/com/appointmenthub/api`

This document is the contract between the web interface, the automated tests and
the API. It refines section 10 of the [PDR](PDR.md) and records the exact
behaviour that was implemented and verified.

* Base URL (default): `http://localhost:8080`
* All request and response bodies are JSON encoded in UTF-8.
* Dates use `yyyy-MM-dd` (strict: `2027-2-10` is rejected).
* Times use `HH:mm` (strict: `10:00:00` is rejected).
* Timestamps use ISO-8601 in UTC, for example `2026-10-15T04:26:58.078434824Z`.
* The time zone of the server is configurable and defaults to `UTC`
  (BR-13, see [configuration](configuration.md)).

## 1. Endpoints

| Method | Endpoint | Purpose | Success |
|---|---|---|---|
| GET | `/api/services` | Retrieve services | 200 |
| GET | `/api/availability?serviceId={id}&date={date}` | Retrieve available time slots | 200 |
| POST | `/api/appointments` | Create an appointment | 201 |
| GET | `/api/appointments` | Retrieve registered appointments | 200 |
| GET | `/api/appointments/{id}` | Retrieve a specific appointment | 200 |
| DELETE | `/api/appointments/{id}` | Cancel an appointment | 200 |

The web interface is served from `/` and `/index.html`, `/styles.css`, `/app.js`.

## 2. GET /api/services

Returns the bookable services ordered by identifier.

**Response `200 OK`**

```json
[
  { "id": 1, "name": "General Consultation", "durationMinutes": 30, "active": true },
  { "id": 2, "name": "Dental Cleaning", "durationMinutes": 30, "active": true },
  { "id": 3, "name": "Physiotherapy Session", "durationMinutes": 60, "active": true },
  { "id": 4, "name": "Nutrition Assessment", "durationMinutes": 45, "active": true }
]
```

Only active services are returned (FR-01). The preconfigured catalog also holds
one inactive service (id 5, `Legacy Wellness Check`), which exists to verify
BR-03.

**Testing extension:** `GET /api/services?includeInactive=true` also returns
inactive services. The parameter exists so that tests and the demo can reference
an inactive service; the web interface never uses it.

| Situation | Status | `errorCode` |
|---|---|---|
| Method other than GET | 405 | `METHOD_NOT_ALLOWED` |
| Unknown sub-path such as `/api/services/1` | 404 | `NOT_FOUND` |

## 3. GET /api/availability

Returns the time slots that can still be booked for a service and date.

**Query parameters**

| Name | Required | Rule |
|---|---|---|
| `serviceId` | yes | Must be the identifier of an existing, active service |
| `date` | yes | Must be a valid `yyyy-MM-dd` date |

**Response `200 OK`**

```json
{
  "serviceId": 1,
  "serviceName": "General Consultation",
  "date": "2027-02-10",
  "durationMinutes": 30,
  "slots": [
    { "startTime": "09:00", "endTime": "09:30" },
    { "startTime": "09:30", "endTime": "10:00" }
  ]
}
```

**Rules applied to the generated slots**

1. Slots start at the configured opening time and step by the configured slot
   size (30 minutes by default); a slot is only generated when the whole
   appointment fits inside business hours (BR-05, BR-06).
2. Slots that overlap a `CONFIRMED` appointment are removed, regardless of the
   service that created them (single shared resource, BR-08).
3. On the current date, slots whose start time has already passed are removed
   (BR-04, BR-07).
4. For a date before the current date the response is `200` with an empty
   `slots` array: the date itself is valid, there is simply nothing to offer.

| Situation | Status | `errorCode` |
|---|---|---|
| `serviceId` missing or not an integer | 400 | `VALIDATION_ERROR` |
| `date` missing | 400 | `VALIDATION_ERROR` |
| `date` in an invalid format | 400 | `INVALID_DATE_FORMAT` |
| Service does not exist | 404 | `SERVICE_NOT_FOUND` |
| Service exists but is inactive | 409 | `SERVICE_INACTIVE` |

## 4. POST /api/appointments

Creates an appointment. The server re-validates everything, even when the web
interface already validated the input (NFR-07).

**Request**

```http
POST /api/appointments
Content-Type: application/json

{
  "customerName": "Ana Example",
  "customerEmail": "ana@example.com",
  "serviceId": 1,
  "appointmentDate": "2027-02-10",
  "startTime": "10:00"
}
```

**Response `201 Created`**

```json
{
  "id": 1001,
  "customerName": "Ana Example",
  "customerEmail": "ana@example.com",
  "serviceId": 1,
  "appointmentDate": "2027-02-10",
  "startTime": "10:00",
  "endTime": "10:30",
  "status": "CONFIRMED",
  "createdAt": "2026-10-15T04:26:58.078434824Z"
}
```

The response always contains the nine fields of the appointment entity
(see [data model](data-model.md)). `endTime` is derived from the service duration
(BR-06) and `id` is unique and never reused (FR-04, BR-11).

**Validation order** (the first failing rule determines the response):

1. The body must be present and valid JSON.
2. Required fields: `customerName`, `customerEmail`, `serviceId`,
   `appointmentDate`, `startTime`.
3. Field formats: email (BR-02), date, time, name length 2–100 characters.
4. The service must exist and be active (BR-03).
5. The date must not be in the past (FR-10).
6. The start time must match a generated slot and fit in business hours
   (BR-05, BR-06).
7. The slot must be free at the moment of creation (BR-07, BR-08).

| Situation | Status | `errorCode` |
|---|---|---|
| Body missing or empty | 400 | `MISSING_BODY` |
| Body is not valid JSON for the contract (including wrong data types) | 400 | `MALFORMED_REQUEST` |
| Required field missing or blank | 400 | `VALIDATION_ERROR` |
| Email does not match the documented rule | 400 | `VALIDATION_ERROR` |
| Date not in `yyyy-MM-dd` | 400 | `INVALID_DATE_FORMAT` |
| Time not in `HH:mm` | 400 | `INVALID_TIME_FORMAT` |
| Date before the current date | 400 | `PAST_DATE` |
| Start time outside business hours or ending after closing time | 400 | `OUTSIDE_BUSINESS_HOURS` |
| Start time not aligned to a slot boundary | 400 | `SLOT_NOT_ALIGNED` |
| Service does not exist | 404 | `SERVICE_NOT_FOUND` |
| Service is inactive | 409 | `SERVICE_INACTIVE` |
| Slot occupied, or already started on the current date | 409 | `SLOT_NOT_AVAILABLE` |

## 5. GET /api/appointments

Returns all registered appointments, most recently created first.

**Response `200 OK`** — JSON array of appointment objects.

**Testing extension:** `GET /api/appointments?customerEmail={email}` filters by
customer email (case-insensitive exact match). The web interface does not use it.

## 6. GET /api/appointments/{id}

Returns one appointment.

| Situation | Status | `errorCode` |
|---|---|---|
| Appointment exists | 200 | – |
| Appointment does not exist | 404 | `APPOINTMENT_NOT_FOUND` |
| Identifier is not an integer | 400 | `VALIDATION_ERROR` |

## 7. DELETE /api/appointments/{id}

Cancels a confirmed appointment. The appointment keeps its data and changes
`status` to `CANCELLED`; the time slot is released immediately (FR-11, BR-10).

**Response `200 OK`** — the updated appointment object.

| Situation | Status | `errorCode` |
|---|---|---|
| Confirmed appointment cancelled | 200 | – |
| Appointment already cancelled | 409 | `APPOINTMENT_ALREADY_CANCELLED` |
| Appointment does not exist | 404 | `APPOINTMENT_NOT_FOUND` |
| Identifier is not an integer | 400 | `VALIDATION_ERROR` |

## 8. Error response format

Every error uses the same three fields (NFR-08, AC-11):

```json
{
  "errorCode": "SLOT_NOT_AVAILABLE",
  "message": "The time slot 10:00 on 2027-02-10 is already occupied.",
  "status": 409
}
```

* `errorCode` is a stable functional code (see the tables above), meant for
  automated assertions.
* `message` explains the problem in plain language and never contains stack
  traces, class names or package names.
* `status` repeats the HTTP status code so a single JSON object is self-contained.

| HTTP status | Used for |
|---|---|
| 200 OK | Successful query, or a cancellation processed with a response |
| 201 Created | Appointment created |
| 400 Bad Request | Invalid, missing or malformed input |
| 404 Not Found | Appointment or service does not exist |
| 405 Method Not Allowed | Method not supported by the endpoint |
| 409 Conflict | Slot occupied, inactive service, or a state transition that is not allowed |
| 500 Internal Server Error | Unexpected server error (details are logged server side only) |
