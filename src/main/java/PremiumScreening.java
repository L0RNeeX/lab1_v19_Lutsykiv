/** Преміум-сеанс із додатковою оплатою сервісу у 20%. */
public final class PremiumScreening extends Screening {
    private static final double PREMIUM_RATE = 0.20;

    /** Створює преміум-сеанс; price у CSV означає базову ціну без доплати. */
    public PremiumScreening(String film, int hall, double ticketPrice, int sold, int durationMin) {
        super(film, hall, ticketPrice, sold, durationMin);
    }

    /** Повертає преміальну категорію. */
    @Override
    public ScreeningKind getKind() { return ScreeningKind.PREMIUM; }

    /** Додає 20% сервісного збору до базового виторгу. */
    @Override
    public double revenue() {
        double base = getTicketSales().revenue();
        return base + base * PREMIUM_RATE;
    }
}
