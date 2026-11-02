import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Точка входу четвертої лабораторної; сумісний звіт і демонстрація трьох Stream API-запитів. */
public final class Lab04 {
    private static final int SOLD_THRESHOLD = 40;
    private Lab04() { }

    /**
     * Без аргументів і для старих опцій зберігає поведінку попереднього проєкту.
     * Опція --queries запускає окрему демонстрацію потокових запитів.
     *
     * @param args параметри командного рядка
     */
    public static void main(String[] args) {
        if (args.length == 1 && "--queries".equals(args[0])) {
            try (PrintStream output = new PrintStream(
                    new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8)) {
                Path input = Path.of("data", "stream.csv");
                List<String> lines = Files.readAllLines(input, StandardCharsets.UTF_8);
                CinemaReport report = CinemaReport.fromLines(lines);
                String text = formatQueries(report);
                output.print(text);
                Path destination = Path.of("out", "queries.txt");
                Files.createDirectories(destination.toAbsolutePath().getParent());
                Files.writeString(destination, text, StandardCharsets.UTF_8);
            } catch (IOException | IllegalArgumentException exception) {
                System.err.println("Помилка демонстрації Stream API: " + exception.getMessage());
                System.exit(1);
            }
            return;
        }
        Lab02.main(args);
    }

    /** Формує текст лише демонстрації; стандартний звіт не змінює. */
    public static String formatQueries(CinemaReport report) {
        List<Screening> screenings = report.getScreenings();
        String popular = ScreeningQueries.soldAbove(screenings, SOLD_THRESHOLD)
                .stream()
                .map(screening -> "%s (%d)".formatted(screening.getFilm(), screening.getSold()))
                .collect(Collectors.joining(", ", "[", "]"));
        List<String> names = ScreeningQueries.filmNames(screenings);
        Map<Integer, Double> grouped = ScreeningQueries.revenueByHall(screenings);
        String halls = grouped.entrySet().stream()
                .map(entry -> String.format(Locale.ROOT, "зал %d = %.2f грн",
                        entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("; "));
        return String.format(Locale.ROOT, """
                Лабораторна робота № 4 — Stream API (варіант 19)
                Дані: data/stream.csv
                Коректних сеансів: %d
                Пропущено рядків: %d
                Поріг sold: понад %d
                Відбір (filter): %s
                Назви фільмів (map): %s
                Виторг за залом (groupingBy): %s
                """, report.getCount(), report.getErrors().size(), SOLD_THRESHOLD,
                popular, names, halls.isEmpty() ? "немає даних" : halls);
    }
}
