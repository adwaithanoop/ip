package bob;

/**
 * A task that runs from one point in time to another, for example
 * {@code project meeting (from: Mon 2pm to: 4pm)}.
 *
 * <p>As with {@link Deadline}, the start and end are kept as the plain text the
 * user typed; the current requirements do not ask for real dates.
 */
public class Event extends Task {

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
        return "E";
    }

    /** Returns for example {@code [E][ ] project meeting (from: Mon 2pm to: 4pm)}. */
    @Override
    public String toString() {
        return super.toString() + " (from: " + from + " to: " + to + ")";
    }
}
