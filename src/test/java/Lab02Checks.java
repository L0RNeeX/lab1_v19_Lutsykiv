import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Перевірки, які можна виконати через JUnit 5 або без зовнішніх бібліотек. */
public final class Lab02Checks {
    private Lab02Checks() { }

    @FunctionalInterface
    interface Check { void run() throws Exception; }
    record TestCase(String group, String name, Check check) { }
    private record RunResult(int code, String output, String error) { }

    /** @return спільний набір перевірок для консольного запуску та JUnit */
    static List<TestCase> cases() {
        List<TestCase> tests = new ArrayList<>();
        add(tests, "Screening", "Конструктор і всі геттери", () -> {
            Screening s = new RegularScreening("Місто світла", 1, 150, 80, 100);
            equal("Місто світла", s.getFilm()); equal(1, s.getHall());
            near(150, s.getTicketPrice()); equal(80, s.getSold()); equal(100, s.getDurationMin());
            near(12000, s.revenue()); equal(new TicketSales(150, 80), s.getTicketSales());
        });
        add(tests, "Screening", "null-назва", () -> rejects(NullPointerException.class,
                () -> new RegularScreening(null, 1, 1, 1, 1)));
        add(tests, "Screening", "Порожня назва", () -> rejects(IllegalArgumentException.class,
                () -> new RegularScreening("", 1, 1, 1, 1)));
        add(tests, "Screening", "Назва із пробілів", () -> rejects(IllegalArgumentException.class,
                () -> new RegularScreening(" \t ", 1, 1, 1, 1)));
        add(tests, "Screening", "Нульовий зал", () -> rejects(IllegalArgumentException.class,
                () -> new RegularScreening("А", 0, 1, 1, 1)));
        add(tests, "Screening", "Від'ємний зал", () -> rejects(IllegalArgumentException.class,
                () -> new RegularScreening("А", -1, 1, 1, 1)));
        add(tests, "Screening", "Від'ємна ціна", () -> rejects(IllegalArgumentException.class,
                () -> new RegularScreening("А", 1, -1, 1, 1)));
        add(tests, "Screening", "Ціна NaN", () -> rejects(IllegalArgumentException.class,
                () -> new RegularScreening("А", 1, Double.NaN, 1, 1)));
        add(tests, "Screening", "Нескінченна ціна", () -> {
            rejects(IllegalArgumentException.class, () -> new RegularScreening("А", 1, Double.POSITIVE_INFINITY, 1, 1));
            rejects(IllegalArgumentException.class, () -> new RegularScreening("А", 1, Double.NEGATIVE_INFINITY, 1, 1));
        });
        add(tests, "Screening", "Від'ємні продажі", () -> rejects(IllegalArgumentException.class,
                () -> new RegularScreening("А", 1, 1, -1, 1)));
        add(tests, "Screening", "Нульова тривалість", () -> rejects(IllegalArgumentException.class,
                () -> new RegularScreening("А", 1, 1, 1, 0)));
        add(tests, "Screening", "Від'ємна тривалість", () -> rejects(IllegalArgumentException.class,
                () -> new RegularScreening("А", 1, 1, 1, -1)));
        add(tests, "Screening", "Граничні допустимі значення", () -> {
            Screening s = new RegularScreening("Безкоштовний сеанс", 1, 0, 0, 1);
            near(0, s.revenue()); equal(0, s.getSold());
        });
        add(tests, "Screening", "Нормалізація назви", () ->
                equal("Кіно", new RegularScreening("  Кіно  ", 1, 1, 1, 1).getFilm()));
        add(tests, "Screening", "toString і українські символи", () -> equal(
                "Місто світла: зал 1; 150.00 грн; продано 80; 100 хв",
                new RegularScreening("Місто світла", 1, 150, 80, 100).toString()));

        add(tests, "fromCsv", "Коректний рядок", () -> {
            Screening s = Screening.fromCsv("Місто світла;1;150.00;80;100");
            equal("Місто світла", s.getFilm()); equal(1, s.getHall());
            near(150, s.getTicketPrice()); equal(80, s.getSold()); equal(100, s.getDurationMin());
        });
        add(tests, "fromCsv", "Пробіли навколо полів", () -> {
            Screening s = Screening.fromCsv(" Кіно ; 2 ; 12.50 ; 3 ; 90 ");
            equal("Кіно", s.getFilm()); near(37.5, s.revenue());
        });
        add(tests, "fromCsv", "Порожній рядок", () -> message("порожній рядок",
                () -> Screening.fromCsv(" \t")));
        add(tests, "fromCsv", "null-рядок", () -> rejects(NullPointerException.class,
                () -> Screening.fromCsv(null)));
        add(tests, "fromCsv", "Недостатньо полів", () -> message("очікується 5 полів, отримано 4",
                () -> Screening.fromCsv("Кіно;1;2;3")));
        add(tests, "fromCsv", "Зайве поле", () -> message("очікується 5 полів, отримано 6",
                () -> Screening.fromCsv("Кіно;1;2;3;4;5")));
        add(tests, "fromCsv", "Порожнє останнє поле зберігається", () -> message(
                "поле \"durationMin\" має бути цілим числом у межах int",
                () -> Screening.fromCsv("Кіно;1;2;3;")));
        add(tests, "fromCsv", "Нечисловий зал", () -> message(
                "поле \"hall\" має бути цілим числом у межах int",
                () -> Screening.fromCsv("Кіно;abc;2;3;4")));
        add(tests, "fromCsv", "Нечислова ціна", () -> message(
                "поле \"ticketPrice\" має бути числом із крапкою",
                () -> Screening.fromCsv("Кіно;1;abc;3;4")));
        add(tests, "fromCsv", "Нечислові продажі", () -> message(
                "поле \"sold\" має бути цілим числом у межах int",
                () -> Screening.fromCsv("Кіно;1;2;abc;4")));
        add(tests, "fromCsv", "Десяткова кома", () -> rejects(IllegalArgumentException.class,
                () -> Screening.fromCsv("Кіно;1;12,5;3;4")));
        add(tests, "fromCsv", "Переповнення int", () -> rejects(IllegalArgumentException.class,
                () -> Screening.fromCsv("Кіно;2147483648;2;3;4")));
        add(tests, "fromCsv", "Нескінченність після розбору", () -> rejects(IllegalArgumentException.class,
                () -> Screening.fromCsv("Кіно;1;1e999;3;4")));
        add(tests, "fromCsv", "Пріоритет помилок як у роботі 1", () -> message(
                "поле \"hall\" має бути не менше 1",
                () -> Screening.fromCsv("Кіно;-1;abc;-2;0")));
        add(tests, "fromCsv", "Порожня назва перевіряється першою", () -> message(
                "поле \"film\" не може бути порожнім",
                () -> Screening.fromCsv(";abc;abc;abc;abc")));

        add(tests, "TicketSales", "Доступ до компонентів", () -> {
            TicketSales s = new TicketSales(12.5, 3); near(12.5, s.ticketPrice()); equal(3, s.sold());
        });
        add(tests, "TicketSales", "Рівність і hashCode", () -> {
            TicketSales a = new TicketSales(150, 80); TicketSales b = new TicketSales(150, 80);
            equal(a, b); equal(a.hashCode(), b.hashCode());
            check(!a.equals(new TicketSales(151, 80)), "Різні ціни не повинні давати рівні record");
        });
        add(tests, "TicketSales", "Виторг", () -> near(37.5, new TicketSales(12.5, 3).revenue()));
        add(tests, "TicketSales", "Нульові компоненти", () -> near(0, new TicketSales(0, 0).revenue()));
        add(tests, "TicketSales", "Від'ємна ціна", () -> rejects(IllegalArgumentException.class,
                () -> new TicketSales(-1, 0)));
        add(tests, "TicketSales", "Від'ємна кількість", () -> rejects(IllegalArgumentException.class,
                () -> new TicketSales(0, -1)));
        add(tests, "TicketSales", "NaN і нескінченність", () -> {
            rejects(IllegalArgumentException.class, () -> new TicketSales(Double.NaN, 1));
            rejects(IllegalArgumentException.class, () -> new TicketSales(Double.POSITIVE_INFINITY, 1));
        });
        add(tests, "TicketSales", "Форматування не залежить від локалі", () -> {
            Locale before = Locale.getDefault();
            try {
                Locale.setDefault(Locale.GERMANY);
                equal("ціна: 12.50 грн; продано: 3", new TicketSales(12.5, 3).toString());
            } finally { Locale.setDefault(before); }
        });

        add(tests, "Звіт", "Чотири контрольні показники", () -> {
            CinemaReport r = sample(); equal(5, r.getCount()); near(35200, r.getTotalRevenue());
            near(142, r.getAveragePrice()); equal(80, r.getMaxSold()); equal(5, r.getErrors().size());
        });
        add(tests, "Звіт", "Порівняння з фактичним звітом роботи 1", () -> {
            String old = Files.readString(Path.of("src/test/resources/lab01-report.txt"), StandardCharsets.UTF_8);
            String now = sample().toText().replace("Лабораторна робота № 3", "Лабораторна робота № 1");
            equal(normalize(old), normalize(now));
        });
        add(tests, "Звіт", "Порожній список", () -> {
            CinemaReport r = CinemaReport.fromLines(List.of()); equal(0, r.getCount()); near(0, r.getTotalRevenue());
            check(r.toText().contains("Середня ціна квитка: немає даних"), "Немає позначки середнього");
            check(r.toText().contains("Найбільша кількість проданих квитків: немає даних"), "Немає позначки максимуму");
        });
        add(tests, "Звіт", "Лише хибні рядки", () -> {
            CinemaReport r = CinemaReport.fromLines(Lab02.readInput(Path.of("data/invalid.csv")));
            equal(0, r.getCount()); equal(5, r.getErrors().size()); near(0, r.getTotalRevenue());
        });
        add(tests, "Звіт", "Хибні рядки не змінюють статистику", () -> {
            CinemaReport r = CinemaReport.fromLines(List.of("А;1;10;2;90", "Б;1;100;-5;90"));
            equal(1, r.getCount()); near(20, r.getTotalRevenue()); near(10, r.getAveragePrice());
        });
        add(tests, "Звіт", "Збереження фізичних номерів рядків", () -> {
            CinemaReport r = CinemaReport.fromLines(List.of("А;1;10;2;90", "", "Б;1;abc;3;90"));
            equal("Рядок 2: порожній рядок.", r.getErrors().get(0));
            check(r.getErrors().get(1).startsWith("Рядок 3:"), "Неправильний номер рядка");
        });
        add(tests, "Звіт", "Незмінність списку сеансів", () -> rejects(UnsupportedOperationException.class,
                () -> sample().getScreenings().clear()));
        add(tests, "Звіт", "Незмінність списку помилок", () -> rejects(UnsupportedOperationException.class,
                () -> sample().getErrors().clear()));
        add(tests, "Звіт", "Дробова середня", () -> {
            CinemaReport r = CinemaReport.fromLines(List.of("А;1;10.25;2;90", "Б;1;10.50;1;90"));
            near(10.375, r.getAveragePrice()); near(31, r.getTotalRevenue());
        });
        add(tests, "Звіт", "Середня не зважується продажами", () -> {
            CinemaReport r = CinemaReport.fromLines(List.of("А;1;10;100;90", "Б;1;30;0;90"));
            near(20, r.getAveragePrice());
        });
        add(tests, "Звіт", "Незалежність показників від локалі", () -> {
            Locale before = Locale.getDefault();
            try { Locale.setDefault(Locale.GERMANY); check(sample().toText().contains("142.00 грн"), "Змінено роздільник"); }
            finally { Locale.setDefault(before); }
        });
        add(tests, "Звіт", "Переповнення виторгу рядка", () -> {
            CinemaReport r = CinemaReport.fromLines(List.of("А;1;1e308;2;90", "Б;1;2;3;90"));
            equal(1, r.getCount()); near(6, r.getTotalRevenue()); equal(1, r.getErrors().size());
        });
        add(tests, "Звіт", "Переповнення суми цін", () -> {
            CinemaReport r = CinemaReport.fromLines(List.of("А;1;1e308;0;90", "Б;1;1e308;0;90"));
            equal(1, r.getCount()); equal(1, r.getErrors().size());
        });

        add(tests, "CLI і файли", "Довідка", () -> {
            RunResult r = run("--help"); equal(0, r.code()); check(r.output().contains("--input"), "Немає довідки");
        });
        add(tests, "CLI і файли", "Невідомий параметр", () -> equal(2, run("--unknown").code()));
        add(tests, "CLI і файли", "Немає значення input", () -> equal(2, run("--input").code()));
        add(tests, "CLI і файли", "Немає значення output", () -> equal(2, run("--output").code()));
        add(tests, "CLI і файли", "Повторений input", () -> equal(2, run("--input", "a", "--input", "b").code()));
        add(tests, "CLI і файли", "Повторений output", () -> equal(2, run("--output", "a", "--output", "b").code()));
        add(tests, "CLI і файли", "Порожній шлях", () -> equal(2, run("--input", " ").code()));
        add(tests, "CLI і файли", "Довідка не змішується з параметрами", () -> equal(2, run("--help", "--input", "a").code()));
        add(tests, "CLI і файли", "Типові шляхи", () -> {
            Path[] p = Lab02.parseArguments(new String[0]);
            equal(Path.of("data", "input.csv"), p[0]); equal(Path.of("out", "report.txt"), p[1]);
        });
        add(tests, "CLI і файли", "BOM, CRLF і український шлях", () -> withTemp(dir -> {
            Path input = dir.resolve("дані сеансів.csv");
            Files.writeString(input, "\uFEFFКіно;1;12.50;2;90\r\n", StandardCharsets.UTF_8);
            CinemaReport r = CinemaReport.fromLines(Lab02.readInput(input));
            equal(1, r.getCount()); near(25, r.getTotalRevenue()); equal("Кіно", r.getScreenings().get(0).getFilm());
        }));
        add(tests, "CLI і файли", "Однаковий текст у файлі й консолі", () -> withTemp(dir -> {
            Path output = dir.resolve("новий каталог/звіт.txt");
            RunResult r = run("--input", "data/input.csv", "--output", output.toString());
            equal(0, r.code()); equal("", r.error()); equal(r.output(), Files.readString(output, StandardCharsets.UTF_8));
        }));
        add(tests, "CLI і файли", "Відсутній вхідний файл", () -> withTemp(dir ->
                equal(1, run("--input", dir.resolve("missing.csv").toString()).code())));
        add(tests, "CLI і файли", "Пошкоджене UTF-8", () -> withTemp(dir -> {
            Path input = dir.resolve("broken.csv"); Files.write(input, new byte[] {(byte) 0xC3, (byte) 0x28});
            equal(1, run("--input", input.toString(), "--output", dir.resolve("report.txt").toString()).code());
        }));
        add(tests, "CLI і файли", "Захист вхідного файла", () -> withTemp(dir -> {
            Path input = dir.resolve("input.csv"); String text = "А;1;10;2;90\n";
            Files.writeString(input, text, StandardCharsets.UTF_8);
            equal(2, run("--input", input.toString(), "--output", input.toString()).code());
            equal(text, Files.readString(input, StandardCharsets.UTF_8));
        }));
        add(tests, "CLI і файли", "Вихід у файл поточного каталогу", () -> {
            Path output = Path.of("lab02-test-output-%d.txt".formatted(System.nanoTime()));
            try { Lab02.writeReport(output, "Український текст"); equal("Український текст", Files.readString(output)); }
            finally { Files.deleteIfExists(output); }
        });
        add(tests, "CLI і файли", "Порожній файл", () -> withTemp(dir -> {
            Path input = dir.resolve("empty.csv"); Files.createFile(input);
            RunResult r = run("--input", input.toString(), "--output", dir.resolve("empty-report.txt").toString());
            equal(0, r.code()); check(r.output().contains("Коректних записів: 0"), "Неправильний порожній звіт");
        }));
        return List.copyOf(tests);
    }

