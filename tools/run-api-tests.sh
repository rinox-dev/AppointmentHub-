#!/usr/bin/env bash
#
# Runs the Postman collection with Newman against a freshly started application
# and stores the evidence under docs/evidence/api/.
#
# Usage: tools/run-api-tests.sh [port]
#
set -euo pipefail

cd "$(dirname "$0")/.."

PORT="${1:-18080}"
BASE_URL="http://localhost:${PORT}"
EVIDENCE_DIR="docs/evidence/api"
NEWMAN="${NEWMAN_BIN:-newman}"
if [ -x ".tools/node/node_modules/.bin/newman" ]; then
  NEWMAN=".tools/node/node_modules/.bin/newman"
fi

mkdir -p "$EVIDENCE_DIR"

echo "==> Building the application"
./mvnw -q -DskipTests package

echo "==> Starting the application on port ${PORT} (in-memory storage)"
java -jar target/appointmenthub.jar --store=memory --port="$PORT" > "$EVIDENCE_DIR/app-under-test.log" 2>&1 &
APP_PID=$!
trap 'kill "$APP_PID" 2>/dev/null || true' EXIT

echo "==> Waiting for the application to answer"
for _ in $(seq 1 40); do
  if curl -fsS "$BASE_URL/api/services" > /dev/null 2>&1; then
    break
  fi
  sleep 1
done
curl -fsS "$BASE_URL/api/services" > /dev/null || { echo "The application did not start"; exit 1; }

echo "==> Running the Postman collection with Newman"
"$NEWMAN" run postman/AppointmentHub.postman_collection.json \
  -e postman/AppointmentHub.postman_environment.json \
  --env-var "baseUrl=$BASE_URL" \
  --reporters cli,json \
  --reporter-json-export "$EVIDENCE_DIR/newman-report.json" \
  --color off | tee "$EVIDENCE_DIR/newman-console.txt"

echo "==> Evidence written to $EVIDENCE_DIR"
