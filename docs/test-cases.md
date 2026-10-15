# AppointmentHub — manual and automated test cases

Version 1.0 · Execution: see the [test report](test-report.md)

## How to read this document

Each case documents its identifier, the requirement it covers, priority, the test
design technique used, preconditions, test data, steps, expected result, the
**actual result observed during execution**, the status and the evidence.

* **Status** values: `Passed`, `Failed`, `Blocked`, `Not executed`.
* **Evidence** names the automated test (JUnit or Postman request) and the raw
  report under `docs/evidence/`.
* Test data uses the fictional customers `Ana Example`, `Bruno Example` and
  `Carla Example`; dates are generated at run time so that the cases are
  repeatable (NFR-10).
* `TC-021` and `TC-022` extend the list in the PDR (section 12) to cover BR-03
  (inactive service) and BR-05 (business hours); the PDR identifiers of the
  original twenty cases are unchanged.

## Coverage summary

| Status | Cases |
|---|---|
| Passed | TC-001 … TC-022 (22 cases) |
| Failed | – |
| Blocked | – |
| Not executed | – |

Automation: 22 of 22 cases are covered by at least one automated check
(component test, API test, Postman request or Selenium test).

---

### TC-001 — Retrieve active services

**Requirement:** FR-01 · **Priority:** High · **Technique:** use case testing
**Preconditions:** Application running; catalog preconfigured.
**Test data:** none.
**Steps:**
1. Call `GET /api/services`.
2. Open the home page and wait for the catalog to render.
**Expected result:** `200` with the four active services (id, name, durationMinutes, active); the page lists exactly those four names and does not offer the inactive service.
**Actual result:** As expected. Four services returned; the page showed the same four; `Legacy Wellness Check` was absent.
**Status:** Passed · **Evidence:** `ServicesApiTest`, `BrowseServicesUiTest`, Postman *TC-001 Get active services*.

### TC-002 — Retrieve availability for a valid date

**Requirement:** FR-02 · **Priority:** High · **Technique:** use case testing, boundary value analysis
**Preconditions:** Application running; no appointment on the selected date.
**Test data:** `serviceId=1`, `date=<today+90..270>`.
**Steps:**
1. Call `GET /api/availability?serviceId=1&date={date}`.
2. Inspect every returned slot.
**Expected result:** `200` with aligned slots between 09:00 and 17:00, each 30 minutes long, matching the service duration.
**Actual result:** As expected. 16 slots from 09:00 to 16:30, each 30 minutes.
**Status:** Passed · **Evidence:** `AvailabilityApiTest`, Postman *TC-002 Get availability for a valid date*.

### TC-003 — Book an appointment with valid data

**Requirement:** FR-03, FR-04 · **Priority:** High · **Technique:** use case testing
**Preconditions:** Application running; the requested slot is free.
**Test data:** `Ana Example`, `ana@example.com`, service 1, future date, `10:00`.
**Steps:**
1. Call `POST /api/appointments` with the test data.
2. Read the identifier from the response.
3. Call `GET /api/appointments/{id}`.
**Expected result:** `201` with a unique identifier, `status=CONFIRMED`, `endTime=10:30` and a `createdAt` timestamp; step 3 returns the same appointment.
**Actual result:** As expected. Identifier 1001 returned and retrievable.
**Status:** Passed · **Evidence:** `BookingApiTest`, `BookAppointmentUiTest`, Postman *TC-003*.

### TC-004 — Book without a name

**Requirement:** FR-09 (BR-01) · **Priority:** High · **Technique:** equivalence partitioning
**Preconditions:** Application running.
**Test data:** `customerName` omitted, other fields valid.
**Steps:**
1. Call `POST /api/appointments` without `customerName`.
2. Call `GET /api/appointments` to check the list.
**Expected result:** `400` with `errorCode=VALIDATION_ERROR` and a message naming `customerName`; no appointment is created.
**Actual result:** As expected: `{"errorCode":"VALIDATION_ERROR","message":"customerName is required.","status":400}` and the list was unchanged.
**Status:** Passed · **Evidence:** `BookingApiTest.bookingWithoutNameIsRejected`, `BookingServiceTest`, Postman *TC-004*; UI equivalent in `BookingValidationUiTest`.

