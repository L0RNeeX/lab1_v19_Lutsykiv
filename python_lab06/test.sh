#!/bin/sh
cd "$(dirname "$0")" || exit 1
export PYTHONPATH="$(pwd)/src:$(cd ../python_lab06 && pwd)/src"
python3 -m pytest -v tests ../python_lab06/tests
