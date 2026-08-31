package bob;

/**
 * Marks one task as done, or as not done again.
 *
 * <p>{@link CommandWord#MARK} and {@link CommandWord#UNMARK} share this class
 * because they differ only in the status they set and the line they report. A
 * class apiece would be two copies of the same five lines, distinguished by a
 * word.
 */
public class MarkCommand extends TaskNumberCommand {

    /** The status to set: {@code true} for done, {@code false} for not done yet. */
    private final boolean isDone;

    /**
     * Creates a command that will set one task's done status.
     *
     * @param taskNumber the number the user typed, counting from 1.
     * @param isDone     {@code true} to mark the task as done, {@code false} to
     *                   mark it as not done yet.
     */
    public MarkCommand(int taskNumber, boolean isDone) {
        super(taskNumber, isDone ? CommandWord.MARK : CommandWord.UNMARK);
        this.isDone = isDone;
    }

    @Override
    public void execute(TaskList tasks, Ui ui, Storage storage) throws BobException {
        Task task = tasks.get(requireTaskIndex(tasks));
        if (isDone) {
            task.markAsDone();
        } else {
            task.markAsNotDone();
        }
        ui.showMarkedTask(task, isDone);
        storage.save(tasks.asList());
    }
}