### TC-005 — Book without an email

**Requirement:** FR-09 (BR-01) · **Priority:** High · **Technique:** equivalence partitioning
**Preconditions:** Application running.
**Test data:** `customerEmail` omitted (and, in the UI case, left empty).
**Steps:**
1. Call `POST /api/appointments` without `customerEmail`.
2. Attempt the same through the web form with an empty email field.
**Expected result:** `400 VALIDATION_ERROR` naming `customerEmail`; the interface shows a message and creates nothing.
**Actual result:** As expected. The UI displayed “Please enter the customer email.” and the appointment list stayed unchanged.
**Status:** Passed · **Evidence:** `BookingApiTest`, `BookingValidationUiTest`, Postman *TC-005*.

### TC-006 — Book with a malformed email

**Requirement:** FR-09 (BR-02) · **Priority:** High · **Technique:** equivalence partitioning
**Preconditions:** Application running.
**Test data:** `ana.example.com`, `ana@`, `@example.com`, `ana@com`, `ana @example.com`.
**Steps:**
1. Call `POST /api/appointments` for each invalid email.
2. Repeat once through the web form.
**Expected result:** Every invalid value is rejected with `400 VALIDATION_ERROR`; no appointment is created.
**Actual result:** As expected for all five partitions; the UI reported “Please enter a valid email address…”.
**Status:** Passed · **Evidence:** `BookingApiTest.bookingWithMalformedEmailIsRejected`, `BookingServiceTest` (parameterised), `BookingValidationUiTest`, Postman *TC-006*.

### TC-007 — Book for a past date

**Requirement:** FR-10 (BR-04) · **Priority:** High · **Technique:** boundary value analysis
**Preconditions:** Application running.
**Test data:** `appointmentDate = yesterday`; also `today` and `today+1` for the boundary.
**Steps:**
1. Call `POST /api/appointments` with yesterday's date.
2. Repeat with the current date and with tomorrow's date.
**Expected result:** Yesterday → `400 PAST_DATE`; today → accepted while the slot has not started, otherwise `409 SLOT_NOT_AVAILABLE` (never `PAST_DATE`); tomorrow → accepted.
**Actual result:** As expected: yesterday returned `400 PAST_DATE` with the date in the message; the boundary cases behaved as documented (`BookingServiceTest.currentDateIsAcceptedForAFutureSlot` / `currentDateIsRejectedForAnElapsedSlot`).
**Status:** Passed · **Evidence:** `BookingApiTest.bookingForPastDateIsRejected`, `BookingServiceTest`, Postman *TC-007*.

### TC-008 — Book an occupied time slot

**Requirement:** FR-08 (BR-07, BR-08) · **Priority:** High · **Technique:** decision table, state transition
**Preconditions:** One confirmed appointment for the slot; a second customer tries the same slot.
**Test data:** slot `10:00` on a future date; second request from `Bruno Example` for the same slot, also with another service.
**Steps:**
1. Create the first appointment.
2. Attempt the same slot with the same and with another service.
3. Retrieve the appointment list and the availability.
**Expected result:** `409 SLOT_NOT_AVAILABLE`, no second appointment, the original stays `CONFIRMED` and the slot is no longer offered.
**Actual result:** As expected on all four checks. Ten parallel requests for the same slot produced exactly one appointment and nine conflicts.
**Status:** Passed · **Evidence:** `BookingApiTest.bookingOccupiedSlotIsRejected`, `ConcurrencyApiTest`, `DuplicateBookingUiTest`, Postman *06 Conflict scenarios*.

### TC-009 — Retrieve an existing appointment

