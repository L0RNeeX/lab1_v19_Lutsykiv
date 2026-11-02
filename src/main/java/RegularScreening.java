/** Звичайний кіносеанс без додаткової плати. */
public final class RegularScreening extends Screening {
    /** Створює звичайний сеанс із перевіркою спільних полів у Screening. */
    public RegularScreening(String film, int hall, double ticketPrice, int sold, int durationMin) {
        super(film, hall, ticketPrice, sold, durationMin);
    }

    /** Повертає звичайну категорію. */
    @Override
    public ScreeningKind getKind() { return ScreeningKind.REGULAR; }

    /** Виторг дорівнює вартості проданих квитків. */
    @Override
    public double revenue() { return getTicketSales().revenue(); }
}
