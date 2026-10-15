# AppointmentHub — risk register

Version 1.0 · Updated after the execution recorded in the [test report](test-report.md)

Probability and impact are assessed for the first release in a test environment.

| ID | Risk | Probability | Impact | Mitigation | Status after the campaign |
|---|---|---|---|---|---|
| R-01 | Duplicate bookings for the same slot | Medium | Critical | Availability validated on the server inside the creation step; concurrency tests at service and HTTP level | **Controlled**: 12 and 10 parallel requests always produce exactly one appointment; `ConcurrencyApiTest` passes |
| R-02 | Wrong date or time interpretation (time zone, boundaries) | Medium | High | Time zone configurable and documented (`UTC` by default); boundary tests for yesterday/today/tomorrow and for opening/closing times with a fixed clock | **Controlled**: `BookingServiceTest` verifies every boundary deterministically |
| R-03 | Interface shows data that differs from the API | Medium | High | Every operation re-reads the data from the API; `ApiToUiConsistencyUiTest` compares both sides | **Controlled**: consistency tests pass; the API is the single source of truth |
| R-04 | Unstable automated tests | Medium | Medium | Explicit waits on user-visible conditions, `data-testid` locators, per-test data, loading indicator | **Realised and resolved**: BUG-004 (stale elements) was found and fixed; three consecutive green runs |
| R-05 | Residual data affects the next execution | High | Medium | Tests use in-memory storage and generate their own far-future dates; Postman requests compute a unique date each run | **Controlled**: the suites were re-run without cleaning data |
| R-06 | Ambiguous requirements | Medium | High | Interpretation decisions documented in `business-rules.md`; error codes and validation order documented in `api-spec.md` | **Partially controlled**: seven decisions documented and awaiting product-owner confirmation |
| R-07 | Scope grows beyond a beginner project | Medium | Medium | Booking and cancellation implemented and verified first; everything else listed as out of scope | **Controlled**: the first release stays within the PDR scope |
| R-08 | Performance results are not representative | Medium | Medium | Environment, sample size and client behaviour documented; results reported as a local sample, not a load test | **Open (accepted)**: the 2 s target is met with a large margin (p95 = 42 ms), but the sample is local |
| R-09 | Browser-dependent behaviour of the interface | Low | Medium | Documented browser version; stable locators; headless and headed runs | **Open (accepted)**: only Chromium 155 was executed |
| R-10 | Incomplete error handling leaking internal details | Low | High | Single error handler; `ErrorContractApiTest` checks structure and the absence of stack traces | **Controlled**: no internal detail is exposed |

## Residual risk statement

The campaign leaves no open defect and no unverified high-priority requirement.
The residual risks are R-08 (performance sample), R-09 (single browser engine) and
R-06 (interpretation decisions to be confirmed). None of them blocks the
acceptance criteria of the first release, and all are documented so that the
decision to release or to extend the campaign is taken with the facts at hand.
