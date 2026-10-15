# PDR — AppointmentHub

## Appointment Booking and Management System

**Version:** 1.0 · **Status:** Initial Proposal · **Difficulty Level:** Beginner ·
**Project Type:** Web Application with REST API and Test Automation ·
**Focus:** Functional QA, API Testing, and UI Test Automation ·
**Primary Technologies:** Java, Postman, and Selenium WebDriver ·
**Testing Framework Reference:** ISTQB Certified Tester Foundation Level (CTFL)

> This is the source product definition used by the whole project. Implementation
> decisions and their justification are recorded in
> [business-rules.md](business-rules.md), [api-spec.md](api-spec.md) and
> [configuration.md](configuration.md); execution results are in
> [test-report.md](test-report.md).

---

## 1. Executive Summary

AppointmentHub is a web application designed to manage appointments between
customers and service providers. Users will be able to browse available services,
check available time slots, book appointments, view existing bookings, and cancel
appointments.

The system will include a web interface and a REST API responsible for managing
appointment information and availability.

The project will have a deliberately limited scope to make implementation
achievable for a beginner while emphasizing software quality assurance practices.

Testing and validation activities will include:

- Manual functional testing.
- API testing using Postman.
- Web UI test automation using Selenium WebDriver with Java.
- Application of ISTQB CTFL test design techniques.
- Defect reporting, tracking, and verification.
- Requirements-to-test traceability.
- Regression testing of critical functionality.

The expected outcome is a demonstrable QA portfolio project that showcases the
ability to analyze requirements, identify risks, design test cases, execute manual
tests, automate critical scenarios, and communicate testing results.

## 2. Project Objectives

### 2.1 General Objective

Design, implement, and validate an appointment management application that
demonstrates fundamental QA competencies through functional testing, REST API
testing, and web UI automation.

### 2.2 Specific Objectives

1. Implement the core functionality for checking availability, booking
   appointments, viewing bookings, and canceling appointments.
2. Define verifiable functional and non-functional requirements.
3. Design positive, negative, and boundary test cases.
4. Apply test design techniques described in ISTQB CTFL.
5. Validate API contracts and business rules using Postman.
6. Automate critical user journeys using Selenium WebDriver and Java.
7. Maintain traceability between requirements, test cases, and defects.
8. Evaluate test results and communicate outstanding risks.

## 3. Project Scope

### 3.1 In-Scope Features

- Display available services.
- Retrieve available time slots for a service and date.
- Book an appointment.
- View registered appointments.
- Retrieve appointment details.
- Cancel an appointment.
- Validate required fields and input formats.
- Prevent duplicate bookings for the same time slot.
- Reject bookings for past dates.
- Display clear confirmation and error messages.
- Validate operations through a REST API.
- Automate primary user journeys through the web interface.

### 3.2 Out-of-Scope Features

To keep the initial release suitable for a beginner, the following features are
excluded:

- Online payments.
- Email or SMS reminders.
- External calendar integrations.
- Video conferencing.
- Mobile applications.
- Advanced role and permission management.
- Recurring appointments.
- Multiple branch management.
- Third-party service integrations.
- Advanced load testing or specialized security audits.

### 3.3 Assumptions and Constraints

- The application will initially run in a test environment.
- Customer information will be fictional.
- The system's time zone will be defined and documented.
- Business hours will be configured in advance.
- The application will provide both a web interface and a REST API.
- Java will be the primary implementation and automation language.
- Postman will be the primary tool for API testing.
- Selenium WebDriver will be the primary tool for UI automation.
- No additional technologies will be required as primary project technologies. Any
  supporting dependencies needed to compile or run the Java application must be
  documented separately (see [README-dependencies.md](README-dependencies.md)).

## 4. System Overview

### 4.1 User Types

**Customer:** Browses services and availability, books appointments, views
existing bookings, and cancels eligible appointments.

**Service Provider:** Represents the resource offering appointments at specific
times. In the initial release, providers may be represented through a
preconfigured catalog without a separate administrative portal.

