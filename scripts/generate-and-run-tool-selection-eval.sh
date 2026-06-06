#!/usr/bin/env bash
# Generate a large tool-selection dataset and run live FunctionGemma eval.
set -euo pipefail

SIZE="${1:-500}"
LABEL="${2:-baseline}"
SEED="${SEED:-42}"
OUTPUT="${OUTPUT:-target/eval/tool-selection-large.jsonl}"

echo "Generating $SIZE synthetic eval cases (seed=$SEED)..."
python scripts/generate-tool-selection-eval-dataset.py \
  --size "$SIZE" \
  --seed "$SEED" \
  --min-per-scenario 20 \
  --eval-jsonl "$OUTPUT"

export MEDEXPERTMATCH_TOOL_SELECTION_DATASET="$OUTPUT"
./scripts/run-tool-selection-live-eval.sh "$LABEL"
