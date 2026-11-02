"""Calculate cinema totals through a shared object contract."""

from collections.abc import Iterable

from .models import CinemaSummary, RevenueSource, Screening


def total_revenue(sources: Iterable[RevenueSource]) -> float:
    """Call the polymorphic operation without checking concrete types."""
    return sum(item.revenue() for item in sources)


def selected_screenings(records: list[Screening], threshold: int = 40) -> list[Screening]:
    """Return screenings whose sales exceed threshold."""
    return [screening for screening in records if screening.sold > threshold]


def unique_films(records: list[Screening]) -> set[str]:
    """Get unique titles using a set comprehension."""
    return {screening.film for screening in records}


def revenue_by_hall(records: list[Screening]) -> dict[int, float]:
    """Calculate hall revenue without branching on concrete screening type."""
    halls = {screening.hall for screening in records}
    return {
        hall: total_revenue(screening for screening in records if screening.hall == hall)
        for hall in sorted(halls)
    }


def most_popular(records: list[Screening]) -> Screening | None:
    """Choose most sold tickets, then title and time in case of equality."""
    return min(
        records,
        key=lambda item: (-item.sold, item.film, item.start),
        default=None,
    )


def summarize(records: list[Screening], threshold: int = 40) -> dict[str, object]:
    """Return a frozen numeric summary plus details for legacy report rendering."""
    filtered = selected_screenings(records, threshold)
    films = unique_films(records)
    revenues = revenue_by_hall(records)
    popular = most_popular(records)
    totals = CinemaSummary(
        count=len(records),
        threshold=threshold,
        selected_count=len(filtered),
        total_revenue=total_revenue(records),
        unique_film_count=len(films),
    )
    return {
        "totals": totals,
        "films": films,
        "revenue_by_hall": revenues,
        "popular": popular,
    }
