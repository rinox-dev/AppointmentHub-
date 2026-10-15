#!/usr/bin/env bash
#
# Runs the Selenium UI test suite. The application under test is started by the
# tests themselves on a free port with a clean store, so no manual setup is needed.
#
# Requirements: a Chrome/Chromium browser and a matching driver.
#   - by default Selenium Manager downloads both automatically;
#   - set CHROME_BIN / CHROME_DRIVER to use a specific installation, for example
#     CHROME_BIN=/usr/bin/chromium CHROME_DRIVER=/usr/bin/chromedriver.
#
# Usage:
#   tools/run-ui-tests.sh                    # headless, own application instance
#   HEADLESS=false tools/run-ui-tests.sh     # watch the browser
#   APP_BASE_URL=http://localhost:8080 tools/run-ui-tests.sh   # use a running instance
#
set -euo pipefail

cd "$(dirname "$0")/.."

ARGS=(
  -Pui-tests
  "-Dheadless=${HEADLESS:-true}"
  "-Dbrowser=${BROWSER:-chrome}"
)

if [ -n "${APP_BASE_URL:-}" ]; then
  ARGS+=("-Dapp.base.url=${APP_BASE_URL}")
fi
if [ -n "${CHROME_BIN:-}" ]; then
  ARGS+=("-Dchrome.binary=${CHROME_BIN}")
fi
if [ -n "${CHROME_DRIVER:-}" ]; then
  ARGS+=("-Dwebdriver.chrome.driver=${CHROME_DRIVER}")
fi

echo "==> Running the Selenium UI suite: ${ARGS[*]}"
./mvnw -B test "${ARGS[@]}"
