import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Експортує в UTF-8 CSV анотовані поля довільного плоского класу. */
public final class CsvExporter {
    private CsvExporter() { }

    /** Записує заголовок і записи у визначеному порядку полів. */
    public static <T> void write(Path path, Class<T> type, List<? extends T> records)
            throws DataStorageException {
        Objects.requireNonNull(path, "Шлях не може бути null");
        Objects.requireNonNull(type, "Тип не може бути null");
        Objects.requireNonNull(records, "Записи не можуть бути null");
        List<Field> fields = Arrays.stream(type.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(CsvColumn.class))
                .sorted(Comparator.comparing(Field::getName))
                .toList();
        if (fields.isEmpty()) {
            throw new DataStorageException("Клас не містить полів @CsvColumn");
        }
        for (Field field : fields) {
            if (!field.trySetAccessible()) {
                throw new DataStorageException("Немає доступу до поля " + field.getName());
            }
        }
        StringBuilder csv = new StringBuilder(fields.stream()
                .map(f -> escape(f.getAnnotation(CsvColumn.class).value()))
                .collect(Collectors.joining(","))).append(System.lineSeparator());
        for (T record : records) {
            if (record == null) throw new DataStorageException("Список містить null-запис");
            List<String> values = new ArrayList<>();
            for (Field field : fields) {
                try {
                    values.add(escape(field.get(record)));
                } catch (IllegalAccessException cause) {
                    throw new DataStorageException("Не вдалося прочитати поле " + field.getName(), cause);
                }
            }
            csv.append(String.join(",", values)).append(System.lineSeparator());
        }
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            Files.writeString(path, csv.toString(), StandardCharsets.UTF_8);
        } catch (IOException cause) {
            throw new DataStorageException("Не вдалося записати CSV: " + path, cause);
        }
    }

    /** Екранує коми, лапки та перенесення рядка відповідно до формату CSV. */
    private static String escape(Object value) {
        String text = Objects.toString(value, "");
        if (text.contains(",") || text.contains("\"") || text.contains("\r") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
