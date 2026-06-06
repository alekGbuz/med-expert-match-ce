#!/usr/bin/env python3
"""
Export MedExpertMatch tool-selection JSONL to Unsloth / HuggingFace FunctionGemma SFT format.

Compatible with:
  https://colab.research.google.com/github/unslothai/notebooks/blob/main/nb/FunctionGemma_(270M).ipynb

Each row contains:
  - messages: developer (orchestrator) + user + model (tool call or text)
  - tools: OpenAI-style function schemas (medexpertmatch_tool_schemas.json)

Usage:
  python scripts/generate-tool-selection-eval-dataset.py --size 600 --eval-jsonl target/eval/large.jsonl
  python scripts/export-unsloth-functiongemma-dataset.py \
    --input target/eval/large.jsonl \
    --output target/functiongemma-unsloth-train.jsonl
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from tool_selection_dataset_lib import build_user_prompt

ORCHESTRATOR = """You are in Auto orchestrator mode.

**CRITICAL:** For doctor matching, case analysis, routing, or evidence requests, call the medical tool directly.
- Do NOT use Task or TodoWrite for single-domain requests (match, analyze, route, evidence).
- If a 24-character case ID is available, call match_doctors_to_case / analyze_case / match_facilities_for_case with that exact ID.
- If no case ID is available, call match_doctors_from_text or analyze_case_text with the full case description.
- For evidence: search_clinical_guidelines or query_pubmed."""


def escape_arg(value: str) -> str:
    return value.replace("<escape>", "").replace("<end_function_call>", "")


def format_function_call(tool_name: str, args: dict[str, str]) -> str:
    if not args:
        return f"<start_function_call>call:{tool_name}<end_function_call>"
    parts = []
    for key, value in args.items():
        parts.append(f"{key}:<escape>{escape_arg(str(value))}<escape>")
    inner = ",".join(parts)
    return f"<start_function_call>call:{tool_name}{{{inner}}}<end_function_call>"


def infer_args(row: dict) -> dict[str, str]:
    expected_args = row.get("expectedArgs") or {}
    if expected_args:
        return {k: str(v) for k, v in expected_args.items()}
    tool = row.get("expectedTool")
    user_message = row.get("userMessage", "")
    if tool == "analyze_case_text":
        return {"caseText": user_message}
    if tool == "match_doctors_from_text":
        return {"caseText": user_message}
    if tool == "query_pubmed":
        return {"query": user_message}
    if tool == "search_clinical_guidelines":
        return {"condition": user_message}
    return {}


def to_unsloth_row(row: dict, tools: list[dict]) -> dict:
    goal = row["goalType"]
    case_id = row.get("caseId") if row.get("caseIdInHints") else None
    user_message = row["userMessage"]
    user_prompt = build_user_prompt(case_id, goal, user_message)

    expected_tool = row.get("expectedTool")
    if expected_tool:
        model_content = format_function_call(expected_tool, infer_args(row))
    else:
        model_content = (
            "This is a general informational question. I will answer in natural language "
            "without calling a medical workflow tool."
        )

    return {
        "messages": [
            {"role": "developer", "content": ORCHESTRATOR},
            {"role": "user", "content": user_prompt},
            {"role": "model", "content": model_content},
        ],
        "tools": tools,
        "scenario": row.get("scenario"),
        "locale": row.get("locale"),
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="Export eval JSONL to Unsloth FunctionGemma format")
    parser.add_argument("--input", type=Path, required=True, help="Input eval JSONL")
    parser.add_argument("--output", type=Path, default=Path("target/functiongemma-unsloth-train.jsonl"))
    parser.add_argument(
        "--schemas",
        type=Path,
        default=Path(__file__).resolve().parent / "medexpertmatch_tool_schemas.json",
    )
    args = parser.parse_args()

    tools = json.loads(args.schemas.read_text(encoding="utf-8"))
    rows_out: list[dict] = []
    with args.input.open(encoding="utf-8") as handle:
        for line in handle:
            if not line.strip():
                continue
            row = json.loads(line)
            rows_out.append(to_unsloth_row(row, tools))

    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8") as handle:
        for row in rows_out:
            handle.write(json.dumps(row, ensure_ascii=False) + "\n")

    print(f"Exported {len(rows_out)} Unsloth rows -> {args.output}")
    print("Upload to Colab or load with datasets:")
    print("  dataset = load_dataset('json', data_files={'train': 'functiongemma-unsloth-train.jsonl'})")


if __name__ == "__main__":
    main()
