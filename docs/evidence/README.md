# AppointmentHub — execution evidence

This folder contains the raw evidence behind the numbers in the
[test report](../test-report.md). Everything here was produced by the commands in
[test plan](../test-plan.md) section 8 on 2026-10-15.

## Component and API suites (`component-api/`)

| File | Content |
|---|---|
| `summary.txt` | `Tests run:` lines of the Maven surefire run |
| `com.appointmenthub.*.txt` | Per-class surefire text reports (test counts, failures, stack traces if any) |
| `TEST-com.appointmenthub.*.xml` | Machine readable surefire reports, usable by CI dashboards |

Result of the recorded run: **84 tests, 0 failures, 0 errors, 0 skipped**
(43 component, 41 API).

## UI suite (`ui/`)

| File | Content |
|---|---|
| `summary.txt` | `Tests run:` lines of the Selenium run |
| `com.appointmenthub.ui.*.txt` | Per-class reports of the six UI test classes |

Result of the recorded run: **18 tests, 0 failures, 0 errors, 0 skipped**, and the
same result on a repeated run. Failures would additionally produce screenshots in
`target/screenshots/` (captured automatically by the test watcher); the folder is
empty for a green run, which is itself the expected state.

## API suite through Postman/Newman (`api/`)

| File | Content |
|---|---|
| `newman-report.json` | Full Newman report: 54 requests, 137 assertions, per-request response times |
| `newman-console.txt` | Console output including the summary table and the average response time |
| `app-under-test.log` | Startup banner of the application instance used by the run |

Result of the recorded run: **54 requests, 137 assertions, 0 failures**, average
response time **42 ms**.

## Performance samples (`performance/`)

| File | Content |
|---|---|
| `nfr-02-response-times.txt` | The three NFR-02 measurement lines printed by `PerformanceApiTest` (30 samples per endpoint) |

Result: average 41 ms, p95 42 ms, maximum 43 ms against the 2000 ms target.

## How to reproduce

```bash
./mvnw test                                   # regenerates component-api evidence
tools/run-api-tests.sh                        # regenerates api evidence
tools/run-ui-tests.sh                         # runs the UI suite (see note below)
```

The Maven runs always write to `target/surefire-reports`; copy the files from
there into this folder to refresh the evidence, exactly as the helper scripts and
the recorded campaign did.

## Honest reporting rules applied here

* Every number in the test report comes from a file in this folder.
* Results that were not executed are labelled as such; nothing is presented as
  verified without evidence.
* The simulated defect example in the defect log is explicitly marked as
  simulated.
* Limitations (single browser, local performance sample, container font
  requirement) are listed in the test report instead of being hidden.
