"""Read and validate CSV records from the cinema dataset."""

import csv
import math
from datetime import datetime
from pathlib import Path

from .errors import DataError

COLUMNS = ("film", "hall", "start", "price", "sold")


def _number(row: dict[str, str | None], name: str, line: int, kind: type):
    """Convert one field, retaining its location in error messages."""
    raw = row.get(name)
    try:
        if raw is None or not raw.strip():
            raise ValueError("empty")
        value = kind(raw)
    except (TypeError, ValueError) as error:
        raise DataError(f"Рядок {line}: поле {name} має неправильне значення") from error
    if kind is float and not math.isfinite(value):
        raise DataError(f"Рядок {line}: поле {name} має бути скінченним числом")
    return value


def load_records(path: Path) -> list[dict[str, object]]:
    """Return a typed list of screenings without changing the source CSV."""
    try:
        with path.open("r", encoding="utf-8", newline="") as source:
            reader = csv.DictReader(source, strict=True)
            if reader.fieldnames != list(COLUMNS):
                raise DataError("Некоректний заголовок CSV: потрібні film,hall,start,price,sold")
            records: list[dict[str, object]] = []
            for line, row in enumerate(reader, start=2):
                # A quoted field can contain line breaks; reader.line_num is physical CSV line.
                line = reader.line_num
                if None in row or any(row.get(name) is None for name in COLUMNS):
                    raise DataError(f"Рядок {line}: неправильна кількість полів")
                film = str(row["film"]).strip()
                if not film:
                    raise DataError(f"Рядок {line}: поле film не може бути порожнім")
                hall = _number(row, "hall", line, int)
                sold = _number(row, "sold", line, int)
                price = _number(row, "price", line, float)
                try:
                    start = datetime.fromisoformat(str(row["start"]).strip())
                except ValueError as error:
                    raise DataError(f"Рядок {line}: поле start має мати формат YYYY-MM-DDTHH:MM") from error
                if hall <= 0:
                    raise DataError(f"Рядок {line}: hall має бути більше 0")
                if price < 0:
                    raise DataError(f"Рядок {line}: price не може бути від'ємним")
                if sold < 0:
                    raise DataError(f"Рядок {line}: sold не може бути від'ємним")
                records.append({"film": film, "hall": hall, "start": start,
                                "price": price, "sold": sold})
            return records
    except (OSError, UnicodeError, csv.Error) as error:
        raise DataError(f"Не вдалося прочитати CSV: {path}") from error
