import java.util.Locale;

/**
 * Незмінні дані про ціну та кількість проданих квитків одного сеансу.
 *
 * @param ticketPrice скінченна невід'ємна ціна квитка
 * @param sold невід'ємна кількість проданих квитків
 */
public record TicketSales(double ticketPrice, int sold) {
    /**
     * Перевіряє компоненти перед створенням значення.
     *
     * @throws IllegalArgumentException якщо ціна або кількість неприпустимі
     */
    public TicketSales {
        validatePrice(ticketPrice);
        validateSold(sold);
    }

    /**
     * Надає доступ до значення.
     *
     * @return виторг одного сеансу; переповнення перевіряє обчислювач звіту
     */
    public double revenue() {
        return ticketPrice * sold;
    }

    /** Спільне правило ціни для конструктора record і CSV-фабрики. */
    static double validatePrice(double price) {
        if (!Double.isFinite(price) || price < 0.0) {
            throw new IllegalArgumentException(
                    "поле \"ticketPrice\" має бути скінченним невід'ємним числом");
        }
        return price;
    }

    /** Спільне правило кількості для конструктора record і CSV-фабрики. */
    static int validateSold(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("поле \"sold\" має бути не менше 0");
        }
        return count;
    }

    /**
     * Надає доступ до значення.
     *
     * @return форматоване подання ціни та кількості з десятковою крапкою
     */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "ціна: %.2f грн; продано: %d", ticketPrice, sold);
    }
}