**QA Team:** Reviews requirements, executes test cases, records defects, and
verifies fixes.

### 4.2 Main Booking Workflow

1. The user opens the services page.
2. The user selects a service.
3. The user selects a date.
4. The system displays available time slots.
5. The user selects a time slot.
6. The user enters contact information.
7. The user confirms the booking.
8. The system validates the input and checks availability again.
9. The system creates the appointment and returns a unique identifier.
10. The interface displays a confirmation containing the booking details.

If the selected time slot becomes unavailable before the booking is confirmed, the
system must reject the request and ask the user to select another time slot.

### 4.3 Cancellation Workflow

1. The user views existing appointments.
2. The user selects an appointment.
3. The user requests cancellation.
4. The system verifies that the appointment exists and is eligible for cancellation.
5. The system updates the appointment status.
6. The appointment no longer occupies the time slot.
7. The interface displays a cancellation confirmation.

## 5. Proposed Architecture

AppointmentHub will use a simple architecture consisting of three logical
components.

### 5.1 Web Interface

Responsibilities:

- Display services and time slots.
- Collect booking information.
- Display appointments and statuses.
- Show errors and confirmations.
- Communicate with the REST API.

### 5.2 REST API and Business Logic

Implemented in Java. Responsibilities include:

- Validating incoming requests.
- Applying booking and cancellation rules.
- Retrieving and updating appointments.
- Preventing duplicate bookings.
- Providing consistent responses.
- Returning appropriate HTTP status codes.

### 5.3 Data Persistence

The system must store appointments and the minimum information required to manage
them. The implementation may use a simple storage solution suitable for
development, provided it supports the required persistence and verification
scenarios.

Critical business rules must not be enforced exclusively by the web interface. The
API must independently validate these rules.

### 5.4 Logical Architecture Diagram

```text
User
  |
  v
Web Interface
  |
  v
Java REST API
  |
  v
Business Rules
  |
  v
Appointment Storage
```

The testing approach will use two primary paths:

```text
Postman -----------------> REST API
Selenium + Java ---------> Web Interface
```

API tests will validate server responses directly. Selenium tests will validate
behavior visible to the user.

## 6. Data Model

### 6.1 Appointment Entity

| Field | Conceptual Type | Description |
|---|---|---|
| id | Identifier | Unique appointment identifier |
| customerName | String | Customer's name |
| customerEmail | String | Customer's contact email |
| serviceId | Identifier | Requested service |
| appointmentDate | Date | Appointment date |
| startTime | Time | Appointment start time |
| endTime | Time | Appointment end time |
| status | Enumeration | Appointment status |
| createdAt | Date-time | Appointment creation timestamp |

### 6.2 Appointment Statuses

- `CONFIRMED`: The appointment is confirmed and occupies the time slot.
- `CANCELLED`: The appointment has been canceled and no longer occupies the time slot.

The first release does not require additional statuses such as pending, completed,
or no-show.

### 6.3 Service Entity

| Field | Conceptual Type | Description |
|---|---|---|
| id | Identifier | Unique service identifier |
| name | String | Service name |
| durationMinutes | Positive integer | Appointment duration |
| active | Boolean | Indicates whether the service is available |

Services may be preconfigured to simplify implementation.

## 7. Functional Requirements

| ID | Requirement | Priority |
|---|---|---|
| FR-01 | The system shall display active services. | High |
| FR-02 | The system shall allow users to retrieve available time slots by service and date. | High |
| FR-03 | The system shall allow users to book an appointment using valid data. | High |
| FR-04 | The system shall generate a unique identifier for every appointment. | High |
| FR-05 | The system shall display the details of an existing appointment. | High |
| FR-06 | The system shall allow users to retrieve registered appointments. | High |
| FR-07 | The system shall allow users to cancel a confirmed appointment. | High |
| FR-08 | The system shall prevent booking an occupied time slot. | High |
| FR-09 | The system shall reject bookings with missing or invalid required data. | High |
| FR-10 | The system shall reject bookings for past dates. | High |
| FR-11 | The system shall release the time slot when an appointment is canceled. | High |
| FR-12 | The system shall inform users of the result of each operation. | Medium |
| FR-13 | The system shall reject cancellation requests for nonexistent appointments. | Medium |
| FR-14 | The system shall prevent an appointment from being canceled more than once. | Medium |