**Requirement:** FR-05, FR-06 · **Priority:** High · **Technique:** use case testing
**Preconditions:** An appointment exists.
**Test data:** the identifier returned by TC-003.
**Steps:**
1. Call `GET /api/appointments/{id}`.
2. Call `GET /api/appointments`.
**Expected result:** `200` with all nine fields matching the created appointment; the list contains the same identifier.
**Actual result:** As expected; the retrieved object was identical to the created one.
**Status:** Passed · **Evidence:** `AppointmentRetrievalApiTest`, `BookingApiTest.createdAppointmentCanBeRetrieved`, Postman *TC-009*.

### TC-010 — Retrieve a nonexistent appointment

**Requirement:** FR-05 · **Priority:** Medium · **Technique:** equivalence partitioning
**Preconditions:** Application running.
**Test data:** identifier `987654`, and the non-numeric value `not-a-number`.
**Steps:**
1. Call `GET /api/appointments/987654`.
2. Call `GET /api/appointments/not-a-number`.
**Expected result:** `404 APPOINTMENT_NOT_FOUND` for the unknown identifier; `400 VALIDATION_ERROR` for the non-numeric value; no internal details in the messages.
**Actual result:** As expected.
**Status:** Passed · **Evidence:** `AppointmentRetrievalApiTest`, Postman *TC-010*.

### TC-011 — Cancel a confirmed appointment

**Requirement:** FR-07, FR-11 · **Priority:** High · **Technique:** state transition testing
**Preconditions:** A confirmed appointment exists.
**Test data:** the identifier of that appointment.
**Steps:**
1. Call `DELETE /api/appointments/{id}`.
2. Call `GET /api/appointments/{id}`.
3. Call `GET /api/availability` for the same service and date.
**Expected result:** `200` with `status=CANCELLED`; the stored appointment is cancelled; the slot is offered again.
**Actual result:** As expected. The UI journey (click *Cancel*) produced the same result and displayed a cancellation confirmation.
**Status:** Passed · **Evidence:** `CancellationApiTest`, `CancelAppointmentUiTest`, Postman *TC-011*.

### TC-012 — Cancel an already canceled appointment

**Requirement:** FR-14 (BR-09) · **Priority:** Medium · **Technique:** state transition testing, decision table
**Preconditions:** A cancelled appointment exists.
**Test data:** the identifier of the cancelled appointment.
**Steps:**
1. Call `DELETE /api/appointments/{id}` a second time.
2. Call `GET /api/appointments/{id}`.
**Expected result:** `409 APPOINTMENT_ALREADY_CANCELLED`; the status remains `CANCELLED`; in the interface the cancel action is disabled.
**Actual result:** As expected.
**Status:** Passed · **Evidence:** `CancellationApiTest.cancellingTwiceIsRejected`, `CancelAppointmentUiTest.cancelIsDisabledAfterCancellation`, Postman *TC-012*.

### TC-013 — Cancel a nonexistent appointment

**Requirement:** FR-13 · **Priority:** Medium · **Technique:** equivalence partitioning
**Preconditions:** Application running.
**Test data:** identifier `987654`.
**Steps:**
1. Call `DELETE /api/appointments/987654`.
**Expected result:** `404 APPOINTMENT_NOT_FOUND` with a controlled message.
**Actual result:** As expected: `{"errorCode":"APPOINTMENT_NOT_FOUND","message":"Appointment 987654 does not exist.","status":404}`.
**Status:** Passed · **Evidence:** `CancellationApiTest`, `BookingServiceTest.cancellingUnknownAppointmentIsRejected`, Postman *TC-013*.

### TC-014 — Book a previously released time slot

