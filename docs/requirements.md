# AppointmentHub — requirements

Version 1.0 · Each requirement has a unique identifier, a priority and a way to be
verified. Identifiers are stable and are used by the
[traceability matrix](traceability-matrix.md).

## 1. Functional requirements

| ID | Requirement | Priority | Verification | Acceptance criteria |
|---|---|---|---|---|
| FR-01 | The system shall display active services. | High | API test, UI test, Postman | `GET /api/services` returns the four active services with id, name, duration and active flag; the home page lists exactly those services |
| FR-02 | The system shall allow users to retrieve available time slots by service and date. | High | API test, component test, Postman | `GET /api/availability` returns aligned slots inside business hours, excluding occupied and elapsed slots |
| FR-03 | The system shall allow users to book an appointment using valid data. | High | API test, UI test, Postman | A valid request creates the appointment and returns `201` with the booking details |
| FR-04 | The system shall generate a unique identifier for every appointment. | High | API test, component test | Identifiers are unique, start at 1001 and are never reused, also after a restart |
| FR-05 | The system shall display the details of an existing appointment. | High | API test, Postman | `GET /api/appointments/{id}` returns the nine entity fields with the stored values |
| FR-06 | The system shall allow users to retrieve registered appointments. | High | API test, UI test, Postman | The list contains every created appointment, most recent first, and is shown in the interface |
| FR-07 | The system shall allow users to cancel a confirmed appointment. | High | API test, UI test, Postman | `DELETE` changes the status to `CANCELLED` and returns `200` with the updated appointment |
| FR-08 | The system shall prevent booking an occupied time slot. | High | API test, component test, UI test, Postman | A second booking of the same slot returns `409 SLOT_NOT_AVAILABLE` and creates nothing |
| FR-09 | The system shall reject bookings with missing or invalid required data. | High | API test, component test, UI test, Postman | Each missing or malformed field is rejected with `400` and a message naming the field |
| FR-10 | The system shall reject bookings for past dates. | High | API test, component test, Postman | A date before the current date returns `400 PAST_DATE` and stores nothing |
| FR-11 | The system shall release the time slot when an appointment is cancelled. | High | API test, component test, UI test, Postman | After cancellation the slot appears in availability again and can be booked |
| FR-12 | The system shall inform users of the result of each operation. | Medium | UI test | Every booking and cancellation shows a success or error message that includes the identifier or the reason |
| FR-13 | The system shall reject cancellation requests for nonexistent appointments. | Medium | API test, component test, Postman | `DELETE` on an unknown identifier returns `404 APPOINTMENT_NOT_FOUND` |
| FR-14 | The system shall prevent an appointment from being canceled more than once. | Medium | API test, component test, Postman | The second cancellation returns `409 APPOINTMENT_ALREADY_CANCELLED` and the status stays `CANCELLED` |

## 2. Non-functional requirements

| ID | Requirement | Verification criterion | Result |
|---|---|---|---|
| NFR-01 | Usability | A user completes booking and cancellation without external instructions; messages are understandable | Verified by an exploratory session on the web interface and by the UI journey tests; messages name the field or the reason |
| NFR-02 | Basic performance | Routine API operations answer within 2 s for at least 95 % of the sample | Verified: 30 samples per endpoint, p95 = 42 ms (see `docs/evidence/performance`) |
| NFR-03 | Data integrity | Two confirmed appointments never occupy the same interval | Verified by concurrency tests (10 parallel requests → 1 created, 9 rejected) at service and HTTP level |
| NFR-04 | Consistency | An appointment created or cancelled through the API is reflected in the interface | Verified by `ApiToUiConsistencyUiTest` and the Postman persistence checks |
| NFR-05 | Maintainability | Automated tests use descriptive names and avoid duplication | Verified by review: page objects, test data helper, shared API client, `@DisplayName` with requirement ids |
| NFR-06 | Compatibility | Core journeys work in the documented browser | Verified with Chromium 155 (headless and headed) in the test environment |
| NFR-07 | Input validation | The API validates data even when the interface is bypassed | Verified by `apiValidatesDataIndependentlyOfTheInterface` and the Postman negative folder |
| NFR-08 | Error handling | Consistent HTTP status and messages without internal details | Verified by `ErrorContractApiTest` (structure, mapping, no stack traces) |
| NFR-09 | Data privacy | Tests use fictional data only | Verified by review: `Ana Example`, `ana@example.com`, no real personal data |
| NFR-10 | Repeatability | Test cases document preconditions and results are reproducible | Verified by re-running both suites with identical results; test data is generated per run |

## 3. Product acceptance criteria

| ID | Acceptance criterion | Status | Evidence |
|---|---|---|---|
| AC-01 | Users can retrieve active services | Met | TC-001, `ServicesApiTest`, `BrowseServicesUiTest` |
| AC-02 | Users can retrieve available time slots for a valid service and date | Met | TC-002, `AvailabilityApiTest` |
| AC-03 | A valid booking creates an appointment with a unique identifier | Met | TC-003, `BookingApiTest`, `BookAppointmentUiTest` |
| AC-04 | Missing or invalid required data is rejected | Met | TC-004 to TC-006, TC-017, negative Postman folder |
| AC-05 | Two conflicting appointments cannot both be confirmed for the same slot | Met | TC-008, `ConcurrencyApiTest`, conflict Postman folder |
| AC-06 | Bookings for past dates are rejected | Met | TC-007, `BookingServiceTest` (BVA) |
| AC-07 | An existing appointment can be retrieved using its identifier | Met | TC-009, `AppointmentRetrievalApiTest` |
| AC-08 | A confirmed appointment can be canceled | Met | TC-011, `CancellationApiTest`, `CancelAppointmentUiTest` |
| AC-09 | A canceled appointment no longer occupies its time slot | Met | TC-014, `CancellationApiTest`, Postman regression folder |
| AC-10 | The interface reflects the results returned by the API | Met | TC-015, TC-019, TC-020, `ApiToUiConsistencyUiTest` |
| AC-11 | Errors are communicated through consistent HTTP responses and messages | Met | `ErrorContractApiTest`, Postman error assertions |
| AC-12 | Critical workflows can be verified through API tests and UI automation | Met | 84 component/API tests + 18 UI tests, all passing |
| AC-13 | Test results and defects are documented and linked to requirements | Met | [test report](test-report.md), [defect log](defect-log.md), [traceability matrix](traceability-matrix.md) |

## 4. Out of scope (first release)

Online payments, email or SMS reminders, calendar integrations, video
conferencing, mobile applications, advanced role management, recurring
appointments, multiple branches, third-party integrations, load and security
testing. See section 3.2 of the [PDR](PDR.md).
