# AppointmentHub — requirements traceability matrix

Version 1.0 · Updated after the execution recorded in the [test report](test-report.md)

## 1. Requirements to test cases and automation

| Requirement | Test cases | Component tests | API tests | Postman | UI tests | Status |
|---|---|---|---|---|---|---|
| FR-01 | TC-001 | `BookingServiceTest.servicesAreFilteredByActiveFlag` | `ServicesApiTest` | *00 Services* (3 requests) | `BrowseServicesUiTest` | Verified |
| FR-02 | TC-002, TC-014, TC-022 | `BookingServiceTest` (availability and boundary cases) | `AvailabilityApiTest` | *01 Availability* (8) | `DuplicateBookingUiTest` | Verified |
| FR-03 | TC-003, TC-019, TC-021, TC-022 | `BookingServiceTest.createReturnsConfirmedAppointmentWithUniqueIdentifier` | `BookingApiTest` | *02 Appointment creation* (2) | `BookAppointmentUiTest` | Verified |
| FR-04 | TC-003, TC-009 | `BookingServiceTest`, `AppointmentRepositoryTest.identifiersAreNotReusedAfterARestart` | `BookingApiTest.identifiersAreUnique` | *02 Appointment creation* | – | Verified |
| FR-05 | TC-009, TC-010 | `BookingServiceTest.retrievingUnknownAppointmentIsRejected` | `AppointmentRetrievalApiTest` | *03 Retrieval* (5) | – | Verified |
| FR-06 | TC-009, TC-015 | `BookingServiceTest.appointmentsCanBeFilteredByEmail` | `AppointmentRetrievalApiTest.registeredAppointmentsIncludeTheCreatedOne` | *03 Retrieval* | `ApiToUiConsistencyUiTest` | Verified |
| FR-07 | TC-011, TC-020 | `BookingServiceTest.cancellingReleasesTheSlot` | `CancellationApiTest` | *04 Cancellation* (6) | `CancelAppointmentUiTest` | Verified |
| FR-08 | TC-008, TC-014 | `BookingServiceTest.occupiedSlotIsRejected`, `overlappingAppointmentBlocksOverlappingSlots` | `ConcurrencyApiTest` | *06 Conflict scenarios* (6) | `DuplicateBookingUiTest` | Verified |
| FR-09 | TC-004, TC-005, TC-006, TC-017 | `BookingServiceTest` (missing, blank, email partitions) | `BookingApiTest` | *05 Negative validation* (15) | `BookingValidationUiTest` | Verified |
| FR-10 | TC-007 | `BookingServiceTest.pastDateIsRejected` and the current-date boundary cases | `BookingApiTest.bookingForPastDateIsRejected` | *05 Negative validation* | – | Verified |
| FR-11 | TC-011, TC-014 | `BookingServiceTest.cancellingReleasesTheSlot` | `CancellationApiTest.releasedSlotCanBeBookedAgain` | *04 Cancellation*, *07 Regression* | `CancelAppointmentUiTest` | Verified |
| FR-12 | TC-019, TC-020 | – | – | – | `BookAppointmentUiTest`, `BookingValidationUiTest` | Verified |
| FR-13 | TC-013 | `BookingServiceTest.cancellingUnknownAppointmentIsRejected` | `CancellationApiTest.cancellingNonexistentAppointmentReturnsNotFound` | *04 Cancellation* | – | Verified |
| FR-14 | TC-012 | `BookingServiceTest.cancellingTwiceIsRejected` | `CancellationApiTest.cancellingTwiceIsRejected` | *04 Cancellation* | `CancelAppointmentUiTest` | Verified |

## 2. Non-functional requirements to checks

| Requirement | Verification | Evidence |
|---|---|---|
| NFR-01 Usability | Manual exploratory session on the interface; UI journey tests assert the messages | `test-cases.md` (exploratory notes), `BookAppointmentUiTest` |
| NFR-02 Performance | 30 samples per endpoint, p95 compared with the 2 s target | `PerformanceApiTest`, `docs/evidence/performance/nfr-02-response-times.txt`, Newman summary (avg 42 ms) |
| NFR-03 Data integrity | Parallel requests for the same slot | `ConcurrencyApiTest`, `BookingServiceTest.concurrentBookingsForTheSameSlotProduceOneAppointment` |
| NFR-04 Consistency | API to UI and UI to API comparisons | `ApiToUiConsistencyUiTest`, Postman *03 Retrieval* and *04 Cancellation* |
| NFR-05 Maintainability | Review of names, page objects, helpers | Source review; `BaseUiTest`, `HomePage`, `TestData` |
| NFR-06 Compatibility | Full UI suite executed on the documented browser | `docs/evidence/ui` |
| NFR-07 Input validation | API called directly with invalid data while the interface is not involved | `BookingValidationUiTest.apiValidatesDataIndependentlyOfTheInterface`, Postman *05 Negative validation* |
| NFR-08 Error handling | Error structure, mapping and absence of internal details | `ErrorContractApiTest` |
| NFR-09 Data privacy | Review of the test data used | `TestData`, `test-cases.md` |
| NFR-10 Repeatability | Repeated executions with identical results | `docs/evidence/component-api`, `docs/evidence/ui` (two runs) |

## 3. Acceptance criteria to evidence

| Acceptance criterion | Test cases | Evidence |
|---|---|---|
| AC-01 | TC-001 | `ServicesApiTest`, `BrowseServicesUiTest` |
| AC-02 | TC-002 | `AvailabilityApiTest` |
| AC-03 | TC-003, TC-019 | `BookingApiTest`, `BookAppointmentUiTest` |
| AC-04 | TC-004, TC-005, TC-006, TC-017 | `BookingApiTest`, Postman *05* |
| AC-05 | TC-008 | `ConcurrencyApiTest`, Postman *06* |
| AC-06 | TC-007 | `BookingServiceTest`, `BookingApiTest` |
| AC-07 | TC-009 | `AppointmentRetrievalApiTest` |
| AC-08 | TC-011, TC-020 | `CancellationApiTest`, `CancelAppointmentUiTest` |
| AC-09 | TC-014 | `CancellationApiTest`, Postman *07* |
| AC-10 | TC-015, TC-019, TC-020 | `ApiToUiConsistencyUiTest` |
| AC-11 | TC-018, TC-010, TC-013 | `ErrorContractApiTest` |
| AC-12 | TC-019, TC-020, TC-003, TC-008 | UI and API suites |
| AC-13 | – | this matrix, [defect log](defect-log.md), [test report](test-report.md) |

## 4. Coverage

* Functional requirements with at least one automated check: **14 of 14 (100 %)**.
* Non-functional requirements with a documented check: **10 of 10 (100 %)**.
* Acceptance criteria with evidence: **13 of 13 (100 %)**.
* Test cases executed: **22 of 22 (100 %)**, all passed.
* Test cases automated (component, API, Postman or UI): **22 of 22 (100 %)**.
