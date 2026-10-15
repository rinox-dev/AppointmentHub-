# AppointmentHub — test plan

Version 1.0 · Aligned with the [PDR](PDR.md) section 11 and ISTQB CTFL

## 1. Scope

**In scope:** services catalog, availability, booking, retrieval and cancellation
through the REST API and the web interface; the business rules BR-01 to BR-13; the
functional requirements FR-01 to FR-14; the non-functional requirements NFR-01 to
NFR-10 as far as they can be verified in a local test environment.

**Out of scope:** payments, notifications, calendar integrations, mobile clients,
advanced roles, recurring appointments, multiple branches, load and security
testing (see PDR 3.2).

## 2. Objectives

1. Prove that the booking, retrieval and cancellation workflows satisfy the
   requirements and the acceptance criteria.
2. Prove that the critical business rules are enforced by the API itself and not
   only by the interface.
3. Provide stable, repeatable automation for the critical journeys.
4. Find and document defects with enough detail to reproduce them.
5. Report coverage, results and residual risks with evidence.

## 3. Test approach

| Level | Scope | Technique | Where |
|---|---|---|---|
| Component | Business rules and storage in isolation, with a fixed clock | Equivalence partitioning, boundary value analysis, decision tables, state transition, concurrency | `src/test/java/.../component` (JUnit 5, tag `component`) |
| Integration | Web interface ↔ REST API ↔ storage, and the API contract itself | Use case testing, contract checks, negative testing | `src/test/java/.../api` (JUnit 5, tag `api`) and the Postman collection |
| System | The complete application through the browser | Use case testing, exploratory testing | `src/test/java/.../ui` (Selenium, tag `ui`) |
| Acceptance | The business workflows of the PDR (section 4) | Use case testing against the acceptance criteria | Manual session + Postman *07 Regression* folder |

Test types: functional, basic non-functional (performance, usability,
compatibility), negative/input validation, confirmation testing after fixes and
regression testing.

## 4. Test design techniques applied

| Technique | Where it is applied | Example |
|---|---|---|
| Equivalence partitioning | Email, name, service id, date and time fields | valid email / malformed email / missing email (`BookingServiceTest.malformedEmailsAreRejected`) |
| Boundary value analysis | Booking date and start time | yesterday, today, tomorrow; 08:30, 09:00, 16:30, 17:00 (`BookingServiceTest`) |
| Decision table | Cancellation and conflict combinations | appointment exists/not × confirmed/cancelled × cancel/retrieve (TC-008, TC-012) |
| State transition | Appointment status | `CONFIRMED -> CANCELLED` allowed, reverse and repeated transitions rejected |
| Use case testing | Booking and cancellation journeys | TC-019, TC-020 through the browser |
| Exploratory testing | Interface behaviour not covered by cases | time-boxed sessions recorded in `test-cases.md` |

## 5. Environments

| Item | Value |
|---|---|
| Application | AppointmentHub 1.0.0, JDK 17, embedded HTTP server, in-memory storage during test runs |
| Time zone | `UTC`, business hours 09:00–17:00, 30-minute slots (the defaults) |
| API tooling | Postman collection v2.1, executed with Newman 6.2.3 |
| UI tooling | Selenium WebDriver 4.25, JUnit 5.10, Chromium 155, headless (1280×1024) |
| Build tooling | Maven 3.9.9 through the Maven wrapper |
| Data | Fictional customers only; every test generates its own far future date |

The UI suite starts its own application instance on a free port with a clean
store, so a developer machine needs no manual preparation and the suites cannot
interfere with a running instance.

## 6. Entry criteria

* Requirements, business rules and acceptance criteria are documented.
* The API contract and the error codes are documented.
* The application builds and starts.
* Test data and test cases with expected results exist.
* The browser and the tooling versions are documented.

All entry criteria were met before the execution recorded in the
[test report](test-report.md).

## 7. Exit criteria

* All high-priority test cases executed: **met** (22 of 22 cases executed).
* No open critical or high-severity defects: **met** (0 open defects).
* Booking and cancellation workflows work: **met**.
* Regression pack executed: **met** (Postman *07 Regression* + full suites re-run).
* Traceability matrix updated: **met**.
* Remaining limitations and risks reported: **met** ([risks](risks.md)).

## 8. How to run the campaign

```bash
./mvnw test                     # component + API tests (84)
tools/run-ui-tests.sh           # UI tests (18)
tools/run-api-tests.sh          # Postman collection through Newman (54 requests)
./mvnw test -Pall-tests         # everything that does not need a browser + UI
```

Evidence is written to `docs/evidence/`: surefire reports, the Newman JSON report
and console log, the NFR-02 measurements, and screenshots of any UI failure.

## 9. Suspension and resumption criteria

Testing is suspended if the application does not start, if more than one third of
the executed cases are blocked by the same cause, or if a critical defect makes
the environment unusable. Testing resumes with a confirmation run of the affected
area followed by the regression pack.

## 10. Roles

| Role | Responsibility |
|---|---|
| QA engineer (author of this project) | Test design, automation, execution, defect reporting, reporting |
| Developer | Fixing defects (same person in this practice project, with the fix verified by re-running the tests) |
| Product owner | Confirming the interpretation decisions listed in `business-rules.md` |
