import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Самодостатні перевірки трьох запитів Stream API для першого рівня. */
public final class Lab04Checks {
    @FunctionalInterface public interface Check { void run() throws Exception; }
    public record Case(String name, Check check) { }
    private Lab04Checks() { }

    private static Screening regular(String film, int hall, double price, int sold) {
        return new RegularScreening(film, hall, price, sold, 90);
    }
    private static Screening premium(String film, int hall, double price, int sold) {
        return new PremiumScreening(film, hall, price, sold, 90);
    }
    private static void ok(boolean value, String message) {
        if (!value) { throw new AssertionError(message); }
    }
    private static void eq(Object expected, Object actual) {
        ok(Objects.equals(expected, actual), "Очікується %s, отримано %s".formatted(expected, actual));
    }
    private static void near(double expected, double actual) {
        ok(Math.abs(expected - actual) < 0.000001,
                "Очікується %.4f, отримано %.4f".formatted(expected, actual));
    }

    /** Повертає тестові сценарії, також доступні через JUnit. */
    public static List<Case> cases() {
        List<Case> tests = new ArrayList<>();
        List<Screening> sample = List.of(
                regular("Фільм А", 1, 150, 80),
                premium("VIP: Фільм Б", 2, 200, 5),
                regular("Фільм А", 1, 120, 40),
                regular("Фільм В", 2, 100, 41));

        tests.add(new Case("filter: суворо більше за поріг", () ->
                eq(List.of(sample.get(0), sample.get(3)), ScreeningQueries.soldAbove(sample, 40))));
        tests.add(new Case("filter: значення на межі виключено", () ->
                ok(!ScreeningQueries.soldAbove(sample, 40).contains(sample.get(2)), "40 не відсіяно")));
        tests.add(new Case("filter: нульовий поріг", () ->
                eq(4, ScreeningQueries.soldAbove(sample, 0).size())));
        tests.add(new Case("filter: надмірний поріг", () ->
                eq(List.of(), ScreeningQueries.soldAbove(sample, 100))));
        tests.add(new Case("filter: від'ємний поріг відхилено", () -> {
            try { ScreeningQueries.soldAbove(sample, -1); throw new AssertionError("Помилка не виникла"); }
            catch (IllegalArgumentException expected) { /* очікуване відхилення */ }
        }));
        tests.add(new Case("map: назви й дублікати збережені", () ->
                eq(List.of("Фільм А", "VIP: Фільм Б", "Фільм А", "Фільм В"),
                        ScreeningQueries.filmNames(sample))));
        tests.add(new Case("groupingBy: поліформний виторг за двома залами", () -> {
            Map<Integer, Double> byHall = ScreeningQueries.revenueByHall(sample);
            eq(List.of(1, 2), new ArrayList<>(byHall.keySet()));
            near(16800, byHall.get(1)); // 150*80 + 120*40
            near(5300, byHall.get(2)); // 200*5*1.2 + 100*41
        }));
        tests.add(new Case("groupingBy: преміум-надбавка врахована", () ->
                near(1200, ScreeningQueries.revenueByHall(List.of(premium("VIP", 1, 200, 5))).get(1))));
        tests.add(new Case("filter: порожня колекція", () ->
                eq(List.of(), ScreeningQueries.soldAbove(List.of(), 40))));
        tests.add(new Case("map: порожня колекція", () ->
                eq(List.of(), ScreeningQueries.filmNames(List.of()))));
        tests.add(new Case("groupingBy: порожня колекція", () ->
                eq(Map.of(), ScreeningQueries.revenueByHall(List.of()))));
        tests.add(new Case("Запити не змінюють вхідний список", () -> {
            List<Screening> before = List.copyOf(sample);
            ScreeningQueries.soldAbove(sample, 40);
            ScreeningQueries.filmNames(sample);
            ScreeningQueries.revenueByHall(sample);
            eq(before, sample);
        }));
        tests.add(new Case("Реальні 8 сеансів і 2 хибні рядки", () -> {
            CinemaReport r = CinemaReport.fromLines(Files.readAllLines(
                    Path.of("data/stream.csv"), StandardCharsets.UTF_8));
            eq(8, r.getCount()); eq(2, r.getErrors().size());
            eq(4, ScreeningQueries.soldAbove(r.getScreenings(), 40).size());
            near(43680, r.getTotalRevenue());
            near(136.25, r.getAveragePrice());
            eq(80, r.getMaxSold());
        }));
        tests.add(new Case("Реальне групування за залами", () -> {
            CinemaReport r = CinemaReport.fromLines(Files.readAllLines(
                    Path.of("data/stream.csv"), StandardCharsets.UTF_8));
            var groups = ScreeningQueries.revenueByHall(r.getScreenings());
            near(22000, groups.get(1)); near(12000, groups.get(2)); near(9680, groups.get(3));
        }));
        tests.add(new Case("Демонстраційний текст містить результати", () -> {
            CinemaReport r = CinemaReport.fromLines(Files.readAllLines(
                    Path.of("data/stream.csv"), StandardCharsets.UTF_8));
            String output = Lab04.formatQueries(r);
            ok(output.contains("зал 1 = 22000.00 грн") && output.contains("Коректних сеансів: 8"),
                    "Неправильний текст демонстрації");
        }));
        tests.add(new Case("Звіт ЛР4 має попередні числа", () -> {
            CinemaReport r = CinemaReport.fromLines(Files.readAllLines(
                    Path.of("data/input.csv"), StandardCharsets.UTF_8));
            eq(5, r.getCount()); near(35200, r.getTotalRevenue());
            near(142, r.getAveragePrice()); eq(80, r.getMaxSold());
            eq(5, r.getErrors().size());
        }));
        return List.copyOf(tests);
    }

    /** Виконує перевірки без Maven для зручного запуску на Windows. */
    public static void main(String[] args) {
        int passed = 0;
        for (Case test : cases()) {
            try { test.check().run(); passed++; }
            catch (Exception | AssertionError error) {
                System.out.printf("FAIL: %s — %s%n", test.name(), error.getMessage());
            }
        }
        System.out.printf("Лабораторна робота № 4: %d / %d перевірок Stream API успішні.%n",
                passed, cases().size());
        if (passed != cases().size()) { System.exit(1); }
    }
}
