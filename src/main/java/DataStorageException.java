/** Повідомляє про помилку збереження, читання або CSV-формату. */
public class DataStorageException extends Exception {
    /** Створює виняток із поясненням помилки. */
    public DataStorageException(String message) { super(message); }

    /** Створює виняток із поясненням і збереженою першопричиною. */
    public DataStorageException(String message, Throwable cause) { super(message, cause); }
}
