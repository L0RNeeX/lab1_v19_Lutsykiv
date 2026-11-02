#!/bin/sh
cd "$(dirname "$0")" || exit 1
export PYTHONPATH="$(pwd)/src"
python3 -m kzp_lab07.cli data/input.csv reports/summary.txt "$@"