**Requirement:** FR-02, FR-08, FR-11 · **Priority:** High · **Technique:** use case testing
**Preconditions:** A confirmed appointment was cancelled.
**Test data:** the released date and start time.
**Steps:**
1. Confirm the slot is not offered while the appointment is confirmed.
2. Cancel the appointment.
3. Confirm the slot is offered again.
4. Book the slot for another customer.
**Expected result:** Steps 1 and 3 show the expected availability; step 4 returns `201` with a new identifier and `status=CONFIRMED`.
**Actual result:** As expected. The rebooking received a new identifier; the cancelled appointment kept `CANCELLED`.
**Status:** Passed · **Evidence:** `AvailabilityApiTest.releasedSlotIsOfferedAgain`, `CancellationApiTest.releasedSlotCanBeBookedAgain`, `CancelAppointmentUiTest`, Postman *TC-014*.

### TC-015 — Create an appointment through the API and view it on the website

**Requirement:** FR-06, NFR-04 · **Priority:** High · **Technique:** integration / use case testing
**Preconditions:** Application running with the web interface.
**Test data:** appointment created through the API for `Bruno Example`.
**Steps:**
1. Create the appointment through the API.
2. Open the web interface.
3. Compare the row with the API response.
4. Cancel through the API and refresh the list.
**Expected result:** The row shows the same identifier, customer, date, time and `CONFIRMED` status; after the API cancellation the row shows `CANCELLED`.
**Actual result:** As expected; the interface showed the API data without a page reload (only the list refresh).
**Status:** Passed · **Evidence:** `ApiToUiConsistencyUiTest`, Postman retrieval folder.

### TC-016 — Submit a nonexistent service

**Requirement:** FR-03 (BR-03) · **Priority:** High · **Technique:** equivalence partitioning
**Preconditions:** Application running.
**Test data:** `serviceId=99` for both booking and availability.
**Steps:**
1. Call `POST /api/appointments` with `serviceId=99`.
2. Call `GET /api/availability?serviceId=99&date=…`.
**Expected result:** `404 SERVICE_NOT_FOUND` in both cases; nothing is created.
**Actual result:** As expected.
**Status:** Passed · **Evidence:** `BookingApiTest.bookingWithUnknownServiceIsRejected`, `AvailabilityApiTest`, Postman *TC-016*.

### TC-017 — Submit a date in an invalid format

**Requirement:** FR-09 · **Priority:** Medium · **Technique:** equivalence partitioning
**Preconditions:** Application running.
**Test data:** `appointmentDate=10-02-2027`; `startTime=10h00` for the time equivalent.
**Steps:**
1. Call `POST /api/appointments` with the invalid date.
2. Repeat with the invalid time.
**Expected result:** `400 INVALID_DATE_FORMAT` and `400 INVALID_TIME_FORMAT` with controlled messages that do not expose internals.
**Actual result:** As expected: “…must use the format yyyy-MM-dd (received '10-02-2027').”
**Status:** Passed · **Evidence:** `BookingApiTest`, Postman *TC-017*.

### TC-018 — Submit a request without a JSON body

**Requirement:** NFR-08 · **Priority:** Medium · **Technique:** error guessing, negative testing
**Preconditions:** Application running.
**Test data:** no body; malformed JSON (`{"customerName":`); wrong data type (`serviceId: "abc"`).
**Steps:**
1. Call `POST /api/appointments` without a body.
2. Repeat with malformed JSON and with a wrong type.
**Expected result:** `400` with `MISSING_BODY` and `MALFORMED_REQUEST` respectively; the server stays available.
**Actual result:** As expected; the process did not log an unhandled exception and the following requests worked normally.
**Status:** Passed · **Evidence:** `BookingApiTest.requestWithoutBodyIsRejected`, Postman *TC-018*.

### TC-019 — Confirm a booking through the UI

**Requirement:** FR-03, FR-12 · **Priority:** High · **Technique:** use case testing
**Preconditions:** Web interface reachable; a free slot on a future date.
**Test data:** service *General Consultation*, future date, first available slot, `Ana Example`, `ana@example.com`.
**Steps:**
1. Select the service.
2. Set the date and check availability.
3. Select the time slot.
4. Enter the name and the email.
5. Confirm the booking.
**Expected result:** A success message containing the appointment identifier, date and time; the appointment appears in the table with `CONFIRMED`; the slot is no longer offered; the appointment exists on the server.
**Actual result:** As expected. The message read “Appointment 1001 confirmed for … at … (General Consultation).”
**Status:** Passed · **Evidence:** `BookAppointmentUiTest.bookingThroughTheInterfaceIsConfirmed`.

