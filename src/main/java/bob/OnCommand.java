package bob;

import java.time.LocalDate;

/**
 * Prints the tasks falling on one day, in the order they appear in the list.
 *
 * <p>A deadline falls on the day it is due and an event on any day it is
 * running; a todo, having no date, never appears here. Which of those is which
 * is decided by each kind of task in {@link Task#occursOn}, so this command only
 * has to ask.
 *
 * <p>This command, {@link BeforeCommand} and {@link AfterCommand} are written as
 * three classes rather than one parent with three small subclasses. They are the
 * same shape but not the same code — each asks a different question of a task
 * and names it differently — and a parent holding the shape would leave each
 * subclass too thin to read on its own.
 */
public class OnCommand extends Command {

    /** The day the user asked about. */
    private final LocalDate day;

    /**
     * Creates a command that will list the tasks falling on one day.
     *
     * @param day the day to look at.
     */
    public OnCommand(LocalDate day) {
        this.day = day;
    }

    @Override
    public void execute(TaskList tasks, Ui ui, Storage storage) {
        String dayShown = TaskDate.formatDay(day);
        ui.showTasks(tasks, tasks.findIndexes(task -> task.occursOn(day)),
                "Here is what you have on " + dayShown + ":",
                "You have nothing on " + dayShown + ".");
    }
}