## 8. Non-Functional Requirements

| ID | Requirement | Verification Criterion |
|---|---|---|
| NFR-01 | Usability | Users shall complete the booking and cancellation flows without external instructions, using understandable messages. |
| NFR-02 | Basic performance | Under documented test conditions, routine API operations shall respond within 2 seconds for at least 95% of the sample requests. |
| NFR-03 | Data integrity | Two confirmed appointments shall not occupy incompatible time slots for the same resource. |
| NFR-04 | Consistency | An appointment created or canceled through the API shall be reflected correctly in the web interface. |
| NFR-05 | Maintainability | Automated tests shall use descriptive names and avoid unnecessary duplication. |
| NFR-06 | Compatibility | Core user journeys shall work in the documented browser version selected for testing. |
| NFR-07 | Input validation | The API shall validate submitted data even when interface-level validation is bypassed. |
| NFR-08 | Error handling | Errors shall return consistent HTTP status codes and understandable messages without exposing internal implementation details. |
| NFR-09 | Data privacy | Tests shall use fictional data and shall not expose real personal information. |
| NFR-10 | Repeatability | Test cases shall document preconditions and allow results to be reproduced. |

The performance threshold is an initial project acceptance target, not a claim
about the performance of an implementation that has not yet been tested.

## 9. Business Rules

- **BR-01 — Required fields:** Every booking must include a name, email, service, date, and time.
- **BR-02 — Email format:** The email address must satisfy the project's documented validation rules.
- **BR-03 — Active service:** Only active services may be booked.
- **BR-04 — Valid date:** Bookings for dates before the system's current date are not permitted.
- **BR-05 — Business hours:** Appointments must fall within configured business hours.
- **BR-06 — Duration:** Appointment duration is determined by the selected service.
- **BR-07 — Availability:** The time slot must be available both when queried and when the booking is confirmed.
- **BR-08 — No duplicate bookings:** Conflicting appointments for the same provider or resource and time period are not permitted.
- **BR-09 — Cancellation:** Only confirmed appointments may be canceled.
- **BR-10 — Slot release:** Canceling an appointment must release its associated time slot.
- **BR-11 — Unique identifier:** Each appointment must have a unique identifier.
- **BR-12 — Consistency:** The interface must not display an appointment as confirmed when the server rejected the booking.
- **BR-13 — Time zone:** The system must use a defined time zone when interpreting current dates and appointment times.

For the initial release, appointments will have a fixed duration per service, and
available slots will be generated from predefined schedules.

## 10. REST API Specification

