#!/usr/bin/env bash
# Compare before/after FunctionGemma live eval JSON reports.
set -euo pipefail

BEFORE="${1:?Usage: $0 <before.json> <after.json> [output.md]}"
AFTER="${2:?Usage: $0 <before.json> <after.json> [output.md]}"
OUTPUT="${3:-target/eval/tool-selection-comparison.md}"

mvn -q exec:java \
  -Dexec.mainClass=com.berdachuk.medexpertmatch.llm.eval.ToolSelectionEvalCompareMain \
  -Dexec.args="\"$BEFORE\" \"$AFTER\" \"$OUTPUT\""

echo "Comparison written to $OUTPUT"
