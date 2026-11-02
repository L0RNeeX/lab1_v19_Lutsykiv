"""Create and save a deterministic UTF-8 cinema report."""

from datetime import datetime
from pathlib import Path


def format_report(summary: dict[str, object]) -> str:
    """Return stable lines, with explicit text for empty input."""
    popular = summary["popular"]
    if popular is None:
        popular_text = "немає даних"
    else:
        popular_text = (
            f"{popular['film']} | {popular['start'].strftime('%Y-%m-%dT%H:%M')}"
            f" | {popular['sold']} квитків"
        )
    lines = [
        "Лабораторна робота № 6 — Кінотеатр (варіант 19)",
        f"Записів: {summary['count']}",
        f"Поріг продажів: понад {summary['threshold']} квитків",
        f"Сеансів понад поріг: {summary['selected_count']}",
        f"Загальний виторг: {summary['revenue']:.2f} грн",
        f"Найпопулярніший сеанс: {popular_text}",
        f"Унікальних фільмів: {len(summary['films'])}",
        f"Фільми: {', '.join(sorted(summary['films'])) if summary['films'] else 'немає даних'}",
        "Виторг за залами:",
    ]
    if not summary["revenue_by_hall"]:
        lines.append("  немає даних")
    for hall, revenue in sorted(summary["revenue_by_hall"].items()):
        lines.append(f"  Зал {hall}: {revenue:.2f} грн")
    return "\n".join(lines) + "\n"


def write_report(path: Path, report: str) -> None:
    """Write a report through with and Path.open in explicit UTF-8."""
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as target:
        target.write(report)
