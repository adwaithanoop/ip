package bob;

/**
 * A task that has to be done before a given point in time, for example
 * {@code return book (by: Sunday)}.
 *
 * <p>The due date is kept as the plain text the user typed rather than being
 * converted into a date object. That is all the current requirements ask for,
 * and it lets the user write anything at all, including {@code no idea :-p}.
 * A later increment can parse it into a real date so that dates can be compared
 * and reformatted.
 */
public class Deadline extends Task {

    /** When the task is due, exactly as the user typed it after {@code /by}. */
    protected String by;

    /**
     * Creates a deadline that is not done yet.
     *
     * @param description what the user has to do
     * @param by          when it has to be done by
     */
    public Deadline(String description, String by) {
        super(description);
        this.by = by;
    }

    @Override
    public String getTypeIcon() {
        return "D";
    }

    /** Returns for example {@code [D][ ] return book (by: Sunday)}. */
    @Override
    public String toString() {
        return super.toString() + " (by: " + by + ")";
    }
}
