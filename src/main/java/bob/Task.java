package bob;

import java.util.ArrayList;
import java.util.List;

/**
 * A single task the chatbot remembers: what the user wants to do,
 * and whether it has been done yet.
 *
 * <p>This class holds only what every kind of task has in common. The kinds the
 * chatbot supports — {@link Todo}, {@link Deadline} and {@link Event} — extend it
 * and add whatever is particular to them, so the shared parts are written once
 * here instead of being repeated three times.
 *
 * <p>It is {@code abstract} because "a task" on its own is not something the user
 * can add: every task the chatbot stores is one of the three kinds. Declaring it
 * abstract has the compiler enforce that, rather than leaving it to be remembered.
 */
public abstract class Task {

    /** Status field written to the save file for a task that has been done. */
    public static final String DONE_FLAG = "1";

    /** Status field written to the save file for a task that has not been done yet. */
    public static final String NOT_DONE_FLAG = "0";

    /** What the user typed when adding the task. */
    protected String description;

    /** Whether the task has been marked as done. */
    protected boolean isDone;

    /**
     * Creates a task that is not done yet, since a task the user has just
     * mentioned is something still to do.
     *
     * @param description what the user typed when adding the task
     */
    public Task(String description) {
        this.description = description;
        this.isDone = false;
    }

    /**
     * Returns the single character shown inside the first box of a listing,
     * identifying which kind of task this is: {@code T}, {@code D} or {@code E}.
     *
     * <p>Each subclass answers for itself, and {@link #toString()} calls this
     * without knowing which subclass it is talking to. That is polymorphism doing
     * the work: the listing code stays the same however many kinds of task exist.
     */
    public abstract String getTypeIcon();

    /**
     * Returns the single character shown inside the status box of a listing:
     * {@code X} for a task that is done, a space for one that is not.
     */
    public String getStatusIcon() {
        return (isDone ? "X" : " "); // mark done task with X
    }

    /** Records that the task has been done. */
    public void markAsDone() {
        this.isDone = true;
    }

    /** Records that the task is not done after all. */
    public void markAsNotDone() {
        this.isDone = false;
    }

    /**
     * Returns the task as the list of fields that {@link Storage} writes to the
     * save file: the kind of task, whether it has been done, and what it is.
     *
     * <p>Subclasses that remember more than that — a deadline's due date, an
     * event's start and end — override this and append to
     * {@code super.toSaveFields()}, exactly as they do for {@link #toString()},
     * so the three shared fields are listed in one place only.
     *
     * <p>The fields are returned separately rather than as one finished line of
     * text. Joining them, and protecting a field that itself contains the
     * separator, is then left to {@link Storage}, which is the one class that
     * knows the layout of the file.
     */
    public List<String> toSaveFields() {
        List<String> fields = new ArrayList<>();
        fields.add(getTypeIcon());
        fields.add(isDone ? DONE_FLAG : NOT_DONE_FLAG);
        fields.add(description);
        return fields;
    }

    /**
     * Returns the part of the task's display form that every kind of task shares,
     * for example {@code [T][X] read book}.
     *
     * <p>Subclasses that have something to add, such as a deadline's due date,
     * override this and append to {@code super.toString()}, so the type box,
     * status box and description are formatted in one place only.
     */
    @Override
    public String toString() {
        return "[" + getTypeIcon() + "][" + getStatusIcon() + "] " + description;
    }
}
