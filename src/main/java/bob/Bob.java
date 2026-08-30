package bob;

import java.nio.file.Path;
import java.time.LocalDate;
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
     */
    public void run() {
        ui.showGreeting();
        ui.showLoadReport(tasks.size(), loadMessages);
        handleCommandsUntilExit();
        ui.showFarewell();
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

    /**
     * Reads one command per line and responds to it in its own block,
     * stopping when the user types {@code bye}.
     *
     * <p>{@link CommandWord#BYE} is dealt with here rather than in
     * {@link #handleCommand} because it is the one command whose answer is not
     * printed as a block in the middle of the conversation: the farewell is
     * printed after the loop has ended.
     *
     * <p>The loop is guarded by {@link Ui#hasNextCommand()} rather than looping
     * forever, so the program also ends cleanly if the input runs out (for
     * example when input is piped in from a file that has no {@code bye} line).
     *
     * <p>Both ways out of the loop lead to the same line closing the input, which
     * is why the {@code bye} branch breaks out rather than returning: there is one
     * exit, so there is one place that has to remember to stop reading.
     *
     * <p>This is also the one place where a {@link BobException} is caught. Any
     * command that cannot be carried out reports itself by throwing, the message
     * is printed here, and the loop simply goes on to read the next line — so a
     * mistake costs the user a line, not the whole conversation.
     */
    private void handleCommandsUntilExit() {
        while (ui.hasNextCommand()) {
            String line = ui.readCommand();
            if (CommandWord.BYE.matches(line)) {
                break;
            }
            ui.openBlock();
            try {
                handleCommand(line);
            } catch (BobException e) {
                ui.showError(e.getMessage());
            }
            ui.closeBlock();
        }
        ui.close();
    }

    /**
     * Carries out one command from the user.
     *
     * <p>Kept separate from the reading loop above so that the loop is only about
     * reading lines, and this method is only about what each line means.
     *
     * <p>Working out which command the line is, and what its arguments mean, is
     * left to {@link Parser}. What is left here is a {@code switch} saying what
     * each command does — one branch per command, with the command's name rather
     * than its spelling on the label, so the list of branches can be read against
     * the list of constants in the enum.
     *
     * <p>The three commands that add a task read alike: the parser is asked for
     * the task the line describes, and {@link #addTask} stores whichever kind it
     * turns out to be. They needed a method apiece while each one picked its own
     * arguments apart.
     *
     * @param line one whole line as the user typed it, with surrounding spaces removed.
     * @throws BobException if the line is not a command the chatbot knows, or is
     *                      one it knows but cannot carry out as written.
     */
    private void handleCommand(String line) throws BobException {
        Parser.ParsedCommand parsed = Parser.parseCommand(line);
        String arguments = parsed.arguments();
        switch (parsed.command()) {
            case LIST -> showTasks();
            case ON -> showTasksOn(arguments);
            case BEFORE -> showTasksBefore(arguments);
            case AFTER -> showTasksAfter(arguments);
            case NEXT -> showNextTasks(arguments);
            case MARK -> setTaskDone(arguments, true);
            case UNMARK -> setTaskDone(arguments, false);
            case DELETE -> deleteTask(arguments);
            case TODO -> addTask(Parser.parseTodo(arguments));
            case DEADLINE -> addTask(Parser.parseDeadline(arguments));
            case EVENT -> addTask(Parser.parseEvent(arguments));
            // Listed so that every constant of the enum is accounted for here.
            // The read loop returns on bye before calling this method, so a line
            // reaching this branch would mean that loop had stopped doing so.
            case BYE -> throw new IllegalStateException(
                    "bye should have ended the read loop before reaching here");
        }
    }

    /**
     * Stores an already-built task and confirms it back to the user.
     *
     * <p>The parameter is a {@link Task}, so this one method stores todos,
     * deadlines and events alike; printing the task calls whichever
     * {@code toString} the actual object has.
     *
     * @throws BobException if the list could not be written to disk afterwards.
     */
    private void addTask(Task task) throws BobException {
        tasks.add(task);
        ui.showAddedTask(task, tasks.size());
        saveTasks();
    }

    /**
     * Writes the task list to disk, so that it survives the end of the session.
     *
     * <p>Called after every command that changes the list, and after the change
     * has been confirmed to the user. That order is deliberate: the change really
     * has been made to the list either way, so the confirmation is true even when
     * the save fails, and the failure is then reported after it rather than
     * instead of it.
     *
     * @throws BobException if the file could not be written, carrying the reason
     *                      and a warning that the change will not outlive the session.
     */
    private void saveTasks() throws BobException {
        storage.save(tasks.asList());
    }

    /**
     * Removes the task the user named and shows it one last time, so the user can
     * see which task is gone rather than having to work it out from the numbering.
     *
     * <p>{@link TaskList#delete} closes the gap left behind, so the numbers shown
     * by {@link CommandWord#LIST} stay a run of 1, 2, 3 with nothing missing. That is
     * why the confirmation shows the task itself: after a deletion the number the
     * user typed refers to a different task than it did before.
     *
     * @param taskNumberText the task number as the user typed it, counting from 1.
     * @throws BobException if the number is missing, is not a number, names no task,
     *                      or if the shortened list could not be written to disk.
     */
    private void deleteTask(String taskNumberText) throws BobException {
        int taskIndex = requireTaskIndex(taskNumberText, CommandWord.DELETE);
        // delete returns the task it took out, so it can be shown without
        // having to be fetched separately beforehand.
        Task removed = tasks.delete(taskIndex);
        ui.showRemovedTask(removed, tasks.size());
        saveTasks();
    }

    /**
     * Sets the done status of the task the user named and shows it back to them.
     * Both {@link CommandWord#MARK} and {@link CommandWord#UNMARK} share this method,
     * since they differ only in the status they set and the wording they report.
     *
     * @param taskNumberText the task number as the user typed it, counting from 1
     *                       to match the numbering shown by {@link CommandWord#LIST}.
     * @param isDone         {@code true} to mark the task as done,
     *                       {@code false} to mark it as not done yet.
     * @throws BobException if no task number was given, if what was given is not a
     *                      number, if no task has that number, or if the changed
     *                      list could not be written to disk.
     */
    private void setTaskDone(String taskNumberText, boolean isDone) throws BobException {
        CommandWord command = isDone ? CommandWord.MARK : CommandWord.UNMARK;
        Task task = tasks.get(requireTaskIndex(taskNumberText, command));
        if (isDone) {
            task.markAsDone();
        } else {
            task.markAsNotDone();
        }
        ui.showMarkedTask(task, isDone);
        saveTasks();
    }

    /**
     * Returns the position in {@link #tasks} of the task the user named, having
     * first checked that they named one and that it exists.
     *
     * <p>Reading the number out of what the user typed is left to
     * {@link Parser#parseTaskNumber}. What is added here is the half of the
     * question that only the task list can answer: whether there is a task with
     * that number to act on. {@link CommandWord#MARK}, {@link CommandWord#UNMARK} and
     * {@link CommandWord#DELETE} all need that check, so it is written here once
     * instead of being repeated in each of them.
     *
     * @param taskNumberText the task number as the user typed it, counting from 1
     *                       to match the numbering shown by {@link CommandWord#LIST}.
     * @param command        the command to name in any error message.
     * @return the position of that task in {@link #tasks}, counting from 0.
     * @throws BobException if no task number was given, if what was given is not a
     *                      number, or if no task has that number.
     */
    private int requireTaskIndex(String taskNumberText, CommandWord command) throws BobException {
        String word = command.getKeyword();
        String listCommand = CommandWord.LIST.getKeyword();
        int taskNumber = Parser.parseTaskNumber(taskNumberText, command);
        if (tasks.isEmpty()) {
            throw new BobException("There is nothing to " + word
                    + " yet — your list is empty.");
        }
        if (taskNumber < 1 || taskNumber > tasks.size()) {
            throw new BobException("I don't have a task numbered " + taskNumber + "."
                    + "\nYour list runs from 1 to " + tasks.size() + "; type " + listCommand
                    + " to see it.");
        }
        // The user counts from 1, the list counts from 0.
        return taskNumber - 1;
    }

    /** Prints the stored tasks as a numbered list, counting from 1 for readability. */
    private void showTasks() {
        ui.showTasks(tasks, tasks.allIndexes(),
                "Here are the tasks in your list:",
                "You haven't told me about any tasks yet.");
    }

    /**
     * Prints the tasks falling on one day, in the order they appear in the list.
     *
     * <p>A deadline falls on the day it is due and an event on any day it is
     * running; a todo, having no date, never appears here. Which of those is which
     * is decided by each kind of task in {@link Task#occursOn}, so this method only
     * has to ask.
     *
     * @param dayText the day as the user typed it after {@code on}.
     * @throws BobException if no day was given, or what was given is not a day.
     */
    private void showTasksOn(String dayText) throws BobException {
        LocalDate day = Parser.parseDay(dayText, CommandWord.ON);
        String dayShown = TaskDate.formatDay(day);
        ui.showTasks(tasks, tasks.findIndexes(task -> task.occursOn(day)),
                "Here is what you have on " + dayShown + ":",
                "You have nothing on " + dayShown + ".");
    }

    /**
     * Prints the tasks falling before one day, in the order they appear in the list.
     *
     * <p>The named day itself is not included, so {@code before} and {@code on} for
     * the same day never show the same task twice. Someone wanting both can ask for
     * the day after.
     *
     * @param dayText the day as the user typed it after {@code before}.
     * @throws BobException if no day was given, or what was given is not a day.
     */
    private void showTasksBefore(String dayText) throws BobException {
        LocalDate day = Parser.parseDay(dayText, CommandWord.BEFORE);
        String dayShown = TaskDate.formatDay(day);
        ui.showTasks(tasks, tasks.findIndexes(task -> task.isBefore(day)),
                "Here is what you have before " + dayShown + ":",
                "You have nothing before " + dayShown + ".");
    }

    /**
     * Prints the tasks falling after one day, in the order they appear in the list.
     *
     * <p>The mirror image of {@link #showTasksBefore}: the named day itself is left
     * out here too, so a task pinned to an earlier day is found by {@code before},
     * one pinned to a later day by {@code after}, and a task pinned to the day
     * itself by neither — that one is what {@code on} is for.
     *
     * <p>An event is placed by its start, as everywhere else, so an event that has
     * already begun is not listed as still to come even when it is running past the
     * day asked about. {@code on} finds that event, which is the question it
     * answers.
     *
     * @param dayText the day as the user typed it after {@code after}.
     * @throws BobException if no day was given, or what was given is not a day.
     */
    private void showTasksAfter(String dayText) throws BobException {
        LocalDate day = Parser.parseDay(dayText, CommandWord.AFTER);
        String dayShown = TaskDate.formatDay(day);
        ui.showTasks(tasks, tasks.findIndexes(task -> task.isAfter(day)),
                "Here is what you have after " + dayShown + ":",
                "You have nothing after " + dayShown + ".");
    }

    /**
     * Prints the tasks with the soonest dates on them, most urgent first.
     *
     * <p>Unlike the other listings this one reorders what it shows, which
     * {@link TaskList#findIndexesSoonestFirst()} does without disturbing the list.
     *
     * <p>Fewer tasks than asked for are shown without complaint when the list does
     * not hold that many, and the heading says how many are actually there.
     *
     * @param countText how many tasks to show, as the user typed it after {@code next}.
     * @throws BobException if no count was given, or what was given is not a count
     *                      of one or more.
     */
    private void showNextTasks(String countText) throws BobException {
        int wantedCount = Parser.parseCount(countText);
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
