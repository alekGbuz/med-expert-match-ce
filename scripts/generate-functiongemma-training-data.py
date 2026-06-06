#!/usr/bin/env python3
"""
Generate FunctionGemma fine-tuning rows (M58). Thin wrapper — prefer generate-tool-selection-eval-dataset.py for large sets.
"""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path


def main() -> None:
    script = Path(__file__).resolve().parent / "generate-tool-selection-eval-dataset.py"
    size = 33 * 3  # legacy default: ~99 rows at repeats=3 equivalent
    args = [
        sys.executable,
        str(script),
        "--size",
        str(size),
        "--train-csv",
        "target/functiongemma-train.csv",
        "--train-jsonl",
        "target/functiongemma-train.jsonl",
        "--eval-jsonl",
        "target/functiongemma-train-eval.jsonl",
    ]
    # Forward extra CLI args if provided
    if len(sys.argv) > 1:
        args = [sys.executable, str(script), *sys.argv[1:]]
    subprocess.run(args, check=True)


if __name__ == "__main__":
    main()
