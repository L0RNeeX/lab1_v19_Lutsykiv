"""Aggregate cinema records with lists, dictionaries and sets."""

from datetime import datetime

Record = dict[str, object]


def selected_screenings(records: list[Record], threshold: int = 40) -> list[Record]:
    """Return screenings selling strictly more than threshold tickets."""
    return [record for record in records if record["sold"] > threshold]


def unique_films(records: list[Record]) -> set[str]:
    """Return distinct film titles; duplicates are removed by the set."""
    return {str(record["film"]) for record in records}


def revenue_by_hall(records: list[Record]) -> dict[int, float]:
    """Compute revenue per hall in ascending hall order."""
    halls = {int(record["hall"]) for record in records}
    return {
        hall: sum(
            float(item["price"]) * int(item["sold"])
            for item in records if item["hall"] == hall
        )
        for hall in sorted(halls)
    }


def most_popular(records: list[Record]) -> Record | None:
    """Choose highest sales, resolving ties by title and start time."""
    if not records:
        return None
    return min(records, key=lambda item: (
        -int(item["sold"]), str(item["film"]), item["start"]
    ))


def summarize(
    records: list[Record],
    *metrics: str,
    threshold: int = 40,
    **labels: str,
) -> dict[str, object]:
    """Create a summary; *metrics requests sums, **labels renames them."""
    selected = selected_screenings(records, threshold)
    result: dict[str, object] = {
        "count": len(records),
        "threshold": threshold,
        "selected_count": len(selected),
        "films": unique_films(records),
        "revenue_by_hall": revenue_by_hall(records),
        "popular": most_popular(records),
    }
    available = {
        "revenue": lambda row: float(row["price"]) * int(row["sold"]),
        "sold": lambda row: float(row["sold"]),
    }
    for metric in metrics:
        if metric not in available:
            raise ValueError(f"Невідома метрика: {metric}")
        result[labels.get(metric, metric)] = sum(available[metric](row) for row in records)
    return result
