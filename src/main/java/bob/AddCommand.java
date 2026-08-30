package bob;

/**
 * Adds one task to the list.
 *
 * <p>One class serves {@link CommandWord#TODO}, {@link CommandWord#DEADLINE} and
 * {@link CommandWord#EVENT} alike, because what it holds is a {@link Task} and
 * the three differ only in which kind of task that is. {@link Parser} has
 * already decided which kind and built it, so there is nothing left here that
 * varies between them: the task is stored, shown back, and saved the same way
 * whichever it is.
 */
public class AddCommand extends Command {

    /** The task to add, already built from what the user typed. */
    private final Task task;

    /**
     * Creates a command that will add one task.
     *
     * @param task the task to add.
     */
    public AddCommand(Task task) {
        this.task = task;
    }

    @Override
    public void execute(TaskList tasks, Ui ui, Storage storage) throws BobException {
        tasks.add(task);
        ui.showAddedTask(task, tasks.size());
        storage.save(tasks.asList());
    }
}
