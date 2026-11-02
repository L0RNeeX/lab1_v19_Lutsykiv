#!/bin/sh
set -eu
cd "$(dirname "$0")"
PYTHONPATH=src${PYTHONPATH:+:$PYTHONPATH} python3 -m kzp_lab06.cli data/input.csv reports/summary.txt "$@"
