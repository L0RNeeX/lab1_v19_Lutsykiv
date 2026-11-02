"""Command line entry point for variant 19."""

import argparse
import sys
from pathlib import Path

from .analysis import summarize
from .errors import DataError
from .loader import load_records
from .reporting import format_report, write_report


def main(argv: list[str] | None = None) -> int:
    """Read input CSV, write output report and return shell exit code."""
    parser = argparse.ArgumentParser(description="Звіт кінотеатру, варіант 19")
    parser.add_argument("input_csv", type=Path, help="CSV із полями film,hall,start,price,sold")
    parser.add_argument("output_report", type=Path, help="шлях до UTF-8 звіту")
    parser.add_argument("--threshold", type=int, default=40, help="поріг продажів, типово 40")
    args = parser.parse_args(argv)
    if args.threshold < 0:
        print("Помилка: threshold не може бути від'ємним", file=sys.stderr)
        return 2
    try:
        records = load_records(args.input_csv)
        summary = summarize(records, "revenue", threshold=args.threshold)
        report = format_report(summary)
        if args.input_csv.resolve() == args.output_report.resolve():
            raise DataError("Вхідний і вихідний файли не можуть збігатися")
        write_report(args.output_report, report)
    except (DataError, OSError) as error:
        print(f"Помилка даних: {error}", file=sys.stderr)
        return 2
    print(report, end="")
    print(f"Звіт записано: {args.output_report}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
