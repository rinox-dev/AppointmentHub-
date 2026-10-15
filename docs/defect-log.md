# AppointmentHub — defect log

Version 1.0 · Status at the end of the test campaign

## 1. Lifecycle used

```text
New -> Under Analysis -> Assigned -> Fixed -> Pending Verification -> Closed
                                                |
                                                +-> Reopened (if verification fails)
```

## 2. Severity levels

| Severity | Definition |
|---|---|
| Critical | Prevents operation or compromises data integrity (for example two confirmed appointments in the same slot) |
| High | Breaks a core function without a workable alternative |
| Medium | Partially affects a feature, a workaround exists |
| Low | Cosmetic or tooling issue with little impact |

## 3. Defects found during this campaign

> These defects were really observed while building and executing the test
> campaign. They are the strongest evidence that the tests add value: the first
> two were found by the Selenium suite, the last two by the API automation.

### BUG-001 — The booking form never initialised, so no service was displayed

| Field | Value |
|---|---|
| Defect identifier | BUG-001 |
| Title | The service list stays on "Loading services..." because the page script aborts during start-up |
| Related requirement | FR-01, FR-12, AC-01, AC-10 |
| Related test case | TC-001, TC-019, UI-001 |
| Execution environment | Chromium 155 headless, local test environment, application started with in-memory storage |
| Preconditions | Application running; open the home page |
| Reproduction steps | 1. Open `http://localhost:PORT/`. 2. Wait for the page. 3. Look at the service area. |
| Expected result | The four active services are listed and the booking form works |
| Actual result | The page showed "Loading services..." forever; no service, slot or appointment was ever rendered. The browser reported `TypeError: Cannot read properties of null (reading 'addEventListener')` because `init()` looked up the form through a `data-testid` selector that the form element did not have |
| Severity | High (the whole user journey was blocked; no workaround for the customer) |
| Priority | High |
| Evidence | Failure screenshot `target/screenshots/UI-001-TC-001-the-home-page-lists-the-active-services-returned-by-the-API.png` captured automatically by the test watcher, plus the surefire report of `BrowseServicesUiTest` |
| Status | Closed (fixed, verified) |
| Fix | The `data-testid="booking-form"` attribute was added to the form element |
| Verification | `BrowseServicesUiTest`, `BookAppointmentUiTest` and the whole UI suite passed on two consecutive runs |

### BUG-002 — No loading state for the availability request

| Field | Value |
|---|---|
| Defect identifier | BUG-002 |
| Title | A stale slot list stays visible while a new availability request is in flight |
| Related requirement | FR-02, NFR-01, NFR-04 |
| Related test case | TC-002, TC-008, UI-004 |
| Execution environment | Chromium 155 headless, local test environment |
| Preconditions | A slot is booked by another customer while a customer keeps the page open |
| Reproduction steps | 1. Select a service and a date. 2. Book one of the offered slots through the API (simulating another customer). 3. Press "Check availability" again. 4. Inspect the list while the request is in flight. |
| Expected result | The list is cleared while the new data is loading |
| Actual result | The previous list stayed visible and clickable until the response arrived, so a slot that no longer existed could still be selected, and the interface gave no feedback that a refresh was running |
| Severity | Medium (misleading information; the server still rejected the booking, so no data corruption) |
| Priority | Medium |
| Evidence | Surefire report of `DuplicateBookingUiTest.unavailableSlotIsNotOffered` (assertion "the slot is no longer offered") |
| Status | Closed (fixed, verified) |
| Fix | The slot area is cleared immediately and shows a `data-testid="slots-loading"` hint until the response is rendered |
| Verification | `DuplicateBookingUiTest` and the rest of the UI suite pass; the tests now wait for the loading hint to disappear instead of guessing |

### BUG-003 — Postman assertion for the content type was wrong (testware)

