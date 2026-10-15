# AppointmentHub — business rules

Version 1.0 · Implemented in `BookingService`, enforced on the server

The rules below refine section 9 of the [PDR](PDR.md). Every rule is enforced by
the REST API, so it also holds when the web interface is bypassed (NFR-07). The
"verified by" column points to the automated checks that cover the rule.

| ID | Rule | How it is enforced | Verified by |
|---|---|---|---|
| BR-01 | Required fields | `customerName`, `customerEmail`, `serviceId`, `appointmentDate` and `startTime` must be present and non-blank; otherwise `400 VALIDATION_ERROR` naming the field | `BookingServiceTest`, `BookingApiTest`, Postman *05 Negative validation* |
| BR-02 | Email format | `^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}$`, maximum 254 characters. A TLD is required, so `ana@com` is rejected | `BookingServiceTest`, `BookingApiTest`, Postman email cases |
| BR-03 | Active service | Only services with `active = true` can be booked or queried; otherwise `409 SERVICE_INACTIVE` | `BookingServiceTest`, `BookingApiTest`, Postman TC-021 |
| BR-04 | Valid date | A date before the current date in the configured time zone is rejected with `400 PAST_DATE`. The current date itself is accepted while the slot has not started | `BookingServiceTest` (BVA cases), `BookingApiTest`, Postman TC-007 |
| BR-05 | Business hours | Slots are generated from the opening time in steps of the configured slot size; a booking is accepted only when the whole appointment fits before the closing time, otherwise `400 OUTSIDE_BUSINESS_HOURS` | `BookingServiceTest`, `BookingApiTest`, Postman TC-022 |
| BR-06 | Duration | `endTime = startTime + service.durationMinutes`; the last possible start time depends on the service (16:30 for 30 minutes, 16:00 for 60 minutes) | `BookingServiceTest`, `AvailabilityApiTest`, Postman creation folder |
| BR-07 | Availability | The slot is checked again inside the creation step, so a slot that became occupied between the query and the confirmation is rejected with `409 SLOT_NOT_AVAILABLE` | `BookingServiceTest`, `ConcurrencyApiTest`, `DuplicateBookingUiTest` |
| BR-08 | No duplicate bookings | Two confirmed appointments may not overlap. The first release models **one shared resource**: an appointment occupies its interval for every service | `BookingServiceTest`, `ConcurrencyApiTest`, Postman *06 Conflict scenarios* |
| BR-09 | Cancellation | Only a `CONFIRMED` appointment can be cancelled; a second attempt returns `409 APPOINTMENT_ALREADY_CANCELLED` | `BookingServiceTest`, `CancellationApiTest`, Postman TC-012 |
| BR-10 | Slot release | Availability is computed from `CONFIRMED` appointments only, so cancelling releases the slot immediately | `BookingServiceTest`, `CancellationApiTest`, `CancelAppointmentUiTest`, Postman TC-014 |
| BR-11 | Unique identifier | Identifiers start at 1001 and are handed out by a synchronised sequence that never reuses a value, including after a restart | `AppointmentRepositoryTest`, `BookingApiTest` |
| BR-12 | Consistency | The interface renders a confirmation only when the API answered `201`; any other status produces an error message and no new row | `DuplicateBookingUiTest`, `BookingUiTest` |
| BR-13 | Time zone | All "current date" and "current time" decisions use the configured zone (`APPOINTMENTHUB_TIMEZONE`, default `UTC`); timestamps are stored in UTC | `BookingServiceTest` (fixed clock), `configuration.md` |

## Interpretation decisions taken for the first release

These points were ambiguous in the PDR and were resolved as follows. They are the
kind of decision that should be confirmed by the product owner before release.

1. **Single resource (BR-08).** The PDR speaks of "the same provider or resource".
   The first release models one provider, so an appointment blocks its interval
   for all services. Tests verify that another service cannot book the same slot.
2. **Availability for a past date** returns `200` with an empty `slots` array
   instead of an error: the request is valid, there is simply nothing to offer.
3. **Today with an elapsed start time** returns `409 SLOT_NOT_AVAILABLE`
   (not `400 PAST_DATE`): the date is valid, only the time has passed.
4. **Misaligned start times** (`10:15` with 30-minute slots) are rejected with
   `400 SLOT_NOT_ALIGNED`; times that do not fit inside business hours are
   rejected first with `400 OUTSIDE_BUSINESS_HOURS`.
5. **Email without a TLD** (`ana@com`) is rejected, because BR-02 requires a
   documented, verifiable format.
6. **Cancellation is final.** A cancelled appointment cannot be re-confirmed; the
   customer books a new appointment instead (state transition testing, PDR 13.4).
7. **No native confirmation dialog** before cancelling, so the journey stays
   accessible and the automation deterministic; the result of the action is
   reported in a message instead.
