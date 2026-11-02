"""Convert UTF-8 CSV directly into validated screening objects."""

import csv
from datetime import datetime
from pathlib import Path

from .errors import DataError
from .models import PremiumScreening, RegularScreening, Screening

COLUMNS = ("film", "hall", "start", "price", "sold")


def _parse_number(row: dict[str, str | None], name: str, line: int, kind: type):
    """Convert a numeric field, reporting its location in an input error."""
    raw = row.get(name)
    try:
        if raw is None or not raw.strip():
            raise ValueError("empty")
        return kind(raw)
    except (TypeError, ValueError) as error:
        raise DataError(f"Рядок {line}: поле {name} має неправильне значення") from error


def load_records(path: Path) -> list[Screening]:
    """Read the unchanged five-column CSV and return valid cinema objects."""
    try:
        with path.open("r", encoding="utf-8", newline="") as source:
            reader = csv.DictReader(source, strict=True)
            if reader.fieldnames != list(COLUMNS):
                raise DataError(
                    "Некоректний заголовок CSV: потрібні film,hall,start,price,sold"
                )
            records: list[Screening] = []
            for row in reader:
                line = reader.line_num
                if None in row or any(row.get(name) is None for name in COLUMNS):
                    raise DataError(f"Рядок {line}: неправильна кількість полів")
                film = str(row["film"]).strip()
                if not film:
                    raise DataError(f"Рядок {line}: поле film не може бути порожнім")
                hall = _parse_number(row, "hall", line, int)
                sold = _parse_number(row, "sold", line, int)
                price = _parse_number(row, "price", line, float)
                try:
                    start = datetime.fromisoformat(str(row["start"]).strip())
                except ValueError as error:
                    raise DataError(
                        f"Рядок {line}: поле start має мати формат YYYY-MM-DDTHH:MM"
                    ) from error
                # The VIP prefix is already part of the old CSV, so no new column is added.
                screening_type = PremiumScreening if film.startswith("VIP:") else RegularScreening
                try:
                    records.append(screening_type(film, hall, start, price, sold))
                except ValueError as error:
                    raise DataError(f"Рядок {line}: {error}") from error
            return records
    except (OSError, UnicodeError, csv.Error) as error:
        raise DataError(f"Не вдалося прочитати CSV: {path}") from error
