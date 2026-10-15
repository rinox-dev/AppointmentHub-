# AppointmentHub — test report

**Version:** 1.0 · **Date of the recorded execution:** 2026-10-15 ·
**Application version:** 1.0.0 · **Overall status:** ✅ Exit criteria met

## 1. Summary

The complete test campaign was executed on AppointmentHub 1.0.0 in the local test
environment: 102 automated JUnit tests (43 component, 41 API, 18 UI) and the
Postman collection (54 requests, 137 assertions). **Every test passed.** Four
defects were found and fixed during the campaign — two in the product (one High)
and two in the testware — and no defect is open.

| Suite | Cases / requests | Passed | Failed | Blocked |
|---|---|---|---|---|
| Component tests (JUnit 5) | 43 | 43 | 0 | 0 |
| API tests (JUnit 5) | 41 | 41 | 0 | 0 |
| UI tests (Selenium) | 18 | 18 | 0 | 0 |
| Postman collection (Newman) | 54 requests / 137 assertions | 137 | 0 | 0 |
| **Total** | **156 automated checks** | **239** | **0** | **0** |

Manual test cases: 22 of 22 executed, 22 passed (see
[test cases](test-cases.md)); 22 of them are also covered by automation.

The whole suite was additionally executed with a single command
(`./mvnw test -Pall-tests`): **102 tests, 0 failures, 0 errors, BUILD SUCCESS**,
which confirms that the documented profile works for a reviewer who wants to
reproduce everything at once.

## 2. Environment

| Item | Value |
|---|---|
| Application | AppointmentHub 1.0.0 (`target/appointmenthub.jar`), in-memory storage for the runs |
| Runtime | OpenJDK 17.0.12 (Temurin) |
| Build | Maven 3.9.9 through `./mvnw` |
| Browser | Chromium 155.0.8059.39, headless, 1280×1024 |
| UI automation | Selenium WebDriver 4.25.0, JUnit Jupiter 5.10.2 |
| API tooling | Postman collection v2.1 executed with Newman 6.2.3 |
| Configuration | Time zone `UTC`, business hours 09:00–17:00, 30-minute slots, single shared resource |
| Data | Fictional customers, dates generated per run (90–290 days ahead) |

## 3. Test items and what was verified

* **Web interface** (`/`): service catalog, availability, booking form,
  appointment table, messages, cancellation.
* **REST API**: `GET /api/services`, `GET /api/availability`,
  `POST /api/appointments`, `GET /api/appointments`,
  `GET /api/appointments/{id}`, `DELETE /api/appointments/{id}` and the error
  contract.
* **Business rules** BR-01 to BR-13, enforced on the server.
* **Persistence**: identifier sequence and appointment data across a restart.
* **Concurrency**: parallel bookings of the same slot.

## 4. Results per test case

All 22 cases passed. The per-case results, evidence and exploratory notes are in
[test cases](test-cases.md); the CSV export is in
[`test-cases.csv`](test-cases.csv).

| Requirement group | Cases | Result |
|---|---|---|
| Services and availability (FR-01, FR-02) | TC-001, TC-002 | Passed |
| Booking (FR-03, FR-04, FR-08, FR-09, FR-10) | TC-003 … TC-008, TC-016, TC-017, TC-018, TC-021, TC-022 | Passed |
| Retrieval (FR-05, FR-06) | TC-009, TC-010, TC-015 | Passed |
| Cancellation (FR-07, FR-11, FR-13, FR-14) | TC-011, TC-012, TC-013, TC-014 | Passed |
| User journeys through the UI (FR-03, FR-07, FR-12) | TC-019, TC-020 | Passed |

## 5. Automated test inventory

| Class | Tests | Focus |
|---|---|---|
| `BookingServiceTest` | 39 | Validation order, email partitions, date/time boundaries, business hours, slot alignment, conflicts, cancellation states, concurrency |
| `AppointmentRepositoryTest` | 4 | Persistence, identifier reuse, ordering, in-memory mode |
| `ServicesApiTest` | 4 | Service contract, inactive services, method not allowed |
| `AvailabilityApiTest` | 7 | Slot generation, invalid parameters, unknown/inactive service, slot removal and release |
| `BookingApiTest` | 14 | Valid booking, required fields, email, past date, conflict, unknown/inactive service, formats, missing body |
| `AppointmentRetrievalApiTest` | 5 | Retrieval by id, list, filter, not found, invalid id |
| `CancellationApiTest` | 4 | Cancellation, release, double cancellation, unknown appointment |
| `ConcurrencyApiTest` | 1 | 10 parallel requests for the same slot |
| `PerformanceApiTest` | 3 | NFR-02 sampling for three endpoints |
| `ErrorContractApiTest` | 3 | Error structure, status mapping, no internal details |
| `BrowseServicesUiTest` | 3 | Service catalog in the browser |
| `BookAppointmentUiTest` | 3 | Booking journey, slot removal, form reuse |
| `BookingValidationUiTest` | 5 | Client validation, server validation when the interface is bypassed |
| `DuplicateBookingUiTest` | 2 | Conflict through the UI, BR-12 consistency |
| `CancelAppointmentUiTest` | 2 | Cancellation journey, disabled action afterwards |
| `ApiToUiConsistencyUiTest` | 3 | API → UI and UI → API consistency |
| **Total** | **102** | |

