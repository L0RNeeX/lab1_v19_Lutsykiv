import java.util.Locale;
import java.util.Objects;

/** Спільна незмінна модель кіносеансу варіанта 19. */
public abstract class Screening {
    private final String film;
    private final int hall;
    private final TicketSales ticketSales;
    private final int durationMin;

    /** Перевіряє спільні поля; створення доступне тільки підтипам. */
    protected Screening(String film, int hall, double ticketPrice, int sold, int durationMin) {
        this.film = validateFilm(film);
        this.hall = validatePositive(hall, "hall");
        this.ticketSales = new TicketSales(ticketPrice, sold);
        this.durationMin = validatePositive(durationMin, "durationMin");
    }

    /**
     * Створює потрібний підтип із п'яти CSV-полів.
     * Назви з префіксом VIP: означають преміум-сеанс у нових даних.
     * Старі рядки без префікса обробляються як звичайні, без зміни виторгу.
     *
     * @param line рядок film;hall;ticketPrice;sold;durationMin
     * @return перевірений сеанс конкретного підтипу
     */
    public static Screening fromCsv(String line) {
        Objects.requireNonNull(line, "Рядок не може бути null");
        if (line.isBlank()) {
            throw new IllegalArgumentException("порожній рядок");
        }
        String[] fields = line.split(";", -1);
        if (fields.length != 5) {
            throw new IllegalArgumentException("очікується 5 полів, отримано %d".formatted(fields.length));
        }
        for (int index = 0; index < fields.length; index++) {
            fields[index] = fields[index].strip();
        }
        String film = validateFilm(fields[0]);
        int hall = validatePositive(parseInteger(fields[1], "hall"), "hall");
        double price = TicketSales.validatePrice(parsePrice(fields[2]));
        int sold = TicketSales.validateSold(parseInteger(fields[3], "sold"));
        int duration = validatePositive(parseInteger(fields[4], "durationMin"), "durationMin");
        if (film.startsWith("VIP:")) {
            return new PremiumScreening(film, hall, price, sold, duration);
        }
        return new RegularScreening(film, hall, price, sold, duration);
    }

    private static String validateFilm(String value) {
        String text = Objects.requireNonNull(value, "Назва фільму не може бути null").strip();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("поле \"film\" не може бути порожнім");
        }
        return text;
    }

    private static int validatePositive(int value, String field) {
        if (value < 1) {
            throw new IllegalArgumentException("поле \"%s\" має бути не менше 1".formatted(field));
        }
        return value;
    }

    private static int parseInteger(String text, String field) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "поле \"%s\" має бути цілим числом у межах int".formatted(field), exception);
        }
    }

    private static double parsePrice(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "поле \"ticketPrice\" має бути числом із крапкою", exception);
        }
    }

    /** Повертає назву фільму. */
    public final String getFilm() { return film; }
    /** Повертає номер залу. */
    public final int getHall() { return hall; }
    /** Повертає початкову ціну квитка. */
    public final double getTicketPrice() { return ticketSales.ticketPrice(); }
    /** Повертає кількість проданих квитків. */
    public final int getSold() { return ticketSales.sold(); }
    /** Повертає тривалість сеансу. */
    public final int getDurationMin() { return durationMin; }
    /** Повертає незмінні відомості про продажі. */
    public final TicketSales getTicketSales() { return ticketSales; }

    /** Повертає категорію конкретного кіносеансу. */
    public abstract ScreeningKind getKind();

    /** Обчислює виторг відповідно до правил конкретного підтипу. */
    public abstract double revenue();

    /** Порівнює повний стан і конкретний підтип, а не тільки назву фільму. */
    @Override
    public final boolean equals(Object other) {
        if (this == other) { return true; }
        if (other == null || getClass() != other.getClass()) { return false; }
        Screening that = (Screening) other;
        return hall == that.hall && durationMin == that.durationMin
                && film.equals(that.film) && ticketSales.equals(that.ticketSales);
    }

    /** Хеш використовує ті самі поля, що й equals. */
    @Override
    public final int hashCode() {
        return Objects.hash(getClass(), film, hall, ticketSales, durationMin);
    }

    /** Повертає незмінне текстове подання старого формату. */
    @Override
    public final String toString() {
        return String.format(Locale.ROOT, "%s: зал %d; %.2f грн; продано %d; %d хв",
                film, hall, getTicketPrice(), getSold(), durationMin);
    }
}
