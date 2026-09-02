package bob.task;

import java.util.List;
import java.util.Optional;

/**
 * A task that has to be done before a given point in time, for example
 * {@code return book (by: Dec 02 2026)}.
 *
 * <p>The due date is kept as a {@link TaskDate} rather than as the text the user
 * typed, so the chatbot understands when the task is due instead of merely
 * repeating what it was told. That is what lets the date be shown back in a
 * friendlier form than it was typed in, and what would let two deadlines be
 * compared. The text the user typed is not kept: everything about the date that
 * matters is in the {@code TaskDate}.
 */
public class Deadline extends Task {

    /** The letter that stands for a deadline, as {@link Todo#TYPE_ICON} does for a todo. */
    public static final String TYPE_ICON = "D";

    /** When the task is due. */
    protected TaskDate by;

    /**
     * Creates a deadline that is not done yet.
     *
     * @param description what the user has to do.
     * @param by          when it has to be done by.
     */
    public Deadline(String description, TaskDate by) {
        super(description);
        this.by = by;
    }

    /** Returns {@link #TYPE_ICON}, the {@code D} that marks a deadline. */
    @Override
    public String getTypeIcon() {
        return TYPE_ICON;
    }

    /**
     * Returns the due date, which is the date a deadline is pinned to: it is when
     * the task has to be dealt with, so it is what a deadline is listed under and
     * ordered by.
     */
    @Override
    public Optional<TaskDate> getScheduledDate() {
        return Optional.of(by);
    }

    /** Returns for example {@code [D][ ] return book (by: Dec 02 2026)}. */
    @Override
    public String toString() {
        return super.toString() + " (by: " + by + ")";
    }

    /** Adds the due date after the three fields every task saves. */
    @Override
    public List<String> toSaveFields() {
        List<String> fields = super.toSaveFields();
        fields.add(by.toSaveField());
        return fields;
    }
}
