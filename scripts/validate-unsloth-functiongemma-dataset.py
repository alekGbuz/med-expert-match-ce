#!/usr/bin/env python3
"""
Validate MedExpertMatch Unsloth / HF FunctionGemma export rows.

Checks row shape (messages + tools), roles, and optional chat-template rendering.

Usage:
  python scripts/export-unsloth-functiongemma-dataset.py --input target/eval/large.jsonl --output target/train.jsonl
  python scripts/validate-unsloth-functiongemma-dataset.py target/train.jsonl
  python scripts/validate-unsloth-functiongemma-dataset.py target/train.jsonl --apply-template
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

REQUIRED_ROLES = ("developer", "user", "model")
FUNCTION_CALL_MARKERS = ("<start_function_call>", "<end_function_call>")


def validate_row(row: dict, line_no: int) -> list[str]:
    errors: list[str] = []
    messages = row.get("messages")
    tools = row.get("tools")

    if not isinstance(messages, list) or len(messages) < 3:
        errors.append(f"line {line_no}: messages must be a list with developer/user/model turns")
        return errors

    if not isinstance(tools, list) or not tools:
        errors.append(f"line {line_no}: tools must be a non-empty list")
        return errors

    roles = [m.get("role") for m in messages if isinstance(m, dict)]
    if roles[:3] != list(REQUIRED_ROLES):
        errors.append(f"line {line_no}: expected roles {REQUIRED_ROLES}, got {roles[:3]}")

    model_content = messages[2].get("content", "") if isinstance(messages[2], dict) else ""
    if not isinstance(model_content, str) or not model_content.strip():
        errors.append(f"line {line_no}: model turn content is empty")

    has_call = all(marker in model_content for marker in FUNCTION_CALL_MARKERS)
    if has_call and "call:" not in model_content:
        errors.append(f"line {line_no}: function call block missing call: prefix")

    for tool in tools:
        if not isinstance(tool, dict):
            errors.append(f"line {line_no}: tool entry must be an object")
            continue
        fn = tool.get("function") or {}
        if not fn.get("name"):
            errors.append(f"line {line_no}: tool missing function.name")

    return errors


def apply_template_check(rows: list[dict]) -> list[str]:
    try:
        from transformers import AutoTokenizer
    except ImportError:
        return ["transformers not installed; skip --apply-template or pip install transformers"]

    tokenizer = AutoTokenizer.from_pretrained("google/functiongemma-270m-it", trust_remote_code=True)
    errors: list[str] = []
    for idx, row in enumerate(rows[:5], start=1):
        try:
            rendered = tokenizer.apply_chat_template(
                row["messages"],
                tools=row["tools"],
                tokenize=False,
                add_generation_prompt=False,
            )
            if "<start_of_turn>developer" not in rendered:
                errors.append(f"sample {idx}: template missing developer turn")
            if "<start_of_turn>model" not in rendered:
                errors.append(f"sample {idx}: template missing model turn")
        except Exception as exc:
            errors.append(f"sample {idx}: apply_chat_template failed: {exc}")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate Unsloth FunctionGemma JSONL export")
    parser.add_argument("input", type=Path, help="JSONL file to validate")
    parser.add_argument("--apply-template", action="store_true", help="Render first 5 rows with HF tokenizer")
    args = parser.parse_args()

    rows: list[dict] = []
    errors: list[str] = []
    with args.input.open(encoding="utf-8") as handle:
        for line_no, line in enumerate(handle, start=1):
            if not line.strip():
                continue
            row = json.loads(line)
            rows.append(row)
            errors.extend(validate_row(row, line_no))

    if args.apply_template and rows:
        errors.extend(apply_template_check(rows))

    if errors:
        for err in errors:
            print(f"ERROR: {err}", file=sys.stderr)
        print(f"FAILED: {len(errors)} issue(s) in {len(rows)} rows", file=sys.stderr)
        return 1

    print(f"OK: {len(rows)} rows validated ({args.input})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
