/**
 * A single task the chatbot remembers: what the user wants to do,
 * and whether it has been done yet.
 *
 * <p>Bundling the description and the done status into one object replaces the
 * two parallel arrays {@code Bob} used before, where slot {@code i} of one array
 * had to be kept lined up with slot {@code i} of the other by hand.
 */
public class Task {

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
     * Returns the task as it is shown to the user, for example
     * {@code [X] read book}.
     *
     * <p>Overriding {@code toString} rather than writing a separate formatting
     * method means the task prints correctly wherever it is used in a string,
     * such as {@code printLine("  " + task)}.
     */
    @Override
    public String toString() {
        return "[" + getStatusIcon() + "] " + description;
    }
}
