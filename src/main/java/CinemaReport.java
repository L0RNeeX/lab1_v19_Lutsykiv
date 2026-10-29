import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Результат обробки списку кіносеансів і діагностики хибних рядків. */
public final class CinemaReport {
    private final List<Screening> screenings;
    private final List<String> errors;
    private final double totalRevenue;
    private final double totalPrices;
    private final int maxSold;

    /** Зберігає незмінні копії прийнятих об'єктів та повідомлень. */
    private CinemaReport(List<Screening> screenings, List<String> errors,
                         double totalRevenue, double totalPrices, int maxSold) {
        this.screenings = List.copyOf(screenings);
        this.errors = List.copyOf(errors);
        this.totalRevenue = totalRevenue;
        this.totalPrices = totalPrices;
        this.maxSold = maxSold;
    }

    /**
     * Створює список об'єктів і накопичує показники лише за прийнятими сеансами.
     * Переповнення сум, як у роботі № 1, відхиляє відповідний рядок.
     *
     * @param lines фізичні рядки вхідного файла в початковому порядку
     * @return звіт із коректними об'єктами, показниками та повідомленнями
     */
    public static CinemaReport fromLines(List<String> lines) {
        Objects.requireNonNull(lines, "Список рядків не може бути null");
        List<Screening> screenings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        double totalRevenue = 0.0;
        double totalPrices = 0.0;
        int maxSold = 0;
        for (int index = 0; index < lines.size(); index++) {
            try {
                Screening screening = Screening.fromCsv(lines.get(index));
                double nextRevenue = totalRevenue + screening.revenue();
                double nextPrices = totalPrices + screening.getTicketPrice();
                if (!Double.isFinite(nextRevenue) || !Double.isFinite(nextPrices)) {
                    throw new IllegalArgumentException("завеликі числа спричиняють переповнення");
                }
                screenings.add(screening);
                totalRevenue = nextRevenue;
                totalPrices = nextPrices;
                maxSold = Math.max(maxSold, screening.getSold());
            } catch (IllegalArgumentException exception) {
                errors.add("Рядок %d: %s.".formatted(index + 1, exception.getMessage()));
            }
        }
        return new CinemaReport(screenings, errors, totalRevenue, totalPrices, maxSold);
    }

    /**
     * Надає доступ до значення.
     *
     * @return незмінний список прийнятих кіносеансів
     */
    public List<Screening> getScreenings() { return screenings; }

    /**
     * Надає доступ до значення.
     *
     * @return незмінний список повідомлень про помилки
     */
    public List<String> getErrors() { return errors; }

    /**
     * Надає доступ до значення.
     *
     * @return кількість коректних сеансів
     */
    public int getCount() { return screenings.size(); }

    /**
     * Надає доступ до значення.
     *
     * @return загальний виторг
     */
    public double getTotalRevenue() { return totalRevenue; }

    /**
     * Надає доступ до значення.
     *
     * @return середня ціна за сеансами або 0 для порожнього списку
     */
    public double getAveragePrice() {
        return screenings.isEmpty() ? 0.0 : totalPrices / screenings.size();
    }

    /**
     * Надає доступ до значення.
     *
     * @return найбільша кількість проданих квитків або 0 для порожнього списку
     */
    public int getMaxSold() { return maxSold; }

    /**
     * Формує спільний текст для файла й консолі.
     * Порівняно з роботою № 1 змінено лише номер роботи в першому рядку.
     *
     * @return звіт з тим самим порядком показників і повідомлень
     */
    public String toText() {
        String averageText = screenings.isEmpty() ? "немає даних"
                : String.format(Locale.ROOT, "%.2f грн", getAveragePrice());
        String maxText = screenings.isEmpty() ? "немає даних" : Integer.toString(maxSold);
        StringBuilder report = new StringBuilder(String.format(Locale.ROOT, """
                Лабораторна робота № 2 — Кінотеатр%nВаріант: 19; рівень: 1%n%nКоректних записів: %d%nЗагальний виторг: %.2f грн%nСередня ціна квитка: %s%nНайбільша кількість проданих квитків: %s%nПропущених рядків: %d%n""",
                getCount(), totalRevenue, averageText, maxText, errors.size()));
        if (screenings.isEmpty()) {
            report.append(String.format("%nКоректних записів немає; середнє та максимум не визначено.%n"));
        }
        if (!errors.isEmpty()) {
            report.append(String.format("%nПомилки у вхідних даних:%n"));
            for (String error : errors) {
                report.append(String.format("%s%n", error));
            }
        }
        return report.toString();
    }
}