    private static CinemaReport sample() throws IOException {
        return CinemaReport.fromLines(Lab02.readInput(Path.of("data/input.csv")));
    }
    private static void add(List<TestCase> tests, String group, String name, Check check) {
        tests.add(new TestCase(group, name, check));
    }
    private static void equal(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Очікується: %s; отримано: %s".formatted(expected, actual));
        }
    }
    private static void near(double expected, double actual) {
        if (!Double.isFinite(actual) || Math.abs(expected - actual) > 0.000001) {
            throw new AssertionError("Очікується: %s; отримано: %s".formatted(expected, actual));
        }
    }
    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
    private static void rejects(Class<? extends Throwable> type, Check action) throws Exception {
        try { action.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) { return; }
            throw new AssertionError("Неочікуваний тип винятку: %s".formatted(error), error);
        }
        throw new AssertionError("Очікується виняток %s".formatted(type.getSimpleName()));
    }
    private static void message(String expected, Check action) throws Exception {
        try { action.run(); }
        catch (IllegalArgumentException error) { equal(expected, error.getMessage()); return; }
        throw new AssertionError("Очікується IllegalArgumentException");
    }
    private static String normalize(String text) { return text.replace("\r\n", "\n"); }
    private static RunResult run(String... args) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(output, true, StandardCharsets.UTF_8);
             PrintStream err = new PrintStream(error, true, StandardCharsets.UTF_8)) {
            int code = Lab02.run(args, out, err);
            return new RunResult(code, output.toString(StandardCharsets.UTF_8), error.toString(StandardCharsets.UTF_8));
        }
    }
    @FunctionalInterface
    private interface TempCheck { void run(Path dir) throws Exception; }
    private static void withTemp(TempCheck check) throws Exception {
        Path dir = Files.createTempDirectory("lab02-");
        try { check.run(dir); }
        finally {
            try (var paths = Files.walk(dir)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) { Files.deleteIfExists(path); }
            }
        }
    }

    /**
     * Виконує той самий набір перевірок без завантаження JUnit.
     *
     * @param args не використовуються
     */
    public static void main(String[] args) {
        PrintStream output = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        Map<String, int[]> groups = new LinkedHashMap<>();
        int failed = 0;
        List<TestCase> tests = cases();
        for (TestCase test : tests) {
            int[] counts = groups.computeIfAbsent(test.group(), key -> new int[2]);
            counts[1]++;
            try { test.check().run(); counts[0]++; }
            catch (Exception | AssertionError error) {
                failed++;
                output.printf("FAIL: %s / %s — %s%n", test.group(), test.name(), error);
            }
        }
        output.printf("Лабораторна робота № 3. Варіант 19. Перевірки%n%n");
        for (Map.Entry<String, int[]> entry : groups.entrySet()) {
            output.printf("%-20s %d / %d%n", entry.getKey(), entry.getValue()[0], entry.getValue()[1]);
        }
        output.printf("%nУспішно: %d / %d. Помилок: %d.%n", tests.size() - failed, tests.size(), failed);
        output.printf("Контрольний звіт роботи № 1 порівнюється без номера роботи в заголовку.%n");
        if (failed != 0) { System.exit(1); }
    }
}
