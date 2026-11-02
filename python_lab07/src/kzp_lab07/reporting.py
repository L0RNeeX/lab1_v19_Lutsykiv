"""Create the same external report as laboratory 6, with an updated title."""

from pathlib import Path


def format_report(summary: dict[str, object]) -> str:
    """Render ordered lines and explicit empty-input placeholders."""
    totals = summary["totals"]
    popular = summary["popular"]
    if popular is None:
        popular_text = "немає даних"
    else:
        popular_text = (
            f"{popular.film} | {popular.start.strftime('%Y-%m-%dT%H:%M')}"
            f" | {popular.sold} квитків"
        )
    films = summary["films"]
    by_hall = summary["revenue_by_hall"]
    lines = [
        "Лабораторна робота № 7 — Кінотеатр (варіант 19)",
        f"Записів: {totals.count}",
        f"Поріг продажів: понад {totals.threshold} квитків",
        f"Сеансів понад поріг: {totals.selected_count}",
        f"Загальний виторг: {totals.total_revenue:.2f} грн",
        f"Найпопулярніший сеанс: {popular_text}",
        f"Унікальних фільмів: {totals.unique_film_count}",
        f"Фільми: {', '.join(sorted(films)) if films else 'немає даних'}",
        "Виторг за залами:",
    ]
    if not by_hall:
        lines.append("  немає даних")
    for hall, revenue in sorted(by_hall.items()):
        lines.append(f"  Зал {hall}: {revenue:.2f} грн")
    return "\n".join(lines) + "\n"


def write_report(path: Path, report: str) -> None:
    """Save text through with and explicitly UTF-8, without modifying source data."""
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as output:
        output.write(report)
