package bob;

import java.time.LocalDate;

/**
 * Prints the tasks falling after one day, in the order they appear in the list.
 *
 * <p>The mirror image of {@link BeforeCommand}: the named day itself is left out
 * here too, so a task pinned to an earlier day is found by {@code before}, one
 * pinned to a later day by {@code after}, and a task pinned to the day itself by
 * neither — that one is what {@code on} is for.
 *
 * <p>An event is placed by its start, as everywhere else, so an event that has
 * already begun is not listed as still to come even when it is running past the
 * day asked about. {@code on} finds that event, which is the question it answers.
 */
public class AfterCommand extends Command {

    /** The day the user asked about. */
    private final LocalDate day;

    /**
     * Creates a command that will list the tasks falling after one day.
     *
     * @param day the day to look after.
     */
    public AfterCommand(LocalDate day) {
        this.day = day;
    }

    @Override
    public void execute(TaskList tasks, Ui ui, Storage storage) {
        String dayShown = TaskDate.formatDay(day);
        ui.showTasks(tasks, tasks.findIndexes(task -> task.isAfter(day)),
                "Here is what you have after " + dayShown + ":",
                "You have nothing after " + dayShown + ".");
    }
}
