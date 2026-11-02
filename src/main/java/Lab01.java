/** Зберігає сумісність зі старою командою запуску після рефакторингу. */
public final class Lab01 {
    private Lab01() { }

    /**
     * Делегує запуск оновленій реалізації без дублювання предметної логіки.
     *
     * @param args аргументи командного рядка
     */
    public static void main(String[] args) {
        Lab02.main(args);
    }
}
