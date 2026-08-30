package bob;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
     * Returns the start, which is the date an event is pinned to: it is when the
     * event first wants the user's attention, so it is what an event is ordered by
     * and what {@link Command#BEFORE} and {@link Command#AFTER} measure.
     */
    @Override
    public Optional<TaskDate> getScheduledDate() {
        return Optional.of(from);
    }

    /**
     * Returns whether the event is running on {@code day}, counting the day it
     * starts and the day it ends as days it is running.
     *
     * <p>Widened from the inherited single-date test because an event, unlike a
     * deadline, covers a stretch of time. Asking what is on the Wednesday of a
     * week-long orientation should find it, and the inherited version — which
     * compares only the start — would not.
     */
    @Override
    public boolean occursOn(LocalDate day) {
        return !from.isAfter(day) && !to.isBefore(day);
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
