import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Три потокові запити до незмінної колекції кіносеансів (рівень 1). */
public final class ScreeningQueries {
    private ScreeningQueries() { }

    /**
     * Повертає сеанси, на яких продано суворо більше квитків, ніж поріг.
     * Порядок вхідного списку зберігається; вхідний список не змінюється.
     *
     * @param screenings прийняті сеанси
     * @param threshold невід'ємний поріг кількості продажів
     * @return список вибраних сеансів
     */
    public static List<Screening> soldAbove(List<Screening> screenings, int threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException("Поріг sold не може бути від'ємним");
        }
        return Objects.requireNonNull(screenings, "Список сеансів не може бути null")
                .stream()
                .filter(screening -> screening.getSold() > threshold)
                .toList();
    }

    /**
     * Перетворює колекцію сеансів на колекцію назв фільмів.
     * Дубльовані назви залишаються, оскільки це різні сеанси.
     *
     * @param screenings прийняті сеанси
     * @return назви в початковому порядку
     */
    public static List<String> filmNames(List<Screening> screenings) {
        return Objects.requireNonNull(screenings, "Список сеансів не може бути null")
                .stream()
                .map(Screening::getFilm)
                .toList();
    }

    /**
     * Групує сумарний виторг за залом з урахуванням поліморфного revenue().
     * TreeMap гарантує впорядкування номерів залів за зростанням.
     *
     * @param screenings прийняті сеанси
     * @return нова карта: номер залу -> виторг
     */
    public static Map<Integer, Double> revenueByHall(List<Screening> screenings) {
        return Objects.requireNonNull(screenings, "Список сеансів не може бути null")
                .stream()
                .collect(Collectors.groupingBy(
                        Screening::getHall,
                        TreeMap::new,
                        Collectors.summingDouble(Screening::revenue)));
    }
}
