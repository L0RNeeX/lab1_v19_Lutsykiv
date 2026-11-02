import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/** Лабораторна № 5: параметризоване сховище і CSV-експорт кіносеансів. */
public final class Lab05 {
    private Lab05() { }

    /** Для --export демонструє CSV, інші режими залишаються сумісними з ЛР4. */
    public static void main(String[] args) {
        if (args.length > 0 && "--export".equals(args[0])) {
            try (PrintStream output = new PrintStream(
                    new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8)) {
                if (args.length != 1 && (args.length != 3 || !"--output".equals(args[1]))) {
                    throw new IllegalArgumentException("Використання: --export [--output файл]");
                }
                Path path = args.length == 3 ? Path.of(args[2]) : Path.of("out", "screenings.csv");
                exportSample(path, output);
            } catch (DataStorageException | IllegalArgumentException error) {
                System.err.println("Помилка експорту: " + error.getMessage());
                System.exit(1);
            }
            return;
        }
        Lab04.main(args);
    }

    /** Готує шість записів варіанта 19 і перевіряє збереження значень. */
    public static void exportSample(Path path, PrintStream output) throws DataStorageException {
        Repository<ScreeningRecord> repository = new Repository<>();
        repository.add(new ScreeningRecord("Місто світла", 1, LocalDateTime.parse("2026-11-03T10:00"), 150.0, 80));
        repository.add(new ScreeningRecord("Подорож Карпатами", 2, LocalDateTime.parse("2026-11-03T12:30"), 180.0, 60));
        repository.add(new ScreeningRecord("Таємниця озера", 1, LocalDateTime.parse("2026-11-03T15:00"), 120.0, 50));
        repository.add(new ScreeningRecord("Космос, нова ера", 3, LocalDateTime.parse("2026-11-03T17:30"), 160.0, 40));
        repository.add(new ScreeningRecord("Фільм \"Повернення\"", 2, LocalDateTime.parse("2026-11-03T20:00"), 100.0, 0));
        repository.add(new ScreeningRecord("VIP: Нічний сеанс", 1, LocalDateTime.parse("2026-11-04T21:15"), 200.0, 15));
        CsvExporter.write(path, ScreeningRecord.class, repository.all());
        List<ScreeningRecord> restored = ScreeningCsv.read(path);
        output.printf(Locale.ROOT, "Лабораторна робота № 5 — Кінотеатр (варіант 19)%n");
        output.printf("Сховище Repository<ScreeningRecord>: %d записів%n", repository.all().size());
        output.printf("CSV: %s%n", path);
        output.printf("Заголовок: фільм,зал,продано,час,ціна%n");
        output.printf("Відновлено з UTF-8 CSV: %d записів%n", restored.size());
        output.printf("Збіг початкових і відновлених значень: %s%n", repository.all().equals(restored));
        output.printf("Сеансів у залі 1: %d%n",
                repository.find(item -> item.getHall() == 1).size());
        output.printf("Перший сеанс: %s, %s%n", restored.get(0).getFilm(), restored.get(0).getStartAt());
    }
}