The API will expose the following endpoints. These names form a proposed contract
and must remain consistent throughout implementation. The exact behaviour is
documented in [api-spec.md](api-spec.md).

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/services` | Retrieve active services |
| GET | `/api/availability?serviceId={id}&date={date}` | Retrieve available time slots |
| POST | `/api/appointments` | Create an appointment |
| GET | `/api/appointments` | Retrieve registered appointments |
| GET | `/api/appointments/{id}` | Retrieve a specific appointment |
| DELETE | `/api/appointments/{id}` | Cancel an appointment |

### 10.1 Appointment Creation Example

**Request**

```http
POST /api/appointments
Content-Type: application/json
```

```json
{
  "customerName": "Ana Example",
  "customerEmail": "ana@example.com",
  "serviceId": 1,
  "appointmentDate": "2027-02-10",
  "startTime": "10:00"
}
```

**Proposed successful response**

HTTP status: `201 Created`

```json
{
  "id": 1001,
  "customerName": "Ana Example",
  "customerEmail": "ana@example.com",
  "serviceId": 1,
  "appointmentDate": "2027-02-10",
  "startTime": "10:00",
  "status": "CONFIRMED"
}
```

### 10.2 Expected HTTP Status Codes

| Code | Expected Use |
|---|---|
| 200 OK | Successful query or cancellation processed with a response |
| 201 Created | Appointment created successfully |
| 400 Bad Request | Invalid data or malformed request |
| 404 Not Found | Appointment or service does not exist |
| 409 Conflict | Time slot is occupied or a state transition is not allowed |
| 500 Internal Server Error | Unexpected server error |

The implementation may define a consistent error response containing a functional
error code and a clear message. Actual HTTP responses must match the documented
contract.

## 11. ISTQB CTFL-Based Test Strategy

### 11.1 Testing Principles

1. Testing can reveal the presence of defects, but cannot prove their complete absence.
2. Exhaustive testing is impractical; scenarios with the highest value will be prioritized.
3. Early testing reduces the cost of correcting defects.
4. Defects tend to cluster in specific areas, such as availability and cancellation.
5. Repeating the same tests may eventually stop finding new defects.
6. Testing is context-dependent.
7. A system with no known defects does not necessarily meet user needs.

### 11.2 Test Process

**Planning:** Define scope, objectives, risks, resources, and entry and exit criteria.

**Analysis:** Review requirements and identify test conditions.

**Design:** Apply ISTQB techniques to derive test cases.

**Implementation:** Prepare test data, Postman collections, and Selenium scripts.

**Execution:** Execute tests, compare actual and expected results, and record evidence.

**Completion:** Summarize results, outstanding defects, coverage, and residual risks.

### 11.3 Test Levels

- **Component testing:** Validate individual units of business logic when they can be tested in isolation.
- **Integration testing:** Verify interactions between the interface, API, and storage.
- **System testing:** Validate the complete application against its functional requirements.
- **Acceptance testing:** Verify that the workflows satisfy the defined business acceptance criteria.

### 11.4 Test Types

- Functional testing.
- Basic non-functional testing, especially performance and usability.
- Confirmation testing after defect fixes.
- Regression testing after changes.
- Negative testing and input validation.
- Exploratory testing to discover scenarios not covered by existing cases.

### 11.5 Test Design Techniques

**Equivalence Partitioning:** Divide inputs into classes expected to behave
similarly. Example: valid email, invalid email, and missing email.

**Boundary Value Analysis:** Check values at, immediately inside, and immediately
outside a boundary. Example: booking on the current date versus a past date.

**Decision Tables:** Validate combinations of conditions. Example: appointment
exists or does not exist, and its status is confirmed or canceled.

**State Transition Testing:** Verify permitted and prohibited appointment state
transitions.

**Use Case Testing:** Validate complete booking and cancellation workflows.

**Exploratory Testing:** Investigate unexpected behavior, such as repeatedly
booking the same time slot or refreshing the page after cancellation.

## 12. Functional Test Cases

The following cases form the initial test suite. Their identifiers should remain
consistent during execution and automation. Execution results are in
[test-cases.md](test-cases.md); two cases (TC-021, TC-022) were added during design
to cover BR-03 and BR-05.

| ID | Scenario | Expected Result | Priority |
|---|---|---|---|
| TC-001 | Retrieve active services | Available services are displayed. | High |
| TC-002 | Retrieve availability for a valid date | Available time slots are returned. | High |
| TC-003 | Book an appointment with valid data | The appointment is created and an identifier is returned. | High |
| TC-004 | Book without a name | The request is rejected with a validation error. | High |
| TC-005 | Book without an email | The request is rejected with a validation error. | High |
| TC-006 | Book with a malformed email | The request is rejected. | High |
| TC-007 | Book for a past date | The request is rejected. | High |
| TC-008 | Book an occupied time slot | The request is rejected without creating a duplicate. | High |
| TC-009 | Retrieve an existing appointment | Correct appointment details are returned. | High |
| TC-010 | Retrieve a nonexistent appointment | A not-found response is returned. | Medium |
| TC-011 | Cancel a confirmed appointment | The status changes to canceled and the time slot is released. | High |
| TC-012 | Cancel an already canceled appointment | The request is rejected according to the documented contract. | Medium |
| TC-013 | Cancel a nonexistent appointment | A not-found response is returned. | Medium |
| TC-014 | Book a previously released time slot | The booking succeeds if the slot remains available. | High |
| TC-015 | Create an appointment through the API and view it on the website | The appointment is displayed with matching details. | High |
| TC-016 | Submit a nonexistent service | The booking is rejected. | High |
| TC-017 | Submit a date in an invalid format | The request is rejected in a controlled manner. | Medium |
| TC-018 | Submit a request without a JSON body | The API returns a controlled error. | Medium |
| TC-019 | Confirm a booking through the UI | A success message and appointment identifier are displayed. | High |
| TC-020 | Cancel through the UI | The canceled status is displayed and availability is updated. | High |

Each test case must document preconditions, input data, steps, expected results,
actual results, execution status, and evidence.

## 13. ISTQB Test Design Examples

### 13.1 Equivalence Partitioning: Email Address

| Partition | Example Input | Expected Result |
|---|---|---|
| Valid | `ana@example.com` | Accepted |
| Missing `@` | `ana.example.com` | Rejected |
| Missing domain | `ana@` | Rejected |
| Empty value | Empty string | Rejected |
| Missing field | `customerEmail` property omitted | Rejected |

These examples represent input classes. The final validation rules must match the
project's documented requirements.

### 13.2 Boundary Value Analysis: Booking Date

For a test whose reference current date is February 10, 2027:

| Input | Expected Result |
|---|---|
| February 9, 2027 | Rejected |
| February 10, 2027 | Accepted if a valid future time slot is available |
| February 11, 2027 | Accepted if availability exists |

A valid date does not guarantee a valid booking if the time slot is occupied or
outside business hours.

### 13.3 Decision Table: Cancellation

| Appointment Exists | Current Status | Requested Action | Expected Result |
|---|---|---|---|
| Yes | Confirmed | Cancel | Cancellation accepted |
| Yes | Canceled | Cancel | Rejected |
| No | Not applicable | Cancel | Not found |
| Yes | Confirmed | Retrieve | Appointment details returned |

### 13.4 State Transition Testing

```text
CONFIRMED
    |
    | Cancel
    v
