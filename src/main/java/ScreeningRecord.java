import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** Плоский запис для CSV-експорту сеансу (варіант 19). */
public final class ScreeningRecord {
    @CsvColumn("фільм")
    private final String film;
    @CsvColumn("зал")
    private final int hall;
    @CsvColumn("час")
    private final LocalDateTime startAt;
    @CsvColumn("ціна")
    private final double ticketPrice;
    @CsvColumn("продано")
    private final int soldTickets;

    /** Перевіряє дані нового плоского CSV-запису. */
    public ScreeningRecord(String film, int hall, LocalDateTime startAt,
                           double ticketPrice, int soldTickets) {
        if (film == null || film.isBlank()) {
            throw new IllegalArgumentException("Назва фільму не може бути порожньою");
        }
        if (hall < 1 || soldTickets < 0 || !Double.isFinite(ticketPrice) || ticketPrice < 0) {
            throw new IllegalArgumentException("Неприпустиме значення залу, ціни або продажів");
        }
        this.film = film;
        this.hall = hall;
        this.startAt = Objects.requireNonNull(startAt, "Час сеансу не може бути null");
        this.ticketPrice = ticketPrice;
        this.soldTickets = soldTickets;
    }

    /** Відновлює об'єкт із полів у порядку CSV-стовпців (за Java-іменами). */
    public static ScreeningRecord fromFields(List<String> fields) throws DataStorageException {
        if (fields.size() != 5) {
            throw new DataStorageException("Очікується 5 полів CSV, отримано " + fields.size());
        }
        try {
            return new ScreeningRecord(fields.get(0), Integer.parseInt(fields.get(1)),
                    LocalDateTime.parse(fields.get(3)), Double.parseDouble(fields.get(4)),
                    Integer.parseInt(fields.get(2)));
        } catch (RuntimeException cause) {
            throw new DataStorageException("Неправильне значення в CSV-записі", cause);
        }
    }

    /** Повертає назву фільму. */
    public String getFilm() { return film; }
    /** Повертає дату й час сеансу. */
    public LocalDateTime getStartAt() { return startAt; }
    /** Повертає номер залу. */
    public int getHall() { return hall; }

    /** Порівнює всі значення плоского CSV-запису. */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ScreeningRecord r)) return false;
        return film.equals(r.film) && hall == r.hall && startAt.equals(r.startAt)
                && Double.compare(ticketPrice, r.ticketPrice) == 0 && soldTickets == r.soldTickets;
    }

    /** Хеш використовує ті самі поля, що й equals. */
    @Override
    public int hashCode() { return Objects.hash(film, hall, startAt, ticketPrice, soldTickets); }
}
