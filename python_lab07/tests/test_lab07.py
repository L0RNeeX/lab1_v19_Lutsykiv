"""Behavioral tests of object-oriented cinema model and legacy compatibility."""

from dataclasses import FrozenInstanceError
from datetime import datetime
from pathlib import Path
import csv

import pytest

from kzp_lab07.analysis import (
    most_popular,
    revenue_by_hall,
    selected_screenings,
    summarize,
    total_revenue,
    unique_films,
)
from kzp_lab07.cli import main
from kzp_lab07.errors import DataError
from kzp_lab07.loader import load_records
from kzp_lab07.models import (
    CinemaSummary,
    PremiumScreening,
    RegularScreening,
    RevenueSource,
    Screening,
)
from kzp_lab07.reporting import format_report, write_report

ROOT = Path(__file__).resolve().parents[1]
START = datetime(2026, 11, 3, 10, 0)


def regular(**overrides):
    params = dict(film="Тест", hall=1, start=START, price=150.0, sold=4)
    params.update(overrides)
    return RegularScreening(**params)


def premium(**overrides):
    params = dict(film="VIP: Тест", hall=1, start=START, price=120.0, sold=4)
    params.update(overrides)
    return PremiumScreening(**params)


def write_input(path, rows):
    with path.open("w", encoding="utf-8", newline="") as target:
        writer = csv.writer(target)
        writer.writerow(["film", "hall", "start", "price", "sold"])
        writer.writerows(rows)


def test_loader_returns_objects_of_both_subtypes():
    records = load_records(ROOT / "data" / "input.csv")
    assert len(records) == 7
    assert sum(type(record) is PremiumScreening for record in records) == 1
    assert sum(type(record) is RegularScreening for record in records) == 6
    assert records[3].film == "Космос, нова ера"
    assert records[4].film == 'Фільм "Повернення"'


def test_regular_constructor_getters_and_string():
    item = regular()
    assert item.film == "Тест" and item.hall == 1 and item.start == START
    assert item.price == 150.0 and item.sold == 4
    assert "4 квитків" in str(item)
    assert "RegularScreening" in repr(item)


def test_setter_accepts_boundary_zero_and_updates_revenue():
    item = regular(sold=6)
    item.sold = 0
    item.price = 0
    assert item.revenue() == 0


@pytest.mark.parametrize("invalid", [-1, 1.5, "abc", True, None])
def test_sold_setter_rejects_invalid_values_without_state_change(invalid):
    item = regular(sold=4)
    with pytest.raises(ValueError, match="sold"):
        item.sold = invalid
    assert item.sold == 4


@pytest.mark.parametrize("invalid", [-1.0, float("nan"), float("inf"), "120", True])
def test_price_setter_rejects_invalid_values_without_state_change(invalid):
    item = regular(price=150)
    with pytest.raises(ValueError, match="price"):
        item.price = invalid
    assert item.price == 150.0


@pytest.mark.parametrize(
    "overrides", [
        {"film": "  "},
        {"film": None},
        {"hall": 0},
        {"hall": -5},
        {"start": "2026-11-03T10:00"},
    ]
)
def test_constructor_rejects_invalid_common_state(overrides):
    with pytest.raises(ValueError):
        regular(**overrides)


def test_polymorphic_revenue_differs_by_subtype_implementation():
    standard = regular(price=100, sold=4)
    premium_item = premium(price=120, sold=4)
    assert standard.revenue() == 400
    assert premium_item.revenue() == 480
    assert total_revenue([standard, premium_item]) == 880


def test_premium_price_already_contains_surcharge():
    item = premium(price=200.00, sold=15)
    assert item.revenue() == 3000


def test_protocol_accepts_other_object_by_duck_typing():
    class OtherRevenue:
        def revenue(self) -> float:
            return 12.5

    value = OtherRevenue()
    assert value.revenue() == 12.5
    assert total_revenue([regular(price=100, sold=1), value]) == 112.5


def test_equality_uses_type_and_all_fields():
    assert regular() == regular()
    assert regular() != regular(sold=5)
    assert regular() != premium(film="Тест", price=150, sold=4)
    assert regular() != "Тест"


