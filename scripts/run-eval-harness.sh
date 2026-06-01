#!/usr/bin/env bash
# M29/M33 eval harness gate: structural integrity + optional full pass-rate IT in CI verify.
set -euo pipefail

BASELINE_FILE="src/main/resources/evaluation/baseline-pass-rate.txt"
DATASET="${1:-medical-eval-v1}"

if [[ ! -f "$BASELINE_FILE" ]]; then
  echo "Missing baseline file: $BASELINE_FILE" >&2
  exit 1
fi

BASELINE=$(tr -d '[:space:]' < "$BASELINE_FILE")
echo "Eval harness baseline pass rate: $BASELINE (dataset=$DATASET)"

mvn -q test -Dtest=EvalHarnessBaselineTest,EvalHarnessPassRateGateTest,EvalDatasetIntegrityServiceTest

echo "Eval harness gate passed."