## 6. Non-functional results

### NFR-02 — Basic performance

Sample: 30 sequential requests per endpoint from the same machine as the
application, in-memory storage, single client. Threshold: 95 % of the samples
below 2000 ms.

| Operation | Samples | Average | p95 | Maximum | Result |
|---|---|---|---|---|---|
| `GET /api/services` | 30 | 41 ms | 42 ms | 42 ms | Pass |
| `GET /api/availability` | 30 | 41 ms | 42 ms | 43 ms | Pass |
| `POST /api/appointments` | 30 | 41 ms | 42 ms | 43 ms | Pass |

The Newman run reports an average response time of 42 ms over 54 requests
(minimum 18 ms, maximum 50 ms). Evidence:
`docs/evidence/performance/nfr-02-response-times.txt` and
`docs/evidence/api/newman-report.json`.

### NFR-03 — Data integrity under concurrency

* `BookingServiceTest` — 12 concurrent creations for one slot: exactly 1
  confirmed, 11 rejected with `SLOT_NOT_AVAILABLE`, 1 appointment stored.
* `ConcurrencyApiTest` — 10 concurrent HTTP requests: exactly 1 `201`, 9 `409`,
  1 appointment stored.

Both checks pass repeatedly, which is the evidence that duplicate bookings are
prevented (the highest risk of the project, R-01).

### NFR-04 / NFR-07 / NFR-08 — Consistency, independent validation, error contract

* `ApiToUiConsistencyUiTest` proves that API changes are visible in the interface
  and vice versa.
* `BookingValidationUiTest.apiValidatesDataIndependentlyOfTheInterface` proves
  that the API rejects invalid data without any involvement of the interface.
* `ErrorContractApiTest` proves that every error contains exactly `errorCode`,
  `message` and `status`, that the status matches the HTTP status code and that no
  stack trace, class name or package name is exposed.

### NFR-01 / NFR-06 / NFR-09 / NFR-10

Usability: the journey tests assert the user-visible messages, and the exploratory
session found no blocking usability problem. Compatibility: the full UI suite runs
on the documented browser (Chromium 155, headless and headed). Privacy: only
fictional data is used. Repeatability: the suites were executed twice with
identical results, and test data is generated per run.

## 7. Metrics

**Requirements coverage**

```text
Functional requirements with associated tests  : 14 / 14 = 100 %
Non-functional requirements with a check       : 10 / 10 = 100 %
Acceptance criteria with evidence              : 13 / 13 = 100 %
```

**Execution rate**

```text
Executed test cases            : 22 / 22 = 100 %
Executed automated checks      : 102 JUnit tests + 54 Postman requests
```

**Pass rate**

```text
Passed test cases              : 22 / 22 = 100 %
Passed automated checks        : 239 / 239 = 100 %
```

**Automation**

```text
Test cases automated           : 22 / 22 = 100 %
Automated checks selected      : 156 (102 JUnit + 54 Postman)
```

**Defects**

| Metric | Value |
|---|---|
| Defects found | 4 (2 product, 2 testware) |
| Critical / High / Medium / Low | 0 / 1 / 3 / 0 |
| Closed / Open / Reopened | 4 / 0 / 0 |
| Mean time to fix | Same session (practice project: fix and re-run immediately) |

A high pass rate alone would not be enough: the high-risk scenarios (duplicate
bookings, past dates, cancellation, slot release, API/UI consistency) are all
covered by executed and automated checks, which is what makes the result
meaningful.

## 8. Deviations and limitations

1. The performance figures describe one machine with in-memory storage and a
   single client; they are not a load test (risk R-08).
2. The UI suite was executed with one browser (Chromium 155); cross-browser
   behaviour is not covered.
3. Two PDR cases (TC-021, TC-022) were added to cover BR-03 and BR-05; the PDR
   case list was otherwise unchanged.
4. The Selenium suite requires a browser and a font configuration; in a container
   without fonts the renderer of Chromium aborts, which was observed and resolved
   during the campaign.
5. Cancellation has no confirmation dialog (documented decision, see
   `business-rules.md`), so no dialog handling is automated.

## 9. Remaining risks

See the [risk register](risks.md). The residual risks after the campaign are R-08
(representativeness of the performance sample), R-07 (scope growth if the project
is extended) and the absence of cross-browser coverage. None of them affects the
acceptance criteria of the first release.

## 10. Conclusion

AppointmentHub 1.0.0 satisfies the 13 acceptance criteria and the 14 functional
requirements of the PDR, with 100 % of the planned cases executed and passing, no
open defect and documented evidence for every claim. The most valuable outcome of
the campaign is not the pass rate but the two product defects that the automation
found before release — a blocked booking form (BUG-001) and a misleading
availability list (BUG-002) — and the proof that the API, not the interface,
protects the data.

Recommended next steps, in priority order: (1) confirm the interpretation
decisions in `business-rules.md` with the product owner, (2) extend compatibility
coverage to a second browser engine, (3) add a performance baseline run with
persistent storage, (4) implement the small usability improvements listed in
`defect-log.md` (button disabled while a request is in flight, client-side length
check).
