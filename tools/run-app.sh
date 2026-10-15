#!/usr/bin/env bash
#
# Builds AppointmentHub and starts it in the foreground.
#
# Usage:
#   tools/run-app.sh                 # port 8080, appointments stored in data/appointments.json
#   tools/run-app.sh --store=memory  # no persistence (recommended for demo and test runs)
#   tools/run-app.sh --port=9090
#
set -euo pipefail

cd "$(dirname "$0")/.."

if [ ! -f target/appointmenthub.jar ]; then
  echo "Building the application..."
  ./mvnw -q -DskipTests package
fi

echo "Starting AppointmentHub: http://localhost:${1:-8080}/"
exec java -jar target/appointmenthub.jar "$@"
