package bob;

/**
 * A task with no date or time attached to it, for example
 * {@code borrow book}. It adds nothing to {@link Task} beyond its
 * type icon, since there is nothing more to remember about it.
 */
public class Todo extends Task {

    /**
     * Creates a todo that is not done yet.
     *
     * @param description what the user wants to do
     */
    public Todo(String description) {
        super(description);
    }

    @Override
    public String getTypeIcon() {
        return "T";
    }
}
