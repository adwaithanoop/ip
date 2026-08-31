package bob.command;

import java.util.List;

import bob.storage.Storage;
import bob.task.TaskList;
import bob.ui.Ui;

/**
 * Prints the tasks with the soonest dates on them, most urgent first.
 *
 * <p>Unlike the other listings this one reorders what it shows, which
 * {@link TaskList#findIndexesSoonestFirst()} does without disturbing the list.
 *
 * <p>Fewer tasks than asked for are shown without complaint when the list does
 * not hold that many, and the heading says how many are actually there.
 */
public class NextCommand extends Command {

    /** How many tasks the user asked to see, always one or more. */
    private final int wantedCount;

    /**
     * Creates a command that will list the most urgent tasks.
     *
     * @param wantedCount how many to show at most.
     */
    public NextCommand(int wantedCount) {
        this.wantedCount = wantedCount;
    }

    @Override
    public void execute(TaskList tasks, Ui ui, Storage storage) {
        List<Integer> datedTaskIndexes = tasks.findIndexesSoonestFirst();
        int shownCount = Math.min(wantedCount, datedTaskIndexes.size());
        // When no task has a date the heading is built naming none, and never
        // printed: an empty selection is shown as the message below it instead.
        ui.showTasks(tasks, datedTaskIndexes.subList(0, shownCount),
                shownCount == 1
                        ? "Here is your most urgent task:"
                        : "Here are your " + shownCount + " most urgent tasks, soonest first:",
                "None of your tasks have a date on them yet.");
    }
}