| Field | Value |
|---|---|
| Defect identifier | BUG-003 |
| Title | "Response is JSON" failed on every request that returned `application/json; charset=utf-8` |
| Related requirement | NFR-08 |
| Related test case | TC-001, TC-002, TC-003, TC-009, TC-011 |
| Execution environment | Newman 6.2.3 against the local application |
| Preconditions | Run the collection |
| Reproduction steps | 1. Run the collection with Newman. 2. Inspect the failures. |
| Expected result | All assertions pass |
| Actual result | Six assertions failed: `expected 'Content-Type' response header to be /application\/json/json but got 'application/json; charset=utf-8'`. The assertion passed a regular expression to `to.have.header()`, which Postman converts into a literal string |
| Severity | Low (testware defect, no impact on the product) |
| Priority | Medium (false failures hide real ones) |
| Evidence | `docs/evidence/api/newman-console.txt` (first run) and `newman-report.json` |
| Status | Closed (fixed, verified) |
| Fix | The assertion reads the header and checks that it contains `application/json` |
| Verification | Re-run: 54 requests, 137 assertions, 0 failures |

### BUG-004 — Selenium helpers read elements from a list that was re-rendered (testware)

| Field | Value |
|---|---|
| Defect identifier | BUG-004 |
| Title | Intermittent `StaleElementReferenceException` and waits that never observed the new text |
| Related requirement | NFR-05, NFR-10 |
| Related test case | UI-001 to UI-005 |
| Execution environment | Chromium 155 headless, local test environment |
| Preconditions | Run the UI suite |
| Reproduction steps | 1. Run `tools/run-ui-tests.sh`. 2. Observe intermittent failures in `DuplicateBookingUiTest` and `CancelAppointmentUiTest`. |
| Expected result | Stable results on every run |
| Actual result | Two unstable patterns: (a) reading `data-start-time` from a list that the page replaced between `findElements` and `getAttribute`; (b) `waitForStatus` captured the status element once, so after the table re-render the captured element became stale and the wait timed out although the text had already changed |
| Severity | Low (testware defect, no impact on the product) |
| Priority | High (unstable automation destroys trust in the results, risk R-04) |
| Evidence | Surefire reports of the failing runs; the fixed runs are recorded in `docs/evidence/ui` |
| Status | Closed (fixed, verified) |
| Fix | Slot and service values are read inside the browser in one step, waits re-locate their element on every poll, and clicks retry once after a re-render |
| Verification | Three consecutive green UI runs, including the repeat run recorded in `docs/evidence/ui` |

## 4. Simulated example (not an observation)

The PDR (section 16.4) asks for one clearly labelled example defect so the defect
template can be demonstrated.

> **Simulated example — not found during this campaign.**
>
> **ID:** BUG-999 (simulated) · **Title:** The API allows two confirmed
> appointments for the same time slot.
> **Severity if it occurred:** Critical. **Status:** Not applicable.
>
> The behaviour was explicitly tested and **not** reproduced: the concurrency
> checks (`ConcurrencyApiTest`, `BookingServiceTest.concurrentBookingsForTheSameSlotProduceOneAppointment`)
> send 10 to 12 parallel requests for the same slot and always obtain exactly one
> appointment and the expected conflicts. The entry is kept only as an example of
> the defect template.

## 5. Observations that were not raised as defects

| # | Observation | Decision |
|---|---|---|
| O-1 | The "Confirm booking" button is not disabled while the request is in flight; a fast double click produces one confirmation and one conflict message | Accepted for the first release: no data problem, cosmetic only. Candidate for a future improvement |
| O-2 | The name length rule (2–100 characters) is only enforced by the API; the interface reports the server message | Accepted: the server is the source of truth (NFR-07) |
| O-3 | No pagination or filtering of the appointment list in the interface | Out of scope for the first release |

## 6. Defect metrics

| Metric | Value |
|---|---|
| Defects found and closed | 4 (2 product, 2 testware) |
| Open defects at the end of the campaign | 0 |
| Critical defects found | 0 |
| High severity defects found | 1 (BUG-001) |
| Defects found by UI automation | 2 (BUG-001, BUG-002) |
| Defects found by API automation | 2 (BUG-003, BUG-004) |
| Defects verified as fixed | 4 |
| Reopened defects | 0 |
