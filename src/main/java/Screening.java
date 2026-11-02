import java.util.Locale;
import java.util.Objects;

/**
 * Незмінний кіносеанс варіанта 19.
 * CSV: film;hall;ticketPrice;sold;durationMin.
 */
public final class Screening {
    private final String film;
    private final int hall;
    private final TicketSales ticketSales;
    private final int durationMin;

    /**
     * Створює кіносеанс після перевірки всіх інваріантів.
     *
     * @param film непорожня назва фільму
     * @param hall номер залу, більший за нуль, як у роботі № 1
     * @param ticketPrice скінченна невід'ємна ціна
     * @param sold невід'ємна кількість проданих квитків
     * @param durationMin додатна тривалість у хвилинах
     * @throws NullPointerException якщо film дорівнює null
     * @throws IllegalArgumentException якщо порушено інваріант
     */
    public Screening(String film, int hall, double ticketPrice, int sold, int durationMin) {
        this.film = validateFilm(film);
        this.hall = validatePositive(hall, "hall");
        this.ticketSales = new TicketSales(ticketPrice, sold);
        this.durationMin = validatePositive(durationMin, "durationMin");
    }

    /**
     * Перетворює CSV-рядок на кіносеанс. Правила значень спільні з конструктором.
     * Послідовна перевірка зберігає першу причину помилки з роботи № 1.
     *
     * @param line один запис із п'ятьма полями, розділеними крапкою з комою
     * @return коректний кіносеанс
     * @throws NullPointerException якщо line дорівнює null
     * @throws IllegalArgumentException якщо рядок або його поля неправильні
     */
    public static Screening fromCsv(String line) {
        Objects.requireNonNull(line, "Рядок не може бути null");
        if (line.isBlank()) {
            throw new IllegalArgumentException("порожній рядок");
        }
        String[] fields = line.split(";", -1);
        if (fields.length != 5) {
            throw new IllegalArgumentException(
                    "очікується 5 полів, отримано %d".formatted(fields.length));
        }
        for (int index = 0; index < fields.length; index++) {
            fields[index] = fields[index].strip();
        }
        String film = validateFilm(fields[0]);
        int hall = validatePositive(parseInteger(fields[1], "hall"), "hall");
        double price = TicketSales.validatePrice(parsePrice(fields[2]));
        int sold = TicketSales.validateSold(parseInteger(fields[3], "sold"));
        int duration = validatePositive(parseInteger(fields[4], "durationMin"), "durationMin");
        return new Screening(film, hall, price, sold, duration);
    }

    /** Перевіряє й нормалізує назву; правило не дублюється у фабриці. */
    private static String validateFilm(String value) {
        String text = Objects.requireNonNull(value, "Назва фільму не може бути null").strip();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("поле \"film\" не може бути порожнім");
        }
        return text;
    }

    /** Перевіряє додатність номера залу або тривалості. */
    private static int validatePositive(int value, String field) {
        if (value < 1) {
            throw new IllegalArgumentException(
                    "поле \"%s\" має бути не менше 1".formatted(field));
        }
        return value;
    }

    /** Перетворює тип; предметні обмеження перевіряються окремими спільними методами. */
    private static int parseInteger(String text, String field) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "поле \"%s\" має бути цілим числом у межах int".formatted(field), exception);
        }
    }

    /** Перетворює текст ціни з незмінним повідомленням про помилку. */
    private static double parsePrice(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "поле \"ticketPrice\" має бути числом із крапкою", exception);
        }
    }

    /**
     * Надає доступ до значення.
     *
     * @return назва фільму
     */
    public String getFilm() { return film; }

    /**
     * Надає доступ до значення.
     *
     * @return номер залу
     */
    public int getHall() { return hall; }

    /**
     * Надає доступ до значення.
     *
     * @return ціна квитка
     */
    public double getTicketPrice() { return ticketSales.ticketPrice(); }

    /**
     * Надає доступ до значення.
     *
     * @return кількість проданих квитків
     */
    public int getSold() { return ticketSales.sold(); }

    /**
     * Надає доступ до значення.
     *
     * @return тривалість у хвилинах
     */
    public int getDurationMin() { return durationMin; }

    /**
     * Надає доступ до значення.
     *
     * @return незмінне значення ціни й кількості продажів
     */
    public TicketSales getTicketSales() { return ticketSales; }

    /**
     * Надає доступ до значення.
     *
     * @return виторг сеансу
     */
    public double revenue() { return ticketSales.revenue(); }

    /**
     * Надає доступ до значення.
     *
     * @return текстове подання всіх полів із десятковою крапкою
     */
    @Override
    public String toString() {
        return String.format(Locale.ROOT,
                "%s: зал %d; %.2f грн; продано %d; %d хв",
                film, hall, getTicketPrice(), getSold(), durationMin);
    }
}
