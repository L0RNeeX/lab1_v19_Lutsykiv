"""Tests for cinema CSV input, analysis, report and CLI."""

import csv
from datetime import datetime
from pathlib import Path

import pytest

from kzp_lab06.analysis import (
    most_popular, revenue_by_hall, selected_screenings, summarize, unique_films,
)
from kzp_lab06.cli import main
from kzp_lab06.errors import DataError
from kzp_lab06.loader import load_records
from kzp_lab06.reporting import format_report, write_report


def _write_csv(path: Path, rows: list[list[str | int | float]]) -> None:
    with path.open('w', encoding='utf-8', newline='') as target:
        writer = csv.writer(target)
        writer.writerow(['film', 'hall', 'start', 'price', 'sold'])
        writer.writerows(rows)


def test_sample_csv_uses_utf8_and_types() -> None:
    path = Path(__file__).parent.parent / 'data' / 'input.csv'
    records = load_records(path)
    assert len(records) == 7
    assert records[3]['film'] == 'Космос, нова ера'
    assert records[4]['film'] == 'Фільм "Повернення"'
    assert records[0]['start'] == datetime(2026, 11, 3, 10)
    assert records[0]['hall'] == 1
    assert records[0]['price'] == 150.0


def test_filter_threshold_including_boundary() -> None:
    records = load_records(Path(__file__).parent.parent / 'data' / 'input.csv')
    assert [r['sold'] for r in selected_screenings(records)] == [80, 60, 50]
    assert len(selected_screenings(records, threshold=80)) == 0


def test_set_removes_duplicate_title() -> None:
    records = load_records(Path(__file__).parent.parent / 'data' / 'input.csv')
    assert len(unique_films(records)) == 6


def test_aggregates_and_popular_session() -> None:
    records = load_records(Path(__file__).parent.parent / 'data' / 'input.csv')
    assert revenue_by_hall(records) == {1: 21000.0, 2: 14400.0, 3: 6400.0}
    assert most_popular(records)['film'] == 'Місто світла'
    result = summarize(records, 'revenue', 'sold', threshold=40, revenue='Виторг')
    assert result['Виторг'] == 41800.0
    assert result['sold'] == 285.0  # 80+60+50+40+0+15+40
    assert result['selected_count'] == 3


def test_empty_input_with_header(tmp_path: Path) -> None:
    file = tmp_path / 'empty.csv'
    _write_csv(file, [])
    records = load_records(file)
    assert records == []
    result = summarize(records, 'revenue')
    assert result['revenue'] == 0
    assert result['popular'] is None
    assert result['revenue_by_hall'] == {}
    assert 'Найпопулярніший сеанс: немає даних' in format_report(result)


def test_rejects_missing_header(tmp_path: Path) -> None:
    file = tmp_path / 'missing.csv'
    file.write_text('', encoding='utf-8')
    with pytest.raises(DataError, match='заголовок'):
        load_records(file)


def test_rejects_wrong_header(tmp_path: Path) -> None:
    file = tmp_path / 'bad.csv'
    file.write_text('film,start,hall,price,sold\n', encoding='utf-8')
    with pytest.raises(DataError, match='заголовок'):
        load_records(file)


def test_rejects_invalid_price_with_cause(tmp_path: Path) -> None:
    file = tmp_path / 'invalid.csv'
    _write_csv(file, [['Тест',1,'2026-11-03T10:00','abc',5]])
    with pytest.raises(DataError, match='Рядок 2: поле price') as exc:
        load_records(file)
    assert isinstance(exc.value.__cause__, ValueError)


@pytest.mark.parametrize('name,value', [('hall', -1), ('sold', -5), ('price', -2)])
def test_rejects_negative_values(tmp_path: Path, name: str, value: int) -> None:
    file = tmp_path / 'bad-values.csv'
    row = {'film': 'Тест','hall': 1,'start': '2026-11-03T10:00','price': 20,'sold': 4}
    row[name] = value
    _write_csv(file, [list(row.values())])
    with pytest.raises(DataError, match=name):
        load_records(file)


def test_rejects_missing_cell(tmp_path: Path) -> None:
    path = tmp_path / 'bad.csv'
    path.write_text('film,hall,start,price,sold\nТест,1,2026-11-03T10:00,50\n', encoding='utf-8')
    with pytest.raises(DataError, match='кількість полів'):
        load_records(path)


def test_rejects_extra_cell(tmp_path: Path) -> None:
    path = tmp_path / 'bad.csv'
    path.write_text('film,hall,start,price,sold\nТест,1,2026-11-03T10:00,50,4,extra\n', encoding='utf-8')
    with pytest.raises(DataError, match='кількість полів'):
        load_records(path)


def test_unrecognized_summary_metric_is_error() -> None:
    with pytest.raises(ValueError, match='Невідома метрика'):
        summarize([], 'unknown')


def test_write_report_utf8_and_cli(tmp_path: Path) -> None:
    source = Path(__file__).parent.parent/'data'/'input.csv'
    target = tmp_path / 'new' / 'result.txt'
    assert main([str(source), str(target)]) == 0
    content = target.read_text(encoding='utf-8')
    assert 'Загальний виторг: 41800.00 грн' in content
    assert 'Унікальних фільмів: 6' in content
    assert 'Зал 3: 6400.00 грн' in content


def test_cli_invalid_data_returns_error_code(tmp_path: Path, capsys) -> None:
    source = Path(__file__).parent.parent/'data'/'invalid.csv'
    output = tmp_path / 'error-report.txt'
    assert main([str(source), str(output)]) == 2
    assert not output.exists()
    assert 'Рядок 2' in capsys.readouterr().err


def test_cli_does_not_overwrite_input(tmp_path: Path) -> None:
    source = tmp_path / 'input.csv'
    _write_csv(source, [['Тест',1,'2026-11-03T10:00',150,20]])
    before = source.read_bytes()
    assert main([str(source), str(source)]) == 2
    assert source.read_bytes() == before


def test_write_report_uses_newline_and_unicode(tmp_path: Path) -> None:
    target = tmp_path / 'output.txt'
    write_report(target, 'Кінотеатр\nУспіх\n')
    assert target.read_bytes() == 'Кінотеатр\nУспіх\n'.encode('utf-8')
