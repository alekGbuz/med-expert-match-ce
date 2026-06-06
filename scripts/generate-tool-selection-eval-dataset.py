#!/usr/bin/env python3
"""
Generate large synthetic tool-selection datasets for FunctionGemma eval and fine-tuning (M58).

Outputs:
  - Eval JSONL (live golden eval / policy extension) — same schema as tool-selection-golden.jsonl
  - Train CSV (FunctionGemma Tuning Lab)
  - Optional train/val/test splits

Examples:
  # Large live-eval dataset (500 cases)
  python scripts/generate-tool-selection-eval-dataset.py --size 500 \\
    --eval-jsonl target/eval/tool-selection-large.jsonl

  # Full fine-tune pack per M58 (400-600 train + splits)
  python scripts/generate-tool-selection-eval-dataset.py --size 600 --split \\
    --train-csv target/functiongemma-train.csv \\
    --val-jsonl target/functiongemma-val.jsonl \\
    --test-jsonl target/functiongemma-test.jsonl
"""

from __future__ import annotations

import argparse
import csv
import json
import random
import sys
from pathlib import Path

# Allow import when run from repo root
sys.path.insert(0, str(Path(__file__).resolve().parent))
from tool_selection_dataset_lib import (  # noqa: E402
    SCENARIOS,
    iter_balanced_rows,
    split_rows,
    to_train_row,
)

DEFAULT_CASE_IDS = [
    "6a23f05200155d711484cf69",
    "a1b2c3d4e5f6789012345678",
    "6a1db20e86d74aa336e98ff0",
    "b2c3d4e5f6789012345678a1",
    "c3d4e5f6789012345678a1b2",
]


def write_eval_jsonl(path: Path, rows: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        for row in rows:
            handle.write(json.dumps(row, ensure_ascii=False) + "\n")


def write_train_csv(path: Path, rows: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fields = ["user_prompt", "tool_name", "tool_args_json", "locale", "scenario"]
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        for row in rows:
            writer.writerow({k: row[k] for k in fields})


def build_eval_rows(
    rng: random.Random,
    size: int,
    min_per_scenario: int,
    case_id_pool: list[str],
) -> list[dict]:
    rows = list(
        iter_balanced_rows(
            rng,
            size,
            min_per_scenario=min_per_scenario,
            case_id_pool=case_id_pool,
        )
    )
    rng.shuffle(rows)
    return rows


def eval_to_train_rows(eval_rows: list[dict]) -> list[dict]:
    scenario_by_id = {s.id: s for s in SCENARIOS}
    train_rows: list[dict] = []
    for row in eval_rows:
        scenario = scenario_by_id[row["scenario"]]
        train_rows.append(
            to_train_row(
                scenario,
                row["userMessage"],
                row.get("caseId"),
            )
        )
    return train_rows


def print_summary(rows: list[dict], label: str) -> None:
    by_scenario: dict[str, int] = {}
    ru = 0
    no_tool = 0
    for row in rows:
        by_scenario[row["scenario"]] = by_scenario.get(row["scenario"], 0) + 1
        if row.get("locale") == "ru":
            ru += 1
        if not row.get("expectedTool"):
            no_tool += 1
    print(f"{label}: {len(rows)} rows")
    print(f"  RU share: {ru / len(rows):.1%}" if rows else "")
    print(f"  No-tool (negative): {no_tool / len(rows):.1%}" if rows else "")
    print("  Per scenario:")
    for key in sorted(by_scenario):
        print(f"    {key}: {by_scenario[key]}")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Generate large FunctionGemma tool-selection eval/training datasets"
    )
    parser.add_argument("--size", type=int, default=500, help="Total eval rows to generate")
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument(
        "--min-per-scenario",
        type=int,
        default=20,
        help="Minimum examples per scenario (M58 recommends 20+)",
    )
    parser.add_argument(
        "--eval-jsonl",
        type=Path,
        default=Path("target/eval/tool-selection-large.jsonl"),
        help="Eval JSONL for live FunctionGemma test",
    )
    parser.add_argument("--train-csv", type=Path, default=None, help="Optional Tuning Lab CSV")
    parser.add_argument("--train-jsonl", type=Path, default=None, help="Optional full train JSONL")
    parser.add_argument("--val-jsonl", type=Path, default=None, help="Validation split JSONL")
    parser.add_argument("--test-jsonl", type=Path, default=None, help="Test split JSONL")
    parser.add_argument(
        "--split",
        action="store_true",
        help="Write train/val/test splits (80/10/10) when val/test paths set",
    )
    parser.add_argument(
        "--train-ratio",
        type=float,
        default=0.8,
        help="Train split ratio when --split is used",
    )
    parser.add_argument(
        "--val-ratio",
        type=float,
        default=0.1,
        help="Validation split ratio when --split is used",
    )
    args = parser.parse_args()

    rng = random.Random(args.seed)
    min_required = args.min_per_scenario * len(SCENARIOS)
    if args.size < min_required:
        print(
            f"Note: --size {args.size} is below {min_required} "
            f"(min-per-scenario {args.min_per_scenario} × {len(SCENARIOS)} scenarios). "
            f"Using ~{max(1, args.size // len(SCENARIOS))} per scenario.",
            file=sys.stderr,
        )
    eval_rows = build_eval_rows(
        rng, args.size, args.min_per_scenario, DEFAULT_CASE_IDS
    )
    write_eval_jsonl(args.eval_jsonl, eval_rows)
    print_summary(eval_rows, f"Wrote eval JSONL -> {args.eval_jsonl}")

    train_rows = eval_to_train_rows(eval_rows)

    if args.train_csv:
        write_train_csv(args.train_csv, train_rows)
        print(f"Wrote train CSV -> {args.train_csv} ({len(train_rows)} rows)")

    if args.train_jsonl and not args.split:
        write_eval_jsonl(args.train_jsonl, eval_rows)
        print(f"Wrote train JSONL -> {args.train_jsonl}")

    if args.split:
        train_eval, val_eval, test_eval = split_rows(
            eval_rows, args.train_ratio, args.val_ratio
        )
        train_split = eval_to_train_rows(train_eval)
        if args.train_csv:
            write_train_csv(args.train_csv, train_split)
        if args.train_jsonl:
            write_eval_jsonl(args.train_jsonl, train_eval)
        if args.val_jsonl:
            write_eval_jsonl(args.val_jsonl, val_eval)
            print(f"Wrote val JSONL -> {args.val_jsonl} ({len(val_eval)} rows)")
        if args.test_jsonl:
            write_eval_jsonl(args.test_jsonl, test_eval)
            print(f"Wrote test JSONL -> {args.test_jsonl} ({len(test_eval)} rows)")
        print_summary(train_eval, "Train split")
        if args.val_jsonl:
            print_summary(val_eval, "Val split")
        if args.test_jsonl:
            print_summary(test_eval, "Test split")


if __name__ == "__main__":
    main()
