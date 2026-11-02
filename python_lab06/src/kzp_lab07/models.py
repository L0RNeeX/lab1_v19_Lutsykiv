"""Cinema entities, validated properties, immutable summary and shared protocol."""

from dataclasses import dataclass
from datetime import datetime
import math
from typing import Protocol


class RevenueSource(Protocol):
    """An object supporting the shared revenue operation."""

    def revenue(self) -> float:
        """Return revenue of the object."""
        ...


class Screening:
    """Base screening with validated mutable ticket sales and price."""

    def __init__(
        self, film: str, hall: int, start: datetime, price: float, sold: int
    ) -> None:
        """Create a screening after validating its common state."""
        if not isinstance(film, str) or not film.strip():
            raise ValueError("film не може бути порожнім")
        if type(hall) is not int or hall <= 0:
            raise ValueError("hall має бути додатним цілим числом")
        if not isinstance(start, datetime):
            raise ValueError("start має бути датою і часом")
        self._film = film.strip()
        self._hall = hall
        self._start = start
        self.price = price
        self.sold = sold

    @property
    def film(self) -> str:
        """Return the film title."""
        return self._film

    @property
    def hall(self) -> int:
        """Return the auditorium number."""
        return self._hall

    @property
    def start(self) -> datetime:
        """Return the starting date and time."""
        return self._start

    @property
    def price(self) -> float:
        """Return the final ticket price from the CSV."""
        return self._price

    @price.setter
    def price(self, value: float) -> None:
        """Accept only nonnegative finite numeric ticket prices."""
        if type(value) not in (int, float) or not math.isfinite(value) or value < 0:
            raise ValueError("price має бути скінченним числом не менше 0")
        self._price = float(value)

    @property
    def sold(self) -> int:
        """Return the number of tickets sold."""
        return self._sold

    @sold.setter
    def sold(self, value: int) -> None:
        """Validate ticket sales, including updates after initialization."""
        if type(value) is not int or value < 0:
            raise ValueError("sold має бути цілим числом не менше 0")
        self._sold = value

    def revenue(self) -> float:
        """Define the operation required of concrete screening types."""
        raise NotImplementedError

    def __str__(self) -> str:
        """Return concise human-readable screening information."""
        return f"{self.film}: зал {self.hall}, {self.sold} квитків"

    def __repr__(self) -> str:
        """Return a useful debugging representation."""
        return (
            f"{type(self).__name__}(film={self.film!r}, hall={self.hall!r}, "
            f"start={self.start.isoformat(timespec='minutes')!r}, "
            f"price={self.price!r}, sold={self.sold!r})"
        )

    def __eq__(self, other: object) -> bool:
        """Compare concrete screening type and all record fields."""
        if not isinstance(other, Screening):
            return NotImplemented
        return type(self) is type(other) and (
            self.film, self.hall, self.start, self.price, self.sold
        ) == (other.film, other.hall, other.start, other.price, other.sold)


class RegularScreening(Screening):
    """Standard screening without a premium component in its price."""

    def revenue(self) -> float:
        """Calculate sold tickets multiplied by the standard price."""
        return self.price * self.sold


class PremiumScreening(Screening):
    """Premium screening whose CSV price already includes a 20% service component."""

    PREMIUM_FACTOR = 1.20

    def revenue(self) -> float:
        """Separate and restore the included surcharge; never bill it twice."""
        base_price = self.price / self.PREMIUM_FACTOR
        surcharge = base_price * (self.PREMIUM_FACTOR - 1)
        return round((base_price + surcharge) * self.sold, 2)


@dataclass(frozen=True)
class CinemaSummary:
    """Immutable numeric totals calculated from screening objects."""

    count: int
    threshold: int
    selected_count: int
    total_revenue: float
    unique_film_count: int
