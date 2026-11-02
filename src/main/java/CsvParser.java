import java.util.ArrayList;
import java.util.List;

/** Розбирає CSV-документ, у тому числі лапки та багаторядкові поля. */
public final class CsvParser {
    private CsvParser() { }

    /** Повертає логічні CSV-записи, а не фізичні рядки файла. */
    public static List<List<String>> parseDocument(String text) throws DataStorageException {
        if (text == null) throw new DataStorageException("CSV-текст не може бути null");
        if (text.isEmpty()) return List.of();
        List<List<String>> result = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false, atStart = true, afterQuote = false;
        for (int index = 0; index < text.length(); index++) {
            char c = text.charAt(index);
            if (quoted) {
                if (c == '"') {
                    if (index + 1 < text.length() && text.charAt(index + 1) == '"') {
                        current.append('"'); index++;
                    } else { quoted = false; afterQuote = true; }
                } else { current.append(c); }
            } else if (c == '"') {
                if (!atStart || afterQuote) throw new DataStorageException("Неприпустимі CSV-лапки");
                quoted = true; atStart = false;
            } else if (c == ',' || c == '\n' || c == '\r') {
                row.add(current.toString()); current.setLength(0);
                atStart = true; afterQuote = false;
                if (c != ',') {
                    result.add(List.copyOf(row)); row = new ArrayList<>();
                    if (c == '\r' && index + 1 < text.length() && text.charAt(index + 1) == '\n') index++;
                }
            } else {
                if (afterQuote) throw new DataStorageException("Символ після закритих CSV-лапок");
                current.append(c); atStart = false;
            }
        }
        if (quoted) throw new DataStorageException("Незакриті CSV-лапки");
        if (!row.isEmpty() || !atStart || afterQuote) {
            row.add(current.toString()); result.add(List.copyOf(row));
        }
        return List.copyOf(result);
    }
}
