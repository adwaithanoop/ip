package bob.command;

import bob.storage.Storage;
import bob.task.TaskList;
import bob.ui.Ui;

/**
 * Says goodbye and ends the conversation.
 *
 * <p>This is the one command that answers {@link #isExit()} with {@code true}.
 * Everything about ending the conversation is therefore in this class: the loop
 * in {@link bob.Bob Bob} asks every command it runs whether that was the last one, and
 * only this one ever says yes.
 */
public class ExitCommand extends Command {

    /**
     * Prints the farewell. The list is left as it was, so there is nothing to save.
     */
    @Override
    public void execute(TaskList tasks, Ui ui, Storage storage) {
        ui.showFarewell();
    }

    /** Returns {@code true}: this is the one command that ends the conversation. */
    @Override
    public boolean isExit() {
        return true;
    }
}