### TC-020 — Cancel through the UI

**Requirement:** FR-07, FR-11, FR-12 · **Priority:** High · **Technique:** use case testing
**Preconditions:** A confirmed appointment exists.
**Test data:** the identifier of that appointment.
**Steps:**
1. Open the web interface.
2. Locate the appointment row.
3. Press *Cancel*.
4. Check the status column and the availability of the slot.
**Expected result:** The status becomes `CANCELLED`, a cancellation message is displayed, the server state matches and the slot is offered again.
**Actual result:** As expected.
**Status:** Passed · **Evidence:** `CancelAppointmentUiTest.cancellingThroughTheInterfaceReleasesTheSlot`.

### TC-021 — Book an inactive service (extension of the PDR case list)

**Requirement:** FR-03 (BR-03) · **Priority:** Medium · **Technique:** equivalence partitioning
**Preconditions:** Application running; the catalog contains the inactive service 5.
**Test data:** `serviceId=5`.
**Steps:**
1. Call `POST /api/appointments` with `serviceId=5`.
2. Call `GET /api/availability?serviceId=5&date=…`.
3. Check that the web interface does not offer the service.
**Expected result:** `409 SERVICE_INACTIVE` in both API calls; the service is not listed in the interface.
**Actual result:** As expected: `Service 5 (Legacy Wellness Check) is not active and cannot be booked.`
**Status:** Passed · **Evidence:** `BookingApiTest.bookingWithInactiveServiceIsRejected`, `BrowseServicesUiTest.inactiveServicesAreNotDisplayed`, Postman *TC-021*.

### TC-022 — Book outside business hours (extension of the PDR case list)

**Requirement:** FR-03 (BR-05, BR-06) · **Priority:** Medium · **Technique:** boundary value analysis
**Preconditions:** Application running with business hours 09:00–17:00.
**Test data:** `startTime=07:00` (before opening), `17:00` (at closing), `16:30` for a 60-minute service (ends after closing), `10:15` (not aligned).
**Steps:**
1. Call `POST /api/appointments` for each value.
2. Compare the returned error codes.
**Expected result:** The three out-of-hours values return `400 OUTSIDE_BUSINESS_HOURS`; `10:15` returns `400 SLOT_NOT_ALIGNED`; `16:30` is accepted for a 30-minute service because it ends exactly at 17:00.
**Actual result:** As expected on all five values.
**Status:** Passed · **Evidence:** `BookingApiTest.bookingOutsideBusinessHoursIsRejected`, `BookingServiceTest` (`outsideBusinessHoursIsRejected`, `lastSlotDependsOnServiceDuration`, `misalignedStartTimeIsRejected`), Postman *TC-022*.

---

## Exploratory testing notes

Two short time-boxed sessions (20 minutes each) were executed on the web
interface, following the charter *"explore the booking and cancellation journeys
looking for inconsistencies between what the screen shows and what the API
stores"*. Findings:

1. **Refreshing the page after cancelling** keeps the cancelled appointment in the
   table with the `CANCELLED` status and offers the slot again — consistent.
2. **Submitting the form twice quickly** (double click) results in one confirmation
   and one conflict message; only one appointment exists. The interface does not
   disable the button while the request is in flight, which is cosmetic but could
   be improved (recorded as an observation, not a defect).
3. **Entering a very long name** (200 characters) is rejected by the API with
   `400 VALIDATION_ERROR`, but the interface only reports the server message after
   the request; a client-side length check would be friendlier. Observation.
4. **Leaving the page open past midnight** would leave the date field on yesterday:
   the API then rejects the booking with `400 PAST_DATE`. Acceptable behaviour, and
   it confirms that the server is the source of truth.
