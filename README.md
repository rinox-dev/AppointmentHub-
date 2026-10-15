# AppointmentHub

**Appointment booking and management system — QA portfolio project**

AppointmentHub is a small web application with a Java REST API that manages
appointments between customers and service providers. The project implements the
[AppointmentHub PDR](docs/PDR.md) (v1.0) and the complete QA cycle around it:
verifiable requirements, ISTQB CTFL test design techniques, manual test cases,
Postman API tests, Selenium WebDriver UI automation, defect management,
traceability and a final test report.

> All data used in the project is fictional. The application is meant for a test
> environment only.

---

## 1. What is in the repository

| Area | Where |
|---|---|
| Java REST API + business rules | `src/main/java/com/appointmenthub` |
| Web interface (single page, no build step) | `src/main/resources/static` |
| Component tests (business rules, JUnit 5) | `src/test/java/com/appointmenthub/component` |
| API tests (REST contract, JUnit 5) | `src/test/java/com/appointmenthub/api` |
| UI automation (Selenium WebDriver + JUnit 5) | `src/test/java/com/appointmenthub/ui` |
| Postman collection and environment | `postman/` |
| QA documentation | `docs/` |
| Helper scripts | `tools/` |

QA deliverables: [PDR](docs/PDR.md), [requirements](docs/requirements.md),
[business rules](docs/business-rules.md), [API specification](docs/api-spec.md),
[data model](docs/data-model.md), [configuration](docs/configuration.md),
[test plan](docs/test-plan.md), [test cases](docs/test-cases.md) and
[CSV version](docs/test-cases.csv), [traceability matrix](docs/traceability-matrix.md),
[defect log](docs/defect-log.md), [risk register](docs/risks.md),
[test report](docs/test-report.md) and [execution evidence](docs/evidence).

## 2. Prerequisites

* **JDK 17 or newer** (the Maven wrapper downloads Maven itself, no Maven install needed)
* **Chrome or Chromium** for the Selenium suite (Selenium Manager downloads a
  matching driver automatically)
* **Node.js** only if you want to run the Postman collection with Newman

## 3. Quick start

```bash
# 1. Build and start the application (web interface + REST API)
./mvnw -q -DskipTests package
java -jar target/appointmenthub.jar --store=memory
#   Web interface : http://localhost:8080/
#   REST API      : http://localhost:8080/api/services

# or, without packaging:
./mvnw exec:java
```

The default time zone is `UTC`, business hours are 09:00–17:00 and slots are
generated every 30 minutes. See [docs/configuration.md](docs/configuration.md)
for every setting.

## 4. Running the tests

```bash
# Component + API tests (84 tests, no browser required)
./mvnw test

# Selenium UI tests (18 tests, headless Chrome by default)
tools/run-ui-tests.sh

# Postman collection through Newman (54 requests, 137 assertions)
tools/run-api-tests.sh
```

Useful options:

| Command | Effect |
|---|---|
| `./mvnw test -Pall-tests` | Component + API + UI tests in one run |
| `HEADLESS=false tools/run-ui-tests.sh` | Watch the browser while the UI tests run |
| `APP_BASE_URL=http://localhost:8080 tools/run-ui-tests.sh` | Run the UI suite against a running instance |
| `CHROME_BIN=/usr/bin/chromium CHROME_DRIVER=/usr/bin/chromedriver tools/run-ui-tests.sh` | Use a specific browser build |

## 5. Latest execution results

Full details are in the [test report](docs/test-report.md); raw evidence is in
`docs/evidence/`.

| Suite | Tests / requests | Result |
|---|---|---|
| Component tests (business rules) | 43 | 43 passed |
| API tests (REST contract) | 41 | 41 passed |
| UI tests (Selenium) | 18 | 18 passed |
| Postman collection (Newman) | 54 requests / 137 assertions | 0 failures |

Basic performance sample (NFR-02): 30 requests per endpoint, p95 = 42 ms against
the 2 second acceptance target, average response time 42 ms for the Postman run.

## 6. Documentation map

* Product and process: [PDR](docs/PDR.md), [test plan](docs/test-plan.md), [test report](docs/test-report.md)
* Requirements and rules: [requirements](docs/requirements.md), [business rules](docs/business-rules.md)
* Interface contracts: [API specification](docs/api-spec.md), [data model](docs/data-model.md), [configuration](docs/configuration.md)
* Testware: [test cases](docs/test-cases.md), [traceability matrix](docs/traceability-matrix.md)
* Quality records: [defect log](docs/defect-log.md), [risk register](docs/risks.md), [evidence](docs/evidence/README.md)
* Supporting dependencies: [dependency notes](docs/README-dependencies.md)

## 7. License

See [LICENSE](LICENSE).
