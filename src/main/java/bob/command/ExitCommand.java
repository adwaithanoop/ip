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

    @Override
    public void execute(TaskList tasks, Ui ui, Storage storage) {
        ui.showFarewell();
    }

    @Override
    public boolean isExit() {
        return true;
    }
}