CANCELLED
```

The reverse transition is not permitted in the initial release. A canceled
appointment cannot be re-confirmed through the cancellation operation. If the user
wants another appointment, a new booking must be created.

## 14. API Testing with Postman

### 14.1 Objective

Verify that the API follows its contract, enforces business rules, and returns
correct responses for valid and invalid inputs. The implemented collection is in
`postman/AppointmentHub.postman_collection.json` (8 folders, 54 requests,
137 assertions).

### 14.2 Collection Structure

Organize the Postman collection by functionality:

1. Services.
2. Availability.
3. Appointment creation.
4. Appointment retrieval.
5. Cancellation.
6. Negative validation cases.
7. Conflict scenarios.
8. Regression tests.

### 14.3 Validations Per Request

As applicable, each endpoint will be checked for:

- HTTP status code.
- Response structure.
- Data types.
- Required fields.
- Expected values.
- Appointment identifier.
- Appointment status.
- Response time.
- Error messages.
- Persistence of changes.
- Absence of duplicate bookings.

### 14.4 Example Assertions

For a valid appointment creation:

- The HTTP status is `201`.
- The response contains an `id`.
- The status is `CONFIRMED`.
- The date and time match the request.
- The appointment can be retrieved through the corresponding endpoint.

For an occupied time slot:

- The response uses the documented conflict status.
- No second appointment is created.
- The original appointment remains unchanged.
- Availability reflects the occupied time slot.

For cancellation:

- The response follows the documented contract.
- The appointment is no longer confirmed.
- The time slot becomes available if no other restrictions apply.
- The updated appointment and availability can be verified through subsequent requests.

### 14.5 Test Data

Use controlled and identifiable test data, including:

- An active service.
- An inactive service, if included in the catalog.
- A confirmed appointment.
- A canceled appointment.
- An available time slot.
- An occupied time slot.
- A past date.
- A nonexistent appointment identifier.

Tests should not depend on execution order or on manually created data that may
disappear between runs.

## 15. UI Automation with Selenium and Java

### 15.1 Objective

Automate critical user journeys to verify that the web interface behaves correctly
and reflects the results of API operations.

### 15.2 Candidate Automation Scenarios

**UI-001 — Browse Services**
1. Open the home page.
2. Wait for the service catalog to appear.
3. Verify that at least one active service is displayed.

**UI-002 — Book an Appointment**
1. Select a service.
2. Select a valid date.
3. Select an available time slot.
4. Enter a name and email.
5. Confirm the booking.
6. Verify the success message and appointment identifier.

**UI-003 — Validate Required Fields**
1. Open the booking form.
2. Leave a required field empty.
3. Attempt to confirm the booking.
4. Verify that the booking is not completed and a clear validation message appears.

**UI-004 — Prevent Duplicate Bookings**
1. Book an available time slot.
2. Attempt to book the same time slot again.
3. Verify that the second attempt is rejected.

**UI-005 — Cancel an Appointment**
1. Retrieve a confirmed appointment.
2. Select the cancellation option.
3. Confirm the action if an additional confirmation is presented.
4. Verify the canceled status and released time slot.

### 15.3 Best Practices

- Use stable, descriptive locators.
- Prefer dedicated test identifiers when available.
- Use explicit waits for UI conditions rather than unnecessary fixed delays.
- Keep test data controlled.
- Avoid dependencies between test cases.
- Clean up test data when possible.
- Capture evidence when failures occur.
- Verify visible outcomes, not merely that a button was clicked.
- Separate test data, interaction steps, and assertions when complexity justifies it.

Initial UI automation will focus on critical user journeys. Not every negative
scenario needs to be automated through the UI when it can be validated more
directly through the API.

## 16. Defect Management

### 16.1 Defect Lifecycle

```text
New -> Under Analysis -> Assigned -> Fixed -> Pending Verification -> Closed
                                                |
                                                +-> Reopened, if verification fails
