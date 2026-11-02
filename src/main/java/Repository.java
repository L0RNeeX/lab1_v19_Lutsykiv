import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** Типобезпечне сховище записів із незмінними знімками. */
public final class Repository<T> {
    private final List<T> items = new ArrayList<>();

    /** Додає запис заданого типу. */
    public void add(T item) {
        items.add(Objects.requireNonNull(item, "Запис не може бути null"));
    }

    /** Повертає незмінний знімок записів. */
    public List<T> all() {
        return List.copyOf(items);
    }

    /** Шукає записи, що відповідають заданій умові. */
    public List<T> find(Predicate<? super T> condition) {
        Objects.requireNonNull(condition, "Умова не може бути null");
        return items.stream().filter(condition).toList();
    }
}
