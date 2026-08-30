package bob;

import java.nio.file.Path;
import java.util.List;

/**
 * A chatbot that greets the user, remembers the tasks the user types,
 * lists them back on request, marks them as done, removes the ones the user
 * no longer wants, and exits when the user types {@code bye}.
 *
 * <p>Tasks come in three kinds — {@link Todo}, {@link Deadline} and
 * {@link Event} — each added with its own command word. The words the chatbot
 * understands are listed in {@link CommandWord}; making sense of a whole line
 * written with one of them is {@link Parser}'s work, and this class is left to
 * say what each command does once it has been understood.
 *
 * <p>A chatbot is an object rather than a class of static members. It has three
 * things it works with — a {@link Ui}, a {@link Storage} and a {@link TaskList} —
 * and something that has state of its own is what an object is for. Making them
 * fields of an instance also means there can be more than one: a second chatbot
 * can be pointed at a different save file, which is what makes this class
 * testable at all, and what a graphical front end would need in order to hold a
 * chatbot of its own.
 *
 * <p>Nothing here writes to the console or reads from the keyboard directly.
 * Every line the user sees is handed to {@link Ui}, and every line they type
 * comes back from it, so this class is left deciding <em>what</em> to say while
 * {@code Ui} decides how it looks on screen.
 *
 * <p>The tasks themselves are held by {@link TaskList}, which is also asked for
 * the searches the listing commands need. This class no longer knows that they
 * sit in a list at all — it asks for a task by its number and is given one.
 *
 * <p>The task list outlives a single run: it is read from a file on startup and
 * written back whenever it changes, so the user finds their tasks where they
 * left them. All of that is done by {@link Storage}; this class only says when
 * to load and when to save.
 *
 * <p>A deadline and an event carry dates the chatbot understands rather than
 * text it merely repeats: each is read into a {@link TaskDate}, which is what
 * lets a date be shown back in a friendlier form than it was typed in.
 *
 * <p>Because those dates are understood, the chatbot can be asked about them
 * rather than only told them: {@link CommandWord#ON}, {@link CommandWord#BEFORE} and
 * {@link CommandWord#AFTER} pick out the tasks falling on, before, or after a given
 * day, and {@link CommandWord#NEXT} shows the few with the soonest dates on them.
 * All four are views of the one task list — they change nothing, so nothing is
 * saved after them — and each shows a task with the number it has in
 * {@link CommandWord#LIST}, so a task found this way can be marked or deleted
 * without looking it up again.
 *
 * <p>Anything the user types that cannot be carried out — an unknown command,
 * a missing description, a task number that does not exist, a due date that is
 * not a date — is reported by throwing a {@link BobException} carrying the
 * explanation. Most of those are thrown by {@link Parser}, which reads what the
 * user wrote; the one exception is a task number that is a number but names no
 * task, since only this class knows how long the list is. The command loop
 * catches all of them alike and prints the message, so a mistyped command
 * never ends the conversation.
 * A failure to save is reported the same way, so it is seen rather than passing
 * silently, and the conversation carries on.
 */
public class Bob {

    /** Everything printed to the user, and every line read back from them. */
    private final Ui ui;

    /** The file the task list is read from and written back to. */
    private final Storage storage;

    /** The tasks the user has told the chatbot about, and everything done to them. */
    private final TaskList tasks;

    /**
     * What {@link Storage} had to say about reading the save file, kept until
     * there is a good moment to say it.
     *
     * <p>The tasks are read in the constructor but the report on reading them is
     * not printed there, because the greeting has to come first and a constructor
     * that printed would be deciding when its own output appeared. Holding the
     * messages leaves that decision to {@link #run()}, where the order of the
     * conversation is set out.
     */
    private final List<String> loadMessages;

    /**
     * Creates a chatbot that keeps its tasks in one named file, picking up
     * whatever was saved there last time.
     *
     * <p>Everything this chatbot works with is settled here and never swapped
     * afterwards, so every field is {@code final} and the compiler enforces it.
     * That was not possible while the class was made of static fields: the task
     * list had to start as an empty placeholder and be replaced once the save
     * file had been read.
     *
     * <p>The file is a parameter rather than a constant reached at startup, so a
     * second chatbot can be pointed somewhere else — at a scratch file in a test,
     * for instance — without disturbing the one the user is talking to.
     *
     * <p>{@link Storage#load()} does not throw: a chatbot that cannot read its
     * save file can still be used for the rest of the session, so a problem with
     * the file comes back as a message to report rather than as a failure to
     * start. That is why nothing here is wrapped in a {@code try}.
     *
     * @param filePath where to keep the task list, for example
     *                 {@link Storage#DEFAULT_FILE_PATH}.
     */
    public Bob(Path filePath) {
        ui = new Ui();
        storage = new Storage(filePath);
        Storage.LoadResult result = storage.load();
        tasks = new TaskList(result.tasks());
        loadMessages = result.messages();
    }

    /**
     * Holds the conversation: greets the user, reports on the tasks picked up
     * from last time, answers commands until they say goodbye, then signs off.
     *
     * <p>Every line is read, made sense of, and carried out in the same three
     * steps, whatever the user typed. This method no longer asks which command it
     * is holding: {@link Parser} returns a {@link Command}, and running it is a
     * call to {@link Command#execute}. That is what lets a command be added to
     * the chatbot without this method changing at all.
     *
     * <p>Each answer is framed in its own block, opened before the line is
     * understood so that a complaint about it is framed like any other answer.
     * The block is closed in a {@code finally}, so it closes whether the command
     * ran or threw.
     *
     * <p>This is the one place where a {@link BobException} is caught. Any
     * command that cannot be carried out reports itself by throwing, the message
     * is printed here, and the loop goes on to read the next line — so a mistake
     * costs the user a line, not the whole conversation.
     *
     * <p>The loop ends either because a command said it was the last one, or
     * because the input ran out — which happens when it is piped in from a file
     * with no {@code bye} on the end. Only the first prints a farewell of its
     * own, through {@link ExitCommand}, so the second is given one here. A user
     * whose input simply stopped is still owed a sign-off.
     */
    public void run() {
        ui.showGreeting();
        ui.showLoadReport(tasks.size(), loadMessages);
        boolean isExit = false;
        while (!isExit && ui.hasNextCommand()) {
            String fullCommand = ui.readCommand();
            ui.openBlock();
            try {
                Command command = Parser.parse(fullCommand);
                command.execute(tasks, ui, storage);
                isExit = command.isExit();
            } catch (BobException e) {
                ui.showError(e.getMessage());
            } finally {
                ui.closeBlock();
            }
        }
        ui.close();
        if (!isExit) {
            ui.openBlock();
            ui.showFarewell();
            ui.closeBlock();
        }
    }

    /**
     * Starts a chatbot on the usual save file and talks to whoever runs it.
     *
     * <p>This is the one thing that has to be {@code static}, since it is called
     * before there is an object to call it on. It makes one and steps aside.
     *
     * @param args command line arguments, which this chatbot does not use.
     */
    public static void main(String[] args) {
        new Bob(Storage.DEFAULT_FILE_PATH).run();
    }
}
