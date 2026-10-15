# AppointmentHub — supporting dependencies

The PDR (section 3.3) states that no additional technology is required as a
primary project technology, and that any supporting dependency needed to compile
or run the Java application must be documented separately. This document is that
record.

## 1. Runtime dependency of the application

| Dependency | Version | Scope | Why it is needed | Alternative considered |
|---|---|---|---|---|
| `com.google.code.gson:gson` | 2.11.0 | compile | Reads and writes JSON for the REST API and for the storage file. Writing a JSON parser by hand would add risk without adding learning value | Jackson (larger surface), hand-written parser (rejected) |

Everything else in the application uses the JDK only:

* `com.sun.net.httpserver.HttpServer` for the HTTP server (no application server).
* `java.time` for dates, times and the clock (also used to make the tests
  deterministic).
* `java.util.concurrent` for the thread pool and the synchronisation that prevents
  duplicate bookings.

## 2. Test dependencies (never shipped with the application)

| Dependency | Version | Scope | Why it is needed |
|---|---|---|---|
| `org.junit.jupiter:junit-jupiter` | 5.10.2 | test | Test framework for the component, API and UI suites (including parameterised tests) |
| `org.seleniumhq.selenium:selenium-java` | 4.25.0 | test | WebDriver bindings for the UI automation, including Selenium Manager which resolves a matching driver |

## 3. Build and tooling

| Tool | Version | Notes |
|---|---|---|
| JDK | 17 (Temurin 17.0.12 used for the recorded execution) | `maven.compiler.release=17`; newer JDKs are fine |
| Maven | 3.9.9 | Not installed by hand: `./mvnw` downloads it |
| Maven plugins | compiler 3.13.0, surefire 3.2.5, shade 3.5.1, exec 3.2.0 | Build, test execution, runnable jar, `mvn exec:java` |
| Postman / Newman | Newman 6.2.3 | Runs the Postman collection from the command line; only needed to execute the API suite outside the Postman UI |
| Chromium / Chrome | 155 used for the recorded execution | Required by the Selenium suite; Selenium Manager downloads a matching driver automatically |
| Fonts and fontconfig | any standard installation | Chromium aborts when it cannot find any font (`SkFontMgr_FontConfigInterface`); a normal desktop or CI image already provides fonts |

## 4. Package contents of the runnable jar

`mvn package` produces `target/appointmenthub.jar` with the application classes,
the static web resources and the Gson classes, plus a `Main-Class` entry so that
`java -jar target/appointmenthub.jar` starts the whole system. No external
application server, database or web container is required.
