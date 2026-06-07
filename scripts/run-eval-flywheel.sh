#!/usr/bin/env bash
# M62 eval flywheel: deterministic JSONL suites + combined release-gate report.
set -euo pipefail

OUTPUT_DIR="${1:-target/eval}"

echo "Running deterministic *EvalTest gate..."
mvn -q test -Dtest='*EvalTest,EvalFlywheelReportTest'

echo "Generating combined flywheel report..."
mvn -q "-DskipTests" "exec:java" \
  "-Dexec.mainClass=com.berdachuk.medexpertmatch.llm.eval.EvalFlywheelMain" \
  "-Dexec.args=$OUTPUT_DIR"

echo "Eval flywheel gate passed."
