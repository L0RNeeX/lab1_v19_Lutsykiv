"""Command-line entry point for the cinema object model."""

import argparse
from pathlib import Path
import sys

from .analysis import summarize
from .errors import DataError
from .loader import load_records
from .reporting import format_report, write_report


def main(argv: list[str] | None = None) -> int:
    """Read CSV, calculate a report and return a shell exit code."""
    parser = argparse.ArgumentParser(description="Кінотеатр, лабораторна 7, варіант 19")
    parser.add_argument("input_csv", type=Path)
    parser.add_argument("output_report", type=Path)
    parser.add_argument("--threshold", type=int, default=40)
    args = parser.parse_args(argv)
    if args.threshold < 0:
        print("Помилка: threshold не може бути від'ємним", file=sys.stderr)
        return 2
    try:
        if args.input_csv.resolve() == args.output_report.resolve():
            raise DataError("Вхідний і вихідний файли не можуть збігатися")
        records = load_records(args.input_csv)
        text = format_report(summarize(records, threshold=args.threshold))
        write_report(args.output_report, text)
    except (DataError, OSError) as error:
        print(f"Помилка даних: {error}", file=sys.stderr)
        return 2
    print(text, end="")
    print(f"Звіт записано: {args.output_report}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
