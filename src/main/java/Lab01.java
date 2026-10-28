import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Обробляє записи про кіносеанси для лабораторної роботи № 1.
 * Варіант 19, рівень 1: перевірка рядків, чотири показники та текстовий звіт.
 * Формат запису: film;hall;ticketPrice;sold;durationMin.
 */
public final class Lab01 {
    /** Утилітарному класу не потрібні екземпляри. */
    private Lab01() {
    }

    /**
     * Читає аргументи, обробляє файл і виводить той самий звіт у файл та консоль.
     * Код завершення: 0 — файл оброблено або показано довідку,
     * 1 — помилка читання чи запису, 2 — неправильні аргументи.
     * Хибні рядки у доступному файлі не змінюють код завершення 0.
     *
     * @param args параметри --help, --input шлях, --output шлях
     */
    public static void main(String[] args) {
        // Консоль, як і файли, отримує байти UTF-8.
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        if (args.length == 1 && "--help".equals(args[0])) {
            printHelp();
            return;
        }

        try {
            Path[] paths = parseArguments(args);
            Path input = paths[0];
            Path output = paths[1];
            checkDifferentFiles(input, output);
            List<String> lines = readInput(input);
            String report = analyzeLines(lines);
            writeReport(output, report);
            System.out.print(report);
        } catch (IllegalArgumentException exception) {
            System.err.printf("Помилка параметрів: %s%n", exception.getMessage());
            System.err.printf("Для довідки використайте --help.%n");
            System.exit(2);
        } catch (IOException exception) {
            System.err.printf("Помилка роботи з файлом: %s%n", exception.getMessage());
            System.exit(1);
        }
    }

    /**
     * Перетворює аргументи на шляхи; без аргументів використовує типові файли.
     *
     * @param args аргументи командного рядка
     * @return масив із двох шляхів: вхідного та вихідного
     * @throws IllegalArgumentException якщо параметр невідомий або не має значення
     */
    private static Path[] parseArguments(String[] args) {
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
    private static void checkDifferentFiles(Path input, Path output) throws IOException {
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
    private static List<String> readInput(Path input) throws IOException {
        List<String> lines = Files.readAllLines(input, StandardCharsets.UTF_8);
        if (!lines.isEmpty() && lines.get(0).startsWith("\uFEFF")) {
            lines.set(0, lines.get(0).substring(1));
        }
        return lines;
    }

    /**
     * Перевіряє записи та обчислює показники лише за коректними рядками.
     * Середня ціна — арифметичне середнє цін сеансів, без ваги sold.
     *
     * @param lines рядки вхідного файла
     * @return сформований текст звіту з показниками та діагностикою
     */
    private static String analyzeLines(List<String> lines) {
        int validCount = 0;
        double totalRevenue = 0.0;
        double totalPrices = 0.0;
        int maxSold = 0;
        List<String> errors = new ArrayList<>();

        for (int index = 0; index < lines.size(); index++) {
            try {
                String[] fields = splitRecord(lines.get(index));
                parseIntegerField(fields[1], "hall", 1);
                double price = parsePrice(fields[2]);
                int sold = parseIntegerField(fields[3], "sold", 0);
                parseIntegerField(fields[4], "durationMin", 1);

                double nextRevenue = totalRevenue + price * sold;
                double nextPrices = totalPrices + price;
                if (!Double.isFinite(nextRevenue) || !Double.isFinite(nextPrices)) {
                    throw new IllegalArgumentException("завеликі числа спричиняють переповнення");
                }
                // Змінюємо статистику тільки після всіх перевірок цього рядка.
                validCount++;
                totalRevenue = nextRevenue;
                totalPrices = nextPrices;
                maxSold = Math.max(maxSold, sold);
            } catch (IllegalArgumentException exception) {
                errors.add("Рядок %d: %s.".formatted(index + 1, exception.getMessage()));
            }
        }

        double averagePrice = validCount == 0 ? 0.0 : totalPrices / validCount;
        return formatReport(validCount, totalRevenue, averagePrice, maxSold, errors);
    }

    /**
     * Перевіряє структуру запису та обрізає пробіли на краях полів.
     *
     * @param line один рядок CSV без заголовка
     * @return п'ять полів запису
     * @throws IllegalArgumentException якщо структура або назва фільму неправильні
     */
    private static String[] splitRecord(String line) {
        if (line.isBlank()) {
            throw new IllegalArgumentException("порожній рядок");
        }
        // -1 зберігає порожнє останнє поле для подальшої перевірки.
        String[] fields = line.split(";", -1);
        if (fields.length != 5) {
            throw new IllegalArgumentException(
                    "очікується 5 полів, отримано %d".formatted(fields.length));
        }
        for (int index = 0; index < fields.length; index++) {
            fields[index] = fields[index].strip();
        }
        if (fields[0].isEmpty()) {
            throw new IllegalArgumentException("поле \"film\" не може бути порожнім");
        }
        return fields;
    }

    /**
     * Читає ціле число та перевіряє його нижню допустиму межу.
     *
     * @param text текстове значення
     * @param name назва поля для повідомлення про помилку
     * @param minimum найменше допустиме значення: 0 або 1
     * @return коректне число типу int
     * @throws IllegalArgumentException якщо число не ціле, завелике або менше межі
     */
    private static int parseIntegerField(String text, String name, int minimum) {
        final int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "поле \"%s\" має бути цілим числом у межах int".formatted(name));
        }
        if (value < minimum) {
            throw new IllegalArgumentException(
                    "поле \"%s\" має бути не менше %d".formatted(name, minimum));
        }
        return value;
    }

