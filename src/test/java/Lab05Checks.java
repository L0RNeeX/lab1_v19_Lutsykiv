import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Самодостатні перевірки сховища, анотацій і CSV-експорту варіанта 19. */
public final class Lab05Checks {
    @FunctionalInterface public interface Check { void run() throws Exception; }
    public record Case(String name, Check check) { }
    private Lab05Checks() { }

    private static void equal(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Очікується «%s», отримано «%s»".formatted(expected, actual));
        }
    }
    private static void ok(boolean condition, String text) {
        if (!condition) throw new AssertionError(text);
    }
    private static ScreeningRecord record(String title) {
        return new ScreeningRecord(title, 1, LocalDateTime.parse("2026-11-03T10:00"), 150, 5);
    }
    private static Path tempfile() throws Exception { return Files.createTempFile("lab05-", ".csv"); }

    /** Повертає нові перевірки для консольного запуску та JUnit. */
    public static List<Case> cases() {
        List<Case> tests = new ArrayList<>();
        tests.add(new Case("Repository зберігає записи", () -> {
            Repository<ScreeningRecord> repo = new Repository<>(); repo.add(record("А")); repo.add(record("Б"));
            equal(2, repo.all().size()); equal("А", repo.all().get(0).getFilm());
        }));
        tests.add(new Case("Repository шукає за Predicate", () -> {
            Repository<ScreeningRecord> repo = new Repository<>();
            repo.add(record("А")); repo.add(new ScreeningRecord("Б",2,LocalDateTime.parse("2026-11-03T11:00"),100,3));
            equal(1, repo.find(r -> r.getHall() == 2).size());
        }));
        tests.add(new Case("Незмінний знімок Repository", () -> {
            Repository<String> repo = new Repository<>(); repo.add("перший");
            List<String> snapshot = repo.all(); repo.add("другий");
            equal(List.of("перший"), snapshot);
            try { snapshot.add("зміна"); throw new AssertionError("Знімок змінний"); }
            catch (UnsupportedOperationException expected) { }
        }));
        tests.add(new Case("Repository відхиляє null", () -> {
            try { new Repository<String>().add(null); throw new AssertionError("null прийнято"); }
            catch (NullPointerException expected) { }
        }));
        tests.add(new Case("@CsvColumn доступна через рефлексію", () -> {
            var field = ScreeningRecord.class.getDeclaredField("film");
            equal("фільм", field.getAnnotation(CsvColumn.class).value());
            equal(java.lang.annotation.RetentionPolicy.RUNTIME,
                    CsvColumn.class.getAnnotation(java.lang.annotation.Retention.class).value());
        }));
        tests.add(new Case("CSV заголовок визначений Java-іменами полів", () -> {
            Path path = tempfile(); CsvExporter.write(path, ScreeningRecord.class, List.of(record("Кіно")));
            equal("фільм,зал,продано,час,ціна", Files.readAllLines(path, StandardCharsets.UTF_8).get(0));
        }));
        tests.add(new Case("UTF-8 зберігає українські символи", () -> {
            Path path = tempfile(); CsvExporter.write(path, ScreeningRecord.class, List.of(record("Кіно Львів")));
            ok(Files.readString(path, StandardCharsets.UTF_8).contains("Кіно Львів"), "Загублено кирилицю");
        }));
        tests.add(new Case("Кома екранується в лапках", () -> {
            Path path = tempfile(); CsvExporter.write(path, ScreeningRecord.class, List.of(record("Космос, нова ера")));
            ok(Files.readString(path, StandardCharsets.UTF_8).contains("\"Космос, нова ера\""), "Не екрановано кому");
        }));
        tests.add(new Case("Подвійні лапки в назві подвоюються", () -> {
            Path path = tempfile(); CsvExporter.write(path, ScreeningRecord.class, List.of(record("Фільм \"Кіно\"")));
            ok(Files.readString(path, StandardCharsets.UTF_8).contains("\"Фільм \"\"Кіно\"\"\""), "Не екрановано лапки");
        }));
        tests.add(new Case("Round-trip відновлює всі поля", () -> {
            Path path = tempfile(); List<ScreeningRecord> items = List.of(record("Космос, нова ера"), record("Фільм \"Кіно\""));
            CsvExporter.write(path, ScreeningRecord.class, items);
            equal(items, ScreeningCsv.read(path));
        }));
        tests.add(new Case("Поле з перенесенням рядка відновлюється", () -> {
            Path path = tempfile(); var item = record("Перший рядок\nДругий рядок");
            CsvExporter.write(path, ScreeningRecord.class, List.of(item));
            equal(List.of(item), ScreeningCsv.read(path));
        }));
        tests.add(new Case("Порожній список дає лише CSV-заголовок", () -> {
            Path path = tempfile(); CsvExporter.write(path, ScreeningRecord.class, List.of());
            equal(List.of(), ScreeningCsv.read(path));
            equal(1, Files.readAllLines(path, StandardCharsets.UTF_8).size());
        }));
        tests.add(new Case("Неправильна кількість CSV-полів відхиляється", () -> {
            try { ScreeningRecord.fromFields(List.of("Кіно", "1")); throw new AssertionError("Рядок прийнято"); }
            catch (DataStorageException expected) { }
        }));
        tests.add(new Case("Неправильне число має cause", () -> {
            try { ScreeningRecord.fromFields(List.of("Кіно", "зал", "1", "2026-11-03T10:00", "100"));
                throw new AssertionError("Невірне число прийнято");
            } catch (DataStorageException expected) { ok(expected.getCause() != null, "Немає причини"); }
        }));
        tests.add(new Case("Неправильний заголовок відхиляється", () -> {
            Path path = tempfile(); Files.writeString(path,"неправильно,значення\n", StandardCharsets.UTF_8);
            try { ScreeningCsv.read(path); throw new AssertionError("Заголовок прийнято"); }
            catch (DataStorageException expected) { }
        }));
        tests.add(new Case("Незакриті лапки відхиляються", () -> {
            try { CsvParser.parseDocument("\"Незакриті"); throw new AssertionError("CSV прийнято"); }
            catch (DataStorageException expected) { }
        }));
        tests.add(new Case("Файлова помилка зберігає IOException", () -> {
            Path directory = Files.createTempDirectory("lab05-folder-");
            try { CsvExporter.write(directory, ScreeningRecord.class, List.of());
                throw new AssertionError("Запис в директорію дозволений");
            } catch (DataStorageException expected) {
                ok(expected.getCause() instanceof java.io.IOException, "Втрачено IOException");
            }
        }));
        tests.add(new Case("Старий звіт зберігає всі числові значення", () -> {
            CinemaReport report = CinemaReport.fromLines(Files.readAllLines(
                    Path.of("data", "input.csv"), StandardCharsets.UTF_8));
            equal(5, report.getCount()); equal(35200.0, report.getTotalRevenue());
            equal(142.0, report.getAveragePrice()); equal(80, report.getMaxSold());
        }));
        return List.copyOf(tests);
    }

    /** Виконує нові сценарії без Maven. */
    public static void main(String[] args) {
        int passed = 0;
        for (Case test : cases()) {
            try { test.check().run(); passed++; }
            catch (Exception | AssertionError error) {
                System.out.printf("FAIL: %s — %s%n", test.name(), error.getMessage());
            }
        }
        System.out.printf("Лабораторна робота № 5: %d / %d перевірок успішні.%n", passed, cases().size());
        if (passed != cases().size()) System.exit(1);
    }
}
