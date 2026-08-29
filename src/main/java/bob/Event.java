package bob;

import java.util.List;

/**
 * A task that runs from one point in time to another, for example
 * {@code project meeting (from: Mon 2pm to: 4pm)}.
 *
 * <p>As with {@link Deadline}, the start and end are kept as the plain text the
 * user typed; the current requirements do not ask for real dates.
 */
public class Event extends Task {

    /** The letter that stands for an event, as {@link Todo#TYPE_ICON} does for a todo. */
    public static final String TYPE_ICON = "E";

    /** When the event starts, exactly as the user typed it after {@code /from}. */
    protected String from;

    /** When the event ends, exactly as the user typed it after {@code /to}. */
    protected String to;

    /**
     * Creates an event that is not done yet.
     *
     * @param description what the event is
     * @param from        when it starts
     * @param to          when it ends
     */
    public Event(String description, String from, String to) {
        super(description);
        this.from = from;
        this.to = to;
    }

    @Override
    public String getTypeIcon() {
        return TYPE_ICON;
    }

    /** Returns for example {@code [E][ ] project meeting (from: Mon 2pm to: 4pm)}. */
    @Override
    public String toString() {
        return super.toString() + " (from: " + from + " to: " + to + ")";
    }

    /**
     * Adds the start and the end after the three fields every task saves.
     *
     * <p>They are added as two fields rather than joined into one, so that reading
     * them back is a matter of taking two fields apart rather than of splitting a
     * time the user was free to write any way they liked.
     */
    @Override
    public List<String> toSaveFields() {
        List<String> fields = super.toSaveFields();
        fields.add(from);
        fields.add(to);
        return fields;
    }
}
