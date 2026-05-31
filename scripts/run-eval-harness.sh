#!/usr/bin/env bash
# M29 eval harness gate: fails if pass rate drops more than 5% below baseline file.
set -euo pipefail

BASELINE_FILE="src/main/resources/evaluation/baseline-pass-rate.txt"
DATASET="${1:-medical-eval-v1}"

if [[ ! -f "$BASELINE_FILE" ]]; then
  echo "Missing baseline file: $BASELINE_FILE" >&2
  exit 1
fi

BASELINE=$(tr -d '[:space:]' < "$BASELINE_FILE")
echo "Eval harness baseline pass rate: $BASELINE (dataset=$DATASET)"

# Offline eval requires running app + DB; CI can wire mvn test -Dtest=EvalHarnessBaselineTest
mvn -q test -Dtest=EvalHarnessBaselineTest

echo "Eval harness gate passed."