def test_frozen_dataclass_value_equality():
    first = CinemaSummary(2, 40, 1, 150.0, 2)
    assert first == CinemaSummary(2, 40, 1, 150.0, 2)
    with pytest.raises(FrozenInstanceError):
        first.count = 6


def test_analysis_preserves_legacy_totals_and_hall_breakdown():
    records = load_records(ROOT / "data" / "input.csv")
    totals = summarize(records)["totals"]
    assert totals == CinemaSummary(7, 40, 3, 41800.0, 6)
    assert revenue_by_hall(records) == {1: 21000, 2: 14400, 3: 6400}
    assert [item.sold for item in selected_screenings(records)] == [80, 60, 50]
    assert len(unique_films(records)) == 6
    assert most_popular(records).film == "Місто світла"


def test_output_same_as_lab06_except_number_in_title():
    records = load_records(ROOT / "data" / "input.csv")
    old = (ROOT / "data" / "lab06-baseline-report.txt").read_text(encoding="utf-8")
    new = format_report(summarize(records))
    assert old.splitlines()[1:] == new.splitlines()[1:]
    assert new.startswith("Лабораторна робота № 7")


def test_empty_csv_preserves_empty_semantics():
    records = load_records(ROOT / "data" / "empty.csv")
    assert records == []
    summary = summarize(records)
    assert summary["totals"] == CinemaSummary(0, 40, 0, 0.0, 0)
    assert summary["popular"] is None
    assert revenue_by_hall(records) == {}
    assert "Найпопулярніший сеанс: немає даних" in format_report(summary)


def test_duplicate_titles_remain_unique_only_in_set():
    records = load_records(ROOT / "data" / "input.csv")
    assert len(records) == 7
    assert len({record.film for record in records}) == 6


def test_bad_csv_reports_field_and_cause(tmp_path):
    path = tmp_path / "bad.csv"
    write_input(path, [["Тест", 1, "2026-11-03T10:00", "abc", 4]])
    with pytest.raises(DataError, match="Рядок 2: поле price") as exc:
        load_records(path)
    assert isinstance(exc.value.__cause__, ValueError)


def test_bad_csv_invalid_values_use_validation(tmp_path):
    path = tmp_path / "bad.csv"
    write_input(path, [["Тест", 1, "2026-11-03T10:00", 10, -4]])
    with pytest.raises(DataError, match="Рядок 2: sold"):
        load_records(path)


def test_bad_csv_header_and_missing_cell(tmp_path):
    path = tmp_path / "bad.csv"
    path.write_text("film,price\n", encoding="utf-8")
    with pytest.raises(DataError, match="заголовок"):
        load_records(path)
    path.write_text("film,hall,start,price,sold\nФільм,1,2026-11-03T10:00,100\n", encoding="utf-8")
    with pytest.raises(DataError, match="кількість полів"):
        load_records(path)


def test_cli_writes_utf8_and_returns_success(tmp_path):
    out = tmp_path / "reports" / "summary.txt"
    assert main([str(ROOT / "data" / "input.csv"), str(out)]) == 0
    content = out.read_text(encoding="utf-8")
    assert "Загальний виторг: 41800.00 грн" in content
    assert "Виторг за залами:" in content


def test_cli_rejects_invalid_csv_and_preserves_input(tmp_path):
    path = ROOT / "data" / "invalid.csv"
    before = path.read_bytes()
    output = tmp_path / "output.txt"
    assert main([str(path), str(output)]) == 2
    assert path.read_bytes() == before and not output.exists()


def test_cli_rejects_same_input_and_output(tmp_path):
    path = tmp_path / "input.csv"
    write_input(path, [["Тест", 1, "2026-11-03T10:00", 30, 1]])
    before = path.read_bytes()
    assert main([str(path), str(path)]) == 2
    assert path.read_bytes() == before


def test_write_report_utf8_lf(tmp_path):
    path = tmp_path / "out" / "report.txt"
    write_report(path, "Кінотеатр\nЗвіт\n")
    assert path.read_bytes() == "Кінотеатр\nЗвіт\n".encode("utf-8")
