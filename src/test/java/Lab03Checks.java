import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Незалежні перевірки поліморфної моделі без зовнішніх залежностей. */
public final class Lab03Checks {
    private Lab03Checks() { }

    @FunctionalInterface
    interface Check { void run() throws Exception; }

    record Case(String name, Check check) { }

    static List<Case> cases() {
        List<Case> tests = new ArrayList<>();
        tests.add(new Case("Спільний базовий тип", () -> {
            Screening s = new RegularScreening("Кіно", 1, 100, 5, 90);
            equal("Кіно", s.getFilm()); equal(1, s.getHall()); equal(90, s.getDurationMin());
        }));
        tests.add(new Case("Звичайний виторг", () -> near(1000, new RegularScreening("Кіно",1,100,10,90).revenue())));
        tests.add(new Case("Преміум-виторг", () -> near(1200, new PremiumScreening("VIP: Кіно",1,100,10,90).revenue())));
        tests.add(new Case("Різна поведінка підтипів", () -> {
            Screening a = new RegularScreening("Кіно", 1, 100, 10, 90);
            Screening b = new PremiumScreening("Кіно", 1, 100, 10, 90);
            near(1000, a.revenue()); near(1200, b.revenue());
        }));
        tests.add(new Case("Підтип Regular через CSV", () -> check(
                Screening.fromCsv("Кіно;1;100.00;10;90") instanceof RegularScreening,
                "Не обрано звичайний підтип")));
        tests.add(new Case("Підтип Premium через CSV", () -> check(
                Screening.fromCsv("VIP: Прем'єра;1;100.00;10;90") instanceof PremiumScreening,
                "Не обрано преміум-підтип")));
        tests.add(new Case("enum і текстове представлення", () -> {
            equal("звичайний", ScreeningKind.REGULAR.label());
            equal("преміум", ScreeningKind.PREMIUM.label());
            equal(ScreeningKind.PREMIUM, new PremiumScreening("Кіно", 1, 100, 1, 90).getKind());
        }));
        tests.add(new Case("Однакові об'єкти рівні", () -> {
            Screening a = new RegularScreening("Кіно", 1, 100, 10, 90);
            Screening b = new RegularScreening("Кіно", 1, 100, 10, 90);
            check(a.equals(b) && b.equals(a), "Рівність порушена");
            equal(a.hashCode(), b.hashCode());
        }));
        tests.add(new Case("Різні підтипи не рівні", () -> check(
                !new RegularScreening("Кіно",1,100,10,90).equals(
                 new PremiumScreening("Кіно",1,100,10,90)), "Різні підтипи рівні")));
        tests.add(new Case("Зміна значення порушує рівність", () -> check(
                !new RegularScreening("Кіно",1,100,10,90).equals(
                 new RegularScreening("Кіно",1,101,10,90)), "Різна ціна не врахована")));
        tests.add(new Case("HashSet прибирає дублікати", () -> {
            HashSet<Screening> set = new HashSet<>();
            set.add(new RegularScreening("Кіно",1,100,10,90));
            set.add(new RegularScreening("Кіно",1,100,10,90));
            set.add(new PremiumScreening("Кіно",1,100,10,90));
            equal(2, set.size());
        }));
        tests.add(new Case("Поліморфна колекція", () -> {
            List<Screening> list = List.of(
                    new RegularScreening("A",1,100,10,90),
                    new PremiumScreening("B",2,100,10,90));
            double total = 0;
            for (Screening s : list) { total += s.revenue(); }
            near(2200, total);
        }));
        tests.add(new Case("Спільна валідація звичайного", () -> rejects(
                () -> new RegularScreening("",1,100,10,90))));
        tests.add(new Case("Спільна валідація преміум", () -> rejects(
                () -> new PremiumScreening("VIP: X",0,100,10,90))));
        tests.add(new Case("Від'ємні продажі premium", () -> rejects(
                () -> new PremiumScreening("VIP: X",1,100,-10,90))));
        tests.add(new Case("Нульові продажі premium", () -> near(0,
                new PremiumScreening("VIP: X",1,100,0,90).revenue())));
        tests.add(new Case("Нові коректні й хибні рядки", () -> {
            CinemaReport r = CinemaReport.fromLines(Files.readAllLines(
                    Path.of("data/polymorphism.csv"), StandardCharsets.UTF_8));
            equal(2, r.getCount()); equal(2, r.getErrors().size());
            equal(2, r.getScreenings().size()); near(2200, r.getTotalRevenue());
            near(150, r.getAveragePrice()); equal(10, r.getMaxSold());
            check(r.getScreenings().get(0) instanceof RegularScreening, "Немає Regular");
            check(r.getScreenings().get(1) instanceof PremiumScreening, "Немає Premium");
        }));
        tests.add(new Case("Сумісність старого звіту", () -> {
            CinemaReport r = CinemaReport.fromLines(Files.readAllLines(
                    Path.of("data/input.csv"), StandardCharsets.UTF_8));
            String before = Files.readString(Path.of("src/test/resources/lab01-report.txt"), StandardCharsets.UTF_8);
            String after = r.toText().replace("Лабораторна робота № 4", "Лабораторна робота № 1");
            equal(before.replace("\r\n", "\n"), after.replace("\r\n", "\n"));
        }));
        return List.copyOf(tests);
    }

    private static void check(boolean ok, String error) { if (!ok) { throw new AssertionError(error); } }
    private static void near(double expected, double actual) {
        check(Math.abs(expected-actual) < 0.000001, "Очікується %s, отримано %s".formatted(expected, actual));
    }
    private static void equal(Object expected, Object actual) {
        check(Objects.equals(expected,actual), "Очікується %s, отримано %s".formatted(expected,actual));
    }
    private static void rejects(Check check) throws Exception {
        try { check.run(); } catch (IllegalArgumentException error) { return; }
        throw new AssertionError("Мав бути IllegalArgumentException");
    }

    /** Запускає нові перевірки вручну; попередні запускає скрипт test.cmd. */
    public static void main(String[] args) {
        int succeeded = 0;
        for (Case c : cases()) {
            try { c.check().run(); succeeded++; }
            catch (Exception | AssertionError error) { System.out.printf("FAIL: %s — %s%n", c.name(), error); }
        }
        System.out.printf("Лабораторна робота № 3: %d / %d нових перевірок успішні.%n", succeeded, cases().size());
        if (succeeded != cases().size()) { System.exit(1); }
    }
}
