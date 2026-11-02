/** Дві доступні категорії кінопоказу. */
public enum ScreeningKind {
    REGULAR("звичайний"),
    PREMIUM("преміум");

    private final String label;

    ScreeningKind(String label) { this.label = label; }

    /** Повертає категорію для відображення. */
    public String label() { return label; }
}
