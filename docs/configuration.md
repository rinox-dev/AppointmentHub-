# AppointmentHub — configuration

Version 1.0 · Defaults verified by the automated tests

Every setting can be given as a command line argument (`--key=value`) or as an
environment variable (`APPOINTMENTHUB_<KEY>`). Command line arguments win over
environment variables, which win over the defaults.

| Setting | Argument | Environment variable | Default | Meaning |
|---|---|---|---|---|
| Port | `--port=8080` | `APPOINTMENTHUB_PORT` | `8080` | HTTP port for the interface and the API. `0` lets the operating system choose a free port (used by the tests) |
| Time zone | `--timezone=Europe/Madrid` | `APPOINTMENTHUB_TIMEZONE` | `UTC` | Zone used for every "current date" and "current time" decision (BR-13) |
| Opening time | `--open=09:00` | `APPOINTMENTHUB_OPEN` | `09:00` | First possible appointment start |
| Closing time | `--close=17:00` | `APPOINTMENTHUB_CLOSE` | `17:00` | Latest time an appointment may end |
| Slot size | `--slot-minutes=30` | `APPOINTMENTHUB_SLOT_MINUTES` | `30` | Granularity of the generated slots |
| Storage | `--store=memory` or `--store=data/appointments.json` | `APPOINTMENTHUB_STORE` | `data/appointments.json` | `memory` disables persistence |

Example:

```bash
java -jar target/appointmenthub.jar --port=9090 --store=memory --open=08:00 --close=18:00 --slot-minutes=15
APPOINTMENTHUB_TIMEZONE=Europe/Madrid APPOINTMENTHUB_PORT=8081 java -jar target/appointmenthub.jar
```

## Time zone and dates

* All date and time comparisons use the configured zone; timestamps are stored and
  returned in UTC (`Instant.toString()`).
* Dates in the API are `yyyy-MM-dd` and times are `HH:mm`, both parsed strictly.
* The time zone of the browser is irrelevant: the interface sends the date the
  user picked and the server interprets it in its own zone. This is documented so
  that a test or a customer is never surprised by a one-day difference.

## Test settings

The automated suites accept additional system properties.

| Property | Applies to | Default | Meaning |
|---|---|---|---|
| `app.base.url` | UI tests | empty | When empty, the UI suite starts its own instance on a free port; otherwise the suite runs against this URL |
| `browser` | UI tests | `chrome` | Only `chrome` is configured |
| `headless` | UI tests | `true` | `false` opens a visible window |
| `chrome.binary` | UI tests | Selenium default | Path to a specific browser executable |
| `webdriver.chrome.driver` | UI tests | Selenium Manager | Path to a specific driver executable |

Maven profiles:

| Profile | Command | Effect |
|---|---|---|
| (default) | `./mvnw test` | Component + API tests; the `ui` tag is excluded |
| `ui-tests` | `./mvnw test -Pui-tests` | Only the Selenium tests |
| `all-tests` | `./mvnw test -Pall-tests` | Everything, including the Selenium tests |

## Supported dependencies (documented separately as required by the PDR)

See [README-dependencies.md](README-dependencies.md).