    /**
     * Читає невід'ємну скінченну ціну; NaN та нескінченність не є ціною.
     *
     * @param text ціна з крапкою як десятковим роздільником
     * @return коректна ціна типу double
     * @throws IllegalArgumentException якщо ціна має неправильний формат або значення
     */
    private static double parsePrice(String text) {
        final double value;
        try {
            value = Double.parseDouble(text);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("поле \"ticketPrice\" має бути числом із крапкою");
        }
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(
                    "поле \"ticketPrice\" має бути скінченним невід'ємним числом");
        }
        return value;
    }

    /**
     * Форматує обчислені показники з крапкою та двома дробовими знаками.
     * Для порожнього набору середнє й максимум позначає як відсутні.
     *
     * @param count кількість коректних записів
     * @param revenue загальний виторг
     * @param average середня ціна квитка
     * @param maxSold найбільша кількість проданих квитків за сеанс
     * @param errors повідомлення про пропущені рядки
     * @return один текст для консолі та файла
     */
    private static String formatReport(int count, double revenue, double average,
                                       int maxSold, List<String> errors) {
        String averageText = count == 0 ? "немає даних"
                : String.format(Locale.ROOT, "%.2f грн", average);
        String maxText = count == 0 ? "немає даних" : Integer.toString(maxSold);
        StringBuilder report = new StringBuilder(String.format(Locale.ROOT,
                "Лабораторна робота № 1 — Кінотеатр%n"
                + "Варіант: 19; рівень: 1%n%n"
                + "Коректних записів: %d%n"
                + "Загальний виторг: %.2f грн%n"
                + "Середня ціна квитка: %s%n"
                + "Найбільша кількість проданих квитків: %s%n"
                + "Пропущених рядків: %d%n",
                count, revenue, averageText, maxText, errors.size()));
        if (count == 0) {
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

    /**
     * Створює батьківські каталоги та записує звіт у UTF-8.
     *
     * @param output шлях до файла звіту
     * @param report готовий текст
     * @throws IOException якщо каталог або файл неможливо створити чи записати
     */
    private static void writeReport(Path output, String report) throws IOException {
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output, report, StandardCharsets.UTF_8);
    }

    /** Друкує синтаксис запуску та правила формату вхідного файла. */
    private static void printHelp() {
        System.out.printf("Кінотеатр — лабораторна робота № 1, варіант 19, рівень 1.%n");
        System.out.printf("Запуск: java -cp build/classes Lab01 [--input файл] [--output файл]%n");
        System.out.printf("Довідка: java -cp build/classes Lab01 --help%n");
        System.out.printf("Типовий вхід: data/input.csv; типовий вихід: out/report.txt.%n");
        System.out.printf("Формат UTF-8 без заголовка: film;hall;ticketPrice;sold;durationMin%n");
        System.out.printf("film — непорожній текст; hall і durationMin > 0; price і sold >= 0.%n");
        System.out.printf("Ціна скінченна, десятковий роздільник — крапка. Роздільник полів — ;.%n");
        System.out.printf("Середня ціна = сума цін / кількість коректних сеансів.%n");
        System.out.printf("Відносні шляхи обчислюються від поточного робочого каталогу.%n");
        System.out.printf("Шляхи з пробілами беріть у подвійні лапки. --help запускайте окремо.%n");
    }
}
