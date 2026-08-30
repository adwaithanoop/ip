package bob;

import java.time.LocalDate;

/**
 * Prints the tasks falling before one day, in the order they appear in the list.
 *
 * <p>The named day itself is not included, so {@code before} and {@code on} for
 * the same day never show the same task twice. Someone wanting both can ask for
 * the day after.
 */
public class BeforeCommand extends Command {

    /** The day the user asked about. */
    private final LocalDate day;

    /**
     * Creates a command that will list the tasks falling before one day.
     *
     * @param day the day to look before.
     */
    public BeforeCommand(LocalDate day) {
        this.day = day;
    }

    @Override
    public void execute(TaskList tasks, Ui ui, Storage storage) {
        String dayShown = TaskDate.formatDay(day);
        ui.showTasks(tasks, tasks.findIndexes(task -> task.isBefore(day)),
                "Here is what you have before " + dayShown + ":",
                "You have nothing before " + dayShown + ".");
    }
}
