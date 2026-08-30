package bob;

import java.util.List;

/**
 * A task that runs from one point in time to another, for example
 * {@code project meeting (from: Aug 06 2026 14:00 to: Aug 06 2026 16:00)}.
 *
 * <p>As with {@link Deadline}, the start and the end are kept as {@link TaskDate}
 * values rather than as the text the user typed, so both are dates the chatbot
 * has understood.
 *
 * <p>Nothing here checks that the end comes after the start. The requirements do
 * not ask for it, and it is a separate question from understanding dates; it
 * could be added later in the command that builds the event, which is where the
 * user can still be told to retype it.
 */
public class Event extends Task {

    /** The letter that stands for an event, as {@link Todo#TYPE_ICON} does for a todo. */
    public static final String TYPE_ICON = "E";

    /** When the event starts. */
    protected TaskDate from;

    /** When the event ends. */
    protected TaskDate to;

    /**
     * Creates an event that is not done yet.
     *
     * @param description what the event is.
     * @param from        when it starts.
     * @param to          when it ends.
     */
    public Event(String description, TaskDate from, TaskDate to) {
        super(description);
        this.from = from;
        this.to = to;
    }

    @Override
    public String getTypeIcon() {
        return TYPE_ICON;
    }

    /**
     * Returns for example
     * {@code [E][ ] project meeting (from: Aug 06 2026 14:00 to: Aug 06 2026 16:00)}.
     */
    @Override
    public String toString() {
        return super.toString() + " (from: " + from + " to: " + to + ")";
    }

    /**
     * Adds the start and the end after the three fields every task saves.
     *
     * <p>They are added as two fields rather than joined into one, so that reading
     * them back is a matter of taking two fields apart rather than of splitting a
     * single field down the middle.
     */
    @Override
    public List<String> toSaveFields() {
        List<String> fields = super.toSaveFields();
        fields.add(from.toSaveField());
        fields.add(to.toSaveField());
        return fields;
    }
}
