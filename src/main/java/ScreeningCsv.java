import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Відновлює конкретні ScreeningRecord із експортованого UTF-8 CSV. */
public final class ScreeningCsv {
    private static final List<String> HEADERS = List.of("фільм", "зал", "продано", "час", "ціна");
    private ScreeningCsv() { }

    /** Читає CSV-файл, перевіряє заголовок і відновлює записи. */
    public static List<ScreeningRecord> read(Path path) throws DataStorageException {
        String content;
        try {
            content = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException cause) {
            throw new DataStorageException("Не вдалося прочитати CSV: " + path, cause);
        }
        List<List<String>> rows = CsvParser.parseDocument(content);
        if (rows.isEmpty() || !rows.get(0).equals(HEADERS)) {
            throw new DataStorageException("CSV має неправильний заголовок");
        }
        List<ScreeningRecord> records = new ArrayList<>();
        for (int index = 1; index < rows.size(); index++) {
            try {
                records.add(ScreeningRecord.fromFields(rows.get(index)));
            } catch (DataStorageException cause) {
                throw new DataStorageException("Помилка CSV-запису " + (index + 1), cause);
            }
        }
        return List.copyOf(records);
    }
}
