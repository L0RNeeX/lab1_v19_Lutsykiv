import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Консольна програма «Кінотеатр»: класи, record та інкапсуляція, варіант 19. */
public final class Lab02 {
    private Lab02() { }

    /**
     * Запускає програму з явним UTF-8 для обох потоків консолі.
     *
     * @param args --help або необов'язкові --input файл і --output файл
     */
    public static void main(String[] args) {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true,
                StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true,
                StandardCharsets.UTF_8));
        int code = run(args, System.out, System.err);
        if (code != 0) {
            System.exit(code);
        }
    }

    /** Виконує команду без завершення JVM, щоб файлові сценарії можна було тестувати. */
    static int run(String[] args, PrintStream outputStream, PrintStream errorStream) {
        if (args.length == 1 && "--help".equals(args[0])) {
            printHelp(outputStream);
            return 0;
        }
        try {
            Path[] paths = parseArguments(args);
            checkDifferentFiles(paths[0], paths[1]);
            CinemaReport result = CinemaReport.fromLines(readInput(paths[0]));
            String report = result.toText();
            writeReport(paths[1], report);
            outputStream.print(report);
            return 0;
        } catch (IllegalArgumentException exception) {
            errorStream.printf("Помилка параметрів: %s%n", exception.getMessage());
            errorStream.printf("Для довідки використайте --help.%n");
            return 2;
        } catch (IOException exception) {
            errorStream.printf("Помилка роботи з файлом: %s%n", exception.getMessage());
            return 1;
        }
    }

    /**
     * Перетворює аргументи на шляхи; без аргументів використовує типові файли.
     *
     * @param args аргументи командного рядка
     * @return масив із двох шляхів: вхідного та вихідного
     * @throws IllegalArgumentException якщо параметр невідомий або не має значення
     */
    static Path[] parseArguments(String[] args) {
        Path input = Path.of("data", "input.csv");
        Path output = Path.of("out", "report.txt");
        boolean inputSeen = false;
        boolean outputSeen = false;

        for (int index = 0; index < args.length; index++) {
            String option = args[index];
            if (!"--input".equals(option) && !"--output".equals(option)) {
                throw new IllegalArgumentException(
                        "невідомий параметр: %s".formatted(option));
            }
            if (index + 1 >= args.length || args[index + 1].startsWith("--")
                    || args[index + 1].isBlank()) {
                throw new IllegalArgumentException(
                        "після %s потрібно вказати шлях".formatted(option));
            }
            Path path = Path.of(args[++index]);
            if ("--input".equals(option)) {
                if (inputSeen) {
                    throw new IllegalArgumentException("параметр --input повторено");
                }
                input = path;
                inputSeen = true;
            } else {
                if (outputSeen) {
                    throw new IllegalArgumentException("параметр --output повторено");
                }
                output = path;
                outputSeen = true;
            }
        }
        return new Path[] {input, output};
    }

    /**
     * Захищає вхідні дані від випадкового перезапису звітом.
     * Перевіряє також різні шляхи, які посилаються на той самий наявний файл.
     *
     * @param input вхідний файл
     * @param output файл звіту
     * @throws IOException якщо не вдається порівняти файли
     * @throws IllegalArgumentException якщо обидва шляхи вказують на один файл
     */
    static void checkDifferentFiles(Path input, Path output) throws IOException {
        boolean equalPaths = input.toAbsolutePath().normalize()
                .equals(output.toAbsolutePath().normalize());
        boolean sameFile = Files.exists(input) && Files.exists(output)
                && Files.isSameFile(input, output);
        if (equalPaths || sameFile) {
            throw new IllegalArgumentException("вхідний файл і файл звіту мають відрізнятися");
        }
    }

    /**
     * Читає UTF-8 і прибирає необов'язковий маркер BOM на початку файла.
     *
     * @param input шлях до вхідного файла
     * @return усі фізичні рядки зі збереженням їхнього порядку
     * @throws IOException якщо файл недоступний або має неправильне кодування
     */
    static List<String> readInput(Path input) throws IOException {
        List<String> lines = Files.readAllLines(input, StandardCharsets.UTF_8);
        if (!lines.isEmpty() && lines.get(0).startsWith("\uFEFF")) {
            lines.set(0, lines.get(0).substring(1));
        }
        return lines;
    }

    /**
     * Створює батьківські каталоги та записує звіт у UTF-8.
     *
     * @param output шлях до файла звіту
     * @param report готовий текст
     * @throws IOException якщо каталог або файл неможливо створити чи записати
     */
    static void writeReport(Path output, String report) throws IOException {
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output, report, StandardCharsets.UTF_8);
    }


    /** Друкує синтаксис запуску та незмінний формат вхідних даних. */
    private static void printHelp(PrintStream output) {
        output.printf("Кінотеатр — лабораторна робота № 2, варіант 19, рівень 1.%n");
        output.printf("Запуск: java -cp build/classes Lab02 [--input файл] [--output файл]%n");
        output.printf("Довідка: java -cp build/classes Lab02 --help%n");
        output.printf("JAR: java -jar target/cinema-v19.jar [--input файл] [--output файл]%n");
        output.printf("Типовий вхід: data/input.csv; типовий вихід: out/report.txt.%n");
        output.printf("Формат UTF-8 без заголовка: film;hall;ticketPrice;sold;durationMin%n");
        output.printf("film — непорожній текст; hall і durationMin > 0; ticketPrice і sold >= 0.%n");
        output.printf("Ціна скінченна, десятковий роздільник — крапка. Роздільник полів — ;.%n");
        output.printf("Середня ціна = сума цін / кількість коректних сеансів.%n");
        output.printf("Відносні шляхи обчислюються від поточного робочого каталогу.%n");
        output.printf("Шляхи з пробілами беріть у подвійні лапки. --help запускайте окремо.%n");
    }
}
