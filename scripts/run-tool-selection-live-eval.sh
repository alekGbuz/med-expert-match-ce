#!/usr/bin/env bash
# M58: live FunctionGemma tool-selection eval on golden dataset (before/after fine-tune).
set -euo pipefail

LABEL="${1:-baseline}"
MODEL="${TOOL_CALLING_MODEL:-functiongemma:270m}"
BASE_URL="${TOOL_CALLING_BASE_URL:-http://127.0.0.1:11434/v1}"
API_KEY="${TOOL_CALLING_API_KEY:-none}"
DATASET="${MEDEXPERTMATCH_TOOL_SELECTION_DATASET:-${TOOL_SELECTION_DATASET:-}}"

echo "Running FunctionGemma live eval label=$LABEL model=$MODEL baseUrl=$BASE_URL"
if [[ -n "$DATASET" ]]; then
  echo "Dataset: $DATASET"
fi

export TOOL_CALLING_MODEL="$MODEL"
export TOOL_CALLING_BASE_URL="$BASE_URL"
export TOOL_CALLING_API_KEY="$API_KEY"

MVN_ARGS=(
  -Dtest=ToolSelectionLiveEvalIT
  -Dmedexpertmatch.eval.tool-selection.live=true
  -Dmedexpertmatch.eval.tool-selection.label="$LABEL"
)
if [[ -n "$DATASET" ]]; then
  MVN_ARGS+=(-Dmedexpertmatch.eval.tool-selection.dataset="$DATASET")
fi

mvn -q test "${MVN_ARGS[@]}"

echo "Reports written under target/eval/ (JSON + Markdown)"
