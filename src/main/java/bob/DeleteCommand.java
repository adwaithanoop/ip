package bob;

/**
 * Removes one task from the list.
 *
 * <p>{@link TaskList#delete} closes the gap left behind, so the numbers shown by
 * {@link CommandWord#LIST} stay a run of 1, 2, 3 with nothing missing. That is
 * why the task itself is shown back rather than just its number: after a
 * deletion the number the user typed refers to a different task than it did
 * before.
 */
public class DeleteCommand extends TaskNumberCommand {

    /**
     * Creates a command that will remove one task.
     *
     * @param taskNumber the number the user typed, counting from 1.
     */
    public DeleteCommand(int taskNumber) {
        super(taskNumber, CommandWord.DELETE);
    }

    @Override
    public void execute(TaskList tasks, Ui ui, Storage storage) throws BobException {
        // delete returns the task it took out, so it can be shown without
        // having to be fetched separately beforehand.
        Task removed = tasks.delete(requireTaskIndex(tasks));
        ui.showRemovedTask(removed, tasks.size());
        storage.save(tasks.asList());
    }
}
