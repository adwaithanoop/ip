package bob.task;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * A task that runs from one point in time to another, for example
 * {@code project meeting (from: Aug 06 2026 14:00 to: Aug 06 2026 16:00)}.
 *
 * <p>As with {@link Deadline}, the start and the end are kept as {@link TaskDateTime}
 * values rather than as the text the user typed, so both are dates the chatbot
 * has understood.
 *
 * <p>Nothing here checks that the end comes after the start. That check lives in
 * {@link bob.parser.Parser Parser}, which reads the line the event was typed on, because that is
 * where there is still a user to tell about it: a class that could only throw
 * would leave the caller to turn the failure into something worth reading. The
 * consequence is that an event whose end comes first can still be built — by a
 * hand-edited save file, which is the one route into this class that does not
 * pass through the parser.
 */
public class Event extends Task {

    /** The letter that stands for an event, as {@link Todo#TYPE_ICON} does for a todo. */
    public static final String TYPE_ICON = "E";

    /**
     * When the event starts.
     *
     * <p>Private and {@code final}, as {@link Deadline}'s due date is. It matters
     * a little more here, because the two dates are only meaningful as a pair — an
     * event that ended before it started would be nonsense — and a pair that cannot
     * be half-changed cannot fall into that state after being checked.
     */
    private final TaskDateTime from;

    /** When the event ends. */
    private final TaskDateTime to;

    /**
     * Creates an event that is not done yet.
     *
     * @param description what the event is.
     * @param from        when it starts.
     * @param to          when it ends.
     */
    public Event(String description, TaskDateTime from, TaskDateTime to) {
        super(description);
        this.from = from;
        this.to = to;
    }

    /** Returns {@link #TYPE_ICON}, the {@code E} that marks an event. */
    @Override
    public String getTypeIcon() {
        return TYPE_ICON;
    }

    /**
     * Returns the start, which is the date an event is pinned to: it is when the
     * event first wants the user's attention, so it is what an event is ordered by
     * and what {@link bob.command.CommandWord#BEFORE CommandWord.BEFORE} and
     * {@link bob.command.CommandWord#AFTER CommandWord.AFTER} measure.
     */
    @Override
    public Optional<TaskDateTime> getScheduledDate() {
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