```

The lifecycle may be adapted to the team's workflow as long as traceability is
preserved.

### 16.2 Required Defect Report Information

Each defect must include:

- Defect identifier.
- Clear, concise title.
- Related requirement.
- Related test case.
- Execution environment.
- Preconditions.
- Reproduction steps.
- Expected result.
- Actual result.
- Severity.
- Priority.
- Evidence.
- Current status.

### 16.3 Severity Levels

| Severity | Description | Example |
|---|---|---|
| Critical | Prevents system operation or seriously compromises data integrity. | Two customers receive confirmed bookings for the same exclusive time slot. |
| High | Affects a core function without an acceptable workaround. | No appointments can be booked. |
| Medium | Partially affects a feature, with a workaround available. | The appointment list does not update until the page is refreshed. |
| Low | Minor visual issue with little impact on the workflow. | Secondary text is misaligned. |

Fix priority will consider severity, risk, user impact, and scenario frequency.

### 16.4 Example Defect

**ID:** BUG-001 (in the PDR; recorded as BUG-999 *simulated example* in
[defect-log.md](defect-log.md) to avoid clashing with the real defects found)
**Title:** The API allows two confirmed appointments for the same time slot.

**Precondition:** A service has an available time slot.

**Steps:**
1. Submit a valid request to book the time slot.
2. Submit a second request for the same service, date, and time.
3. Retrieve the registered appointments.

**Expected Result:** The first booking is confirmed, and the second is rejected
with the documented conflict response.

**Actual Result:** Must be determined during real execution; no result will be
assumed before testing the application. *During this project the behaviour was
executed and no defect was reproduced (see the concurrency tests).*

**Proposed Severity:** Critical if the behavior results in two confirmed
conflicting bookings.

## 17. Requirements Traceability Matrix

The following matrix connects requirements to primary test cases and automation
areas. The live version, including non-functional requirements, is in
[traceability-matrix.md](traceability-matrix.md).

| Requirement | Related Test Cases | Primary Method |
|---|---|---|
| FR-01 | TC-001 | Postman and Selenium |
| FR-02 | TC-002, TC-014 | Postman |
| FR-03 | TC-003, TC-019 | Postman and Selenium |
| FR-04 | TC-003, TC-009 | Postman |
| FR-05 | TC-009, TC-010 | Postman |
| FR-06 | TC-009, TC-015 | Postman and Selenium |
| FR-07 | TC-011, TC-020 | Postman and Selenium |
| FR-08 | TC-008, TC-014 | Postman |
| FR-09 | TC-004, TC-005, TC-006, TC-017 | Postman and Selenium |
| FR-10 | TC-007 | Postman |
| FR-11 | TC-011, TC-014 | Postman |
| FR-12 | TC-019, TC-020 | Selenium |
| FR-13 | TC-013 | Postman |
| FR-14 | TC-012 | Postman |

The matrix must be updated whenever requirements or test cases change.
Non-functional requirements will have a complementary matrix of checks,
measurements, and evidence.

## 18. Risk Analysis and Management

| ID | Risk | Probability | Impact | Mitigation |
|---|---|---|---|---|
| R-01 | Duplicate bookings | Medium | Critical | Validate availability on the server and test repeated booking requests. |
| R-02 | Incorrect date or time interpretation | Medium | High | Define a time zone and test date boundaries. |
| R-03 | UI data differs from API data | Medium | High | Perform integration tests and retrieve data after each operation. |
| R-04 | Unstable automated tests | Medium | Medium | Use explicit waits, stable locators, and controlled data. |
| R-05 | Residual data affects test execution | High | Medium | Prepare, identify, and clean test data. |
| R-06 | Ambiguous requirements | Medium | High | Define acceptance criteria before implementation and testing. |
| R-07 | Scope becomes too large for a beginner | Medium | Medium | Prioritize booking and cancellation before adding features. |
| R-08 | Performance results are not representative | Medium | Medium | Document the environment, sample size, and execution conditions. |

Testing priority will be risk-based. Booking, availability, duplicate prevention,
and cancellation scenarios will be executed before low-severity visual checks.

## 19. Test Entry and Exit Criteria

### 19.1 Entry Criteria

Formal testing may begin when:

- Requirements and acceptance criteria are documented.
- The application is deployed to the test environment.
- The API and web interface are accessible.
- Valid test data is available.
- API endpoints and response formats are defined.
- Test cases have clear expected results.
- The browser version and environment are documented.

### 19.2 Exit Criteria

The test campaign may be considered complete when:

- All high-priority test cases have been executed.
- All identified critical and high-severity defects have been fixed and verified,
  or outstanding risks have been formally accepted and documented.
- No known critical defects remain open.
- Booking and cancellation workflows work correctly.
- Critical regression tests have been executed.
- Test results are documented.
- The traceability matrix is up to date.
- Remaining limitations and risks are reported.

Meeting exit criteria does not prove that the application is defect-free. It
indicates that the agreed conditions for the test campaign have been satisfied.

## 20. Work Plan

The project will be delivered in six phases, with a suggested duration of six
weeks. The schedule can be adjusted according to availability and progress.

| Phase | Activities | Deliverables |
|---|---|---|
| 1. Analysis | Define scope, users, business rules, and requirements. | PDR, requirements, and acceptance criteria. |
| 2. Design | Define workflows, data model, endpoints, and test cases. | API specification, traceability matrix, and test cases. |
| 3. Implementation | Build the Java API and basic web interface. | Initial functional application. |
| 4. API Testing | Prepare the Postman collection, data, and assertions. | Collection and test results report. |
| 5. Automation | Automate critical workflows with Selenium and Java. | Automation scripts and execution report. |
| 6. Closure | Execute regression tests, analyze defects, and document results. | Final QA report and outstanding risks. |

### Suggested Schedule

- Week 1: Requirements and acceptance criteria.
- Week 2: API design and test cases.
- Week 3: Implementation of the main workflow.
- Week 4: Manual and API testing.
- Week 5: UI automation.
- Week 6: Regression, evidence collection, and final reporting.

## 21. Project Deliverables

| # | Deliverable | Where |
|---|---|---|
| 1 | AppointmentHub PDR | this document |
| 2 | Functional and non-functional requirements | [requirements.md](requirements.md) |
| 3 | Business rules specification | [business-rules.md](business-rules.md) |
| 4 | REST endpoint specification | [api-spec.md](api-spec.md) |
| 5 | Data model | [data-model.md](data-model.md) |
| 6 | Manual test cases | [test-cases.md](test-cases.md), [test-cases.csv](test-cases.csv) |
| 7 | Requirements traceability matrix | [traceability-matrix.md](traceability-matrix.md) |
| 8 | Postman collection | `postman/` |
| 9 | Selenium and Java automation scripts | `src/test/java/com/appointmenthub/ui` |
| 10 | Defect log (real defects plus one clearly labelled simulated example) | [defect-log.md](defect-log.md) |
| 11 | Execution evidence | [evidence](evidence/README.md) |
| 12 | Test report | [test-report.md](test-report.md) |
| 13 | Outstanding risks and recommendations | [risks.md](risks.md) |
| — | Supporting documents: test plan, configuration, dependencies | [test-plan.md](test-plan.md), [configuration.md](configuration.md), [README-dependencies.md](README-dependencies.md) |

Simulated defects must be clearly labeled as examples. The final report must
distinguish actual execution results from scenarios that have not yet been tested.

## 22. QA Metrics

**Requirements Coverage**

```text
Coverage = Requirements with associated tests / Total verifiable requirements x 100
```

**Execution Rate**

```text
Execution Rate = Executed test cases / Total planned test cases x 100
```

**Pass Rate**

```text
Pass Rate = Passed test cases / Executed test cases x 100
```

The report will also include:

- Failed and blocked test cases.
- Defects by severity.
- Open and closed defects.
- High-priority requirements that remain unverified.
- Automated test cases compared with the total cases selected for automation.

Metrics must be interpreted in context. A high pass rate is insufficient if
high-risk scenarios remain unexecuted. The computed values are in
[test-report.md](test-report.md) section 7.

## 23. Product Acceptance Criteria

AppointmentHub will meet the initial scope when:

- **AC-01:** Users can retrieve active services.
- **AC-02:** Users can retrieve available time slots for a valid service and date.
- **AC-03:** A valid booking creates an appointment with a unique identifier.
- **AC-04:** Missing or invalid required data is rejected.
- **AC-05:** Two conflicting appointments cannot both be confirmed for the same time slot.
- **AC-06:** Bookings for past dates are rejected.
- **AC-07:** An existing appointment can be retrieved using its identifier.
- **AC-08:** A confirmed appointment can be canceled.
- **AC-09:** A canceled appointment no longer occupies its time slot.
- **AC-10:** The interface reflects the results returned by the API.
- **AC-11:** Errors are communicated through consistent HTTP responses and understandable messages.
- **AC-12:** Critical workflows can be verified through API tests and UI automation.
- **AC-13:** Test results and defects are documented and linked to their corresponding requirements.

## 24. Definition of Done

A feature will be considered complete when:

- Its requirement is defined and verifiable.
- Its behavior has been implemented.
- Associated test cases have been executed.
- Identified defects have been recorded.
- Critical cases have passed, or a documented exception exists.
- Relevant automated tests execute successfully.
- Traceability is up to date.
- Documentation reflects the actual system behavior.

## 25. Conclusion

AppointmentHub is a beginner-level QA practice project with sufficient depth to
demonstrate fundamental software testing competencies.

Its primary value is not the number of features, but the quality of the testing
process: verifiable requirements, appropriate application of ISTQB techniques,
reproducible API tests, stable automation, traceability, and clear defect
communication.

The project will be considered successful when evidence demonstrates that the
booking, retrieval, and cancellation workflows satisfy the defined requirements
and that important risks have been identified, evaluated, and communicated.

---

**End of Document — AppointmentHub PDR v1.0**
