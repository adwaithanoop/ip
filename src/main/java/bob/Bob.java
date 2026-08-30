package bob;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.function.Predicate;

/**
 * A chatbot that greets the user, remembers the tasks the user types,
 * lists them back on request, marks them as done, removes the ones the user
 * no longer wants, and exits when the user types {@code bye}.
 *
 * <p>Tasks come in three kinds — {@link Todo}, {@link Deadline} and
 * {@link Event} — each added with its own command word. The words the chatbot
 * understands are listed in {@link Command}, which also decides which one a
 * given line is; this class is left to say what each of them does.
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
 * rather than only told them: {@link Command#ON} and {@link Command#BEFORE} pick
 * out the tasks falling on, or before, a given day, and {@link Command#NEXT}
 * shows the few with the soonest dates on them. All three are views of the one
 * task list — they change nothing, so nothing is saved after them — and each
 * shows a task with the number it has in {@link Command#LIST}, so a task found
 * this way can be marked or deleted without looking it up again.
 *
 * <p>Anything the user types that cannot be carried out — an unknown command,
 * a missing description, a task number that does not exist, a due date that is
 * not a date — is reported by throwing a {@link BobException} carrying the
 * explanation. The command loop catches it and prints it, so a mistyped command
 * never ends the conversation.
 * A failure to save is reported the same way, so it is seen rather than passing
 * silently, and the conversation carries on.
 */
public class Bob {

    /** Name the chatbot introduces itself with. */
    private static final String NAME = "Bob";

    /** Marker separating a deadline's description from its due date. */
    private static final String BY_KEYWORD = "/by";

    /** Marker separating an event's description from its start. */
    private static final String FROM_KEYWORD = "/from";

    /** Marker separating an event's start from its end. */
    private static final String TO_KEYWORD = "/to";

    /**
     * Example of a well-formed {@link Command#DEADLINE} command, shown when one is malformed.
     *
     * <p>The date in it is taken from {@link TaskDate}, which is the class that
     * decides how a date may be written, so this example cannot drift out of step
     * with the dates the chatbot actually accepts.
     */
    private static final String DEADLINE_EXAMPLE =
            Command.DEADLINE.getKeyword() + " return book " + BY_KEYWORD + " " + TaskDate.EXAMPLE_DATE;

    /**
     * The end time shown in {@link #EVENT_EXAMPLE}, two hours after the start.
     *
     * <p>Written out here rather than taken from {@link TaskDate}, which offers a
     * single example date and not a pair of them. It is the one example date in
     * this class that is spelled out, so if the accepted form of a date ever
     * changes, this is the one line to change with it.
     */
    private static final String EVENT_EXAMPLE_END = "2026-12-02 2000";

    /** Example of a well-formed {@link Command#EVENT} command, shown when one is malformed. */
    private static final String EVENT_EXAMPLE =
            Command.EVENT.getKeyword() + " project meeting " + FROM_KEYWORD + " "
                    + TaskDate.EXAMPLE_DATE_TIME + " " + TO_KEYWORD + " " + EVENT_EXAMPLE_END;

    /** Example of a well-formed {@link Command#ON} command, shown when one is malformed. */
    private static final String ON_EXAMPLE =
            Command.ON.getKeyword() + " " + TaskDate.EXAMPLE_DATE;

    /** Example of a well-formed {@link Command#BEFORE} command, shown when one is malformed. */
    private static final String BEFORE_EXAMPLE =
            Command.BEFORE.getKeyword() + " " + TaskDate.EXAMPLE_DATE;

    /** How many tasks {@link #NEXT_EXAMPLE} asks for. */
    private static final int NEXT_EXAMPLE_COUNT = 3;

    /** Example of a well-formed {@link Command#NEXT} command, shown when one is malformed. */
    private static final String NEXT_EXAMPLE =
            Command.NEXT.getKeyword() + " " + NEXT_EXAMPLE_COUNT;

    /** Leading whitespace that sets the chatbot's output apart from the user's input. */
    private static final String INDENT = "    ";

    /**
     * Horizontal rule separating blocks of output.
     * Built with {@code repeat} so the width is stated as a number
     * rather than as a row of underscores that has to be counted by eye.
     */
    private static final String DIVIDER = "_".repeat(60);

    /**
     * Tasks entered so far, in the order they were added.
     *
     * <p>This is an {@link ArrayList} rather than a fixed-size array. Now that
     * tasks can be deleted as well as added, a plain array would mean growing it
     * by hand when it filled up, and shifting every later element down by one on
     * every deletion while tracking how many slots are still in use. An
     * {@code ArrayList} does all of that itself: {@code add} grows it,
     * {@code remove} closes the gap, and {@code size} always says how many tasks
     * there are, so there is no separate count that could drift out of step with
     * the contents.
     *
     * <p>Each element is a {@link Task}. Because {@link Todo}, {@link Deadline}
     * and {@link Event} are all subclasses of {@link Task}, one list of
     * {@code Task} can hold any mixture of the three, and the code that lists,
     * marks or deletes them needs to know only that each is a task.
     */
    private static final ArrayList<Task> tasks = new ArrayList<>();

    /**
     * The file the task list is read from and written back to.
     *
     * <p>Named in {@code camelCase} rather than as a constant: the reference
     * cannot be reassigned, but a {@code Storage} is not immutable, and the
     * coding standard keeps {@code SCREAMING_SNAKE_CASE} for values that are.
     */
    private static final Storage storage = new Storage(Storage.DEFAULT_FILE_PATH);

    /**
     * ASCII-art banner shown when the chatbot starts: its name in the figlet
     * "slant" font, drifting through a field of stars with a few small
     * spaceships. Stored as one line per element so each line can be indented
     * the same way as every other line the chatbot prints.
     *
     * <p>No line is wider than the {@link #DIVIDER} that frames each block of
     * output, so the art never spills past the right-hand end of the rule.
     *
     * <p>Each backslash is written twice because a backslash starts an escape
     * sequence in a Java string literal; {@code \\} is the escape that means one
     * literal backslash, so the doubled ones here print singly.
     *
     * <p>Held in an immutable {@link List#of} list rather than an array. The coding
     * standard reserves the {@code SCREAMING_SNAKE_CASE} name for constants, and a
     * {@code static final String[]} is not one — the reference cannot be reassigned,
     * but any code could still overwrite an element. An immutable list cannot be
     * changed at all, so the name is honest about what it holds.
     */
    private static final List<String> BANNER = List.of(
            "  .        *         .        .        *        .",
            "      *         .         +        .       <]==-     .",
            "   .        +        ____        __      .        *",
            " -==[>  *           / __ )____  / /_         +",
            " +           .     / __  / __ \\/ __ \\  *              .",
            "          *       / /_/ / /_/ / /_/ /   <]==-   .",
            "    .         +  /_____/\\____/_.___/       .        *",
            "        +         .         *        .        +        .",
            "   .        -==[>      .         *                 .");

    /**
     * Starts the chatbot: greets the user, picks up the tasks left from last
     * time, answers commands until they say goodbye, then signs off.
     *
     * @param args command line arguments, which this chatbot does not use.
     */
    public static void main(String[] args) {
        showGreeting();
        loadTasks();
        handleCommandsUntilExit();
        showFarewell();
    }

    /**
     * Fills the task list with whatever was saved last time, and reports anything
     * the user should know about the file it came from.
     *
     * <p>Nothing is printed on the two ordinary cases — no save file yet, or a
     * save file that read cleanly and was empty — because a user who has nothing
     * saved does not need to be told about a file they have never seen.
     *
     * <p>{@link Storage#load()} does not throw: a chatbot that cannot read its
     * save file can still be used for the rest of the session, so a problem with
     * the file comes back as a message to print rather than as a failure to start.
     */
    private static void loadTasks() {
        Storage.LoadResult result = storage.load();
        tasks.addAll(result.tasks());
        if (tasks.isEmpty() && result.messages().isEmpty()) {
            return;
        }
        openBlock();
        if (!tasks.isEmpty()) {
            printLine("Welcome back! I've picked up " + tasks.size()
                    + (tasks.size() == 1 ? " task" : " tasks") + " you saved earlier.");
        }
        for (String message : result.messages()) {
            printLine(message);
        }
        closeBlock();
    }

    /** Prints the banner and welcome message as one block. */
    private static void showGreeting() {
        openBlock();
        for (String bannerLine : BANNER) {
            printLine(bannerLine);
        }
        printLine("Hello! I'm " + NAME + ".");
        printLine("What can I do for you?");
        closeBlock();
    }

    /**
     * Reads one command per line and responds to it in its own block,
     * stopping when the user types {@code bye}.
     *
     * <p>{@link Command#BYE} is dealt with here rather than in
     * {@link #handleCommand} because it is the one command whose answer is not
     * printed as a block in the middle of the conversation: the farewell is
     * printed after the loop has ended.
     *
     * <p>The loop is guarded by {@code hasNextLine} rather than looping forever,
     * so the program also ends cleanly if the input runs out (for example when
     * input is piped in from a file that has no {@code bye} line).
     *
     * <p>This is also the one place where a {@link BobException} is caught. Any
     * command that cannot be carried out reports itself by throwing, the message
     * is printed here, and the loop simply goes on to read the next line — so a
     * mistake costs the user a line, not the whole conversation.
     */
    private static void handleCommandsUntilExit() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (Command.BYE.matches(line)) {
                    return;
                }
                openBlock();
                try {
                    handleCommand(line);
                } catch (BobException e) {
                    showError(e);
                }
                closeBlock();
            }
        }
    }

    /**
     * Carries out one command from the user.
     *
     * <p>Kept separate from the reading loop above so that the loop is only about
     * reading lines, and this method is only about what each line means.
     *
     * <p>Working out which command the line is, and where its arguments start,
     * is left to {@link Command}. What is left here is a {@code switch} saying
     * what each command does — one branch per command, with the command's name
     * rather than its spelling on the label, so the list of branches can be read
     * against the list of constants in the enum.
     *
     * @param line one whole line as the user typed it, with surrounding spaces removed.
     * @throws BobException if the line is not a command the chatbot knows, or is
     *                      one it knows but cannot carry out as written.
     */
    private static void handleCommand(String line) throws BobException {
        if (line.isEmpty()) {
            throw new BobException("You didn't type anything."
                    + "\nTell me about a task, or type " + Command.LIST.getKeyword()
                    + " to see the ones I already have.");
        }
        // orElseThrow unwraps the Optional when a command was recognized, and
        // throws the "I don't know what that means" error when none was.
        Command command = Command.of(line).orElseThrow(() -> unknownCommand(line));
        String arguments = command.argumentsIn(line);
        switch (command) {
            case LIST -> showTasks();
            case ON -> showTasksOn(arguments);
            case BEFORE -> showTasksBefore(arguments);
            case NEXT -> showNextTasks(arguments);
            case MARK -> setTaskDone(arguments, true);
            case UNMARK -> setTaskDone(arguments, false);
            case DELETE -> deleteTask(arguments);
            case TODO -> addTodo(arguments);
            case DEADLINE -> addDeadline(arguments);
            case EVENT -> addEvent(arguments);
            // Listed so that every constant of the enum is accounted for here.
            // The read loop returns on bye before calling this method, so a line
            // reaching this branch would mean that loop had stopped doing so.
            case BYE -> throw new IllegalStateException(
                    "bye should have ended the read loop before reaching here");
        }
    }

    /**
     * Adds a {@link Todo} from the text following {@link Command#TODO}.
     * Everything the user typed is the description.
     *
     * @throws BobException if no description was given.
     */
    private static void addTodo(String arguments) throws BobException {
        if (arguments.isEmpty()) {
            throw new BobException("A todo needs a description — tell me what to do."
                    + "\nFor example: " + Command.TODO.getKeyword() + " borrow book");
        }
        addTask(new Todo(arguments));
    }

    /**
     * Adds a {@link Deadline} from the text following {@link Command#DEADLINE},
     * which is the description and the due date separated by {@value #BY_KEYWORD}.
     *
     * <p>The three things that can be missing — the {@value #BY_KEYWORD} marker,
     * the description before it, the date after it — are reported separately, so
     * the user is told which one to add rather than just that the command is wrong.
     *
     * <p>The due date is handed to {@link TaskDate#parse} rather than stored as
     * typed, so a deadline is only added once its date has been understood. Text
     * that is not a date is refused there, with its own explanation.
     *
     * @throws BobException if the marker, the description or the due date is
     *                      missing, or if the due date is not a date.
     */
    private static void addDeadline(String arguments) throws BobException {
        int byIndex = arguments.indexOf(BY_KEYWORD);
        if (byIndex < 0) {
            throw new BobException("A deadline needs a due date, written after " + BY_KEYWORD + "."
                    + "\nFor example: " + DEADLINE_EXAMPLE);
        }
        String description = arguments.substring(0, byIndex).trim();
        String by = arguments.substring(byIndex + BY_KEYWORD.length()).trim();
        if (description.isEmpty()) {
            throw new BobException("A deadline needs a description, written before "
                    + BY_KEYWORD + "." + "\nFor example: " + DEADLINE_EXAMPLE);
        }
        if (by.isEmpty()) {
            throw new BobException("You wrote " + BY_KEYWORD + " but not when it is due."
                    + "\nFor example: " + DEADLINE_EXAMPLE);
        }
        addTask(new Deadline(description, TaskDate.parse(by)));
    }

    /**
     * Adds an {@link Event} from the text following {@link Command#EVENT},
     * which is the description, then {@value #FROM_KEYWORD} and the start,
     * then {@value #TO_KEYWORD} and the end.
     *
     * <p>The end marker is looked for after the start marker, so that a
     * {@value #TO_KEYWORD} appearing earlier in the description is not
     * mistaken for the separator.
     *
     * <p>As with a deadline, both times are handed to {@link TaskDate#parse}, so
     * an event is only added once the chatbot has understood when it runs.
     *
     * @throws BobException if a marker, the description, the start or the end is
     *                      missing, or if the start or the end is not a date.
     */
    private static void addEvent(String arguments) throws BobException {
        int fromIndex = arguments.indexOf(FROM_KEYWORD);
        if (fromIndex < 0) {
            throw new BobException("An event needs a start time, written after " + FROM_KEYWORD + "."
                    + "\nFor example: " + EVENT_EXAMPLE);
        }
        int toIndex = arguments.indexOf(TO_KEYWORD, fromIndex);
        if (toIndex < 0) {
            throw new BobException("An event needs an end time, written after " + TO_KEYWORD
                    + " at the end." + "\nFor example: " + EVENT_EXAMPLE);
        }
        String description = arguments.substring(0, fromIndex).trim();
        String from = arguments.substring(fromIndex + FROM_KEYWORD.length(), toIndex).trim();
        String to = arguments.substring(toIndex + TO_KEYWORD.length()).trim();
        if (description.isEmpty()) {
            throw new BobException("An event needs a description, written before "
                    + FROM_KEYWORD + "." + "\nFor example: " + EVENT_EXAMPLE);
        }
        if (from.isEmpty() || to.isEmpty()) {
            throw new BobException("An event needs a time on both sides: one after "
                    + FROM_KEYWORD + " and one after " + TO_KEYWORD + "."
                    + "\nFor example: " + EVENT_EXAMPLE);
        }
        addTask(new Event(description, TaskDate.parse(from), TaskDate.parse(to)));
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
    private static void addTask(Task task) throws BobException {
        tasks.add(task);
        printLine("Got it. I've added this task:");
        printLine("  " + task);
        printLine("Now you have " + tasks.size() + " tasks in the list.");
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
    private static void saveTasks() throws BobException {
        storage.save(tasks);
    }

    /**
     * Removes the task the user named and shows it one last time, so the user can
     * see which task is gone rather than having to work it out from the numbering.
     *
     * <p>The tasks after it move up to fill the gap, so the numbers shown by
     * {@link Command#LIST} stay a run of 1, 2, 3 with nothing missing. That
     * renumbering is why the confirmation shows the task itself: after a deletion
     * the number the user typed refers to a different task than it did before.
     *
     * @param taskNumberText the task number as the user typed it, counting from 1.
     * @throws BobException if the number is missing, is not a number, names no task,
     *                      or if the shortened list could not be written to disk.
     */
    private static void deleteTask(String taskNumberText) throws BobException {
        int taskIndex = requireTaskIndex(taskNumberText, Command.DELETE);
        // remove returns the task it took out, so it can be shown without
        // having to be fetched separately beforehand.
        Task removed = tasks.remove(taskIndex);
        printLine("Noted. I've removed this task:");
        printLine("  " + removed);
        printLine("Now you have " + tasks.size() + " tasks in the list.");
        saveTasks();
    }

    /**
     * Returns the error to throw for a line that is not one of the commands the
     * chatbot knows, listing the ones it does know so the user can pick one.
     */
    private static BobException unknownCommand(String line) {
        return new BobException("Sorry, I don't know what \"" + line + "\" means."
                + "\nTry one of: " + Command.allKeywords());
    }

    /**
     * Sets the done status of the task the user named and shows it back to them.
     * Both {@link Command#MARK} and {@link Command#UNMARK} share this method,
     * since they differ only in the status they set and the wording they report.
     *
     * @param taskNumberText the task number as the user typed it, counting from 1
     *                       to match the numbering shown by {@link Command#LIST}.
     * @param isDone         {@code true} to mark the task as done,
     *                       {@code false} to mark it as not done yet.
     * @throws BobException if no task number was given, if what was given is not a
     *                      number, if no task has that number, or if the changed
     *                      list could not be written to disk.
     */
    private static void setTaskDone(String taskNumberText, boolean isDone) throws BobException {
        Command command = isDone ? Command.MARK : Command.UNMARK;
        Task task = tasks.get(requireTaskIndex(taskNumberText, command));
        if (isDone) {
            task.markAsDone();
        } else {
            task.markAsNotDone();
        }
        printLine(isDone
                ? "Nice! I've marked this task as done:"
                : "OK, I've marked this task as not done yet:");
        printLine("  " + task);
        saveTasks();
    }

    /**
     * Returns the position in {@link #tasks} of the task the user named, having
     * first checked that they named one and that it exists.
     *
     * <p>{@link Command#MARK}, {@link Command#UNMARK} and {@link Command#DELETE}
     * all take a task number and all reject the same three mistakes, so the
     * checking lives here once instead of being repeated in each of them.
     *
     * <p>The command is passed as a {@link Command} rather than as its keyword,
     * so a caller cannot name a command in the message that does not exist. The
     * keyword is read off it here, where the message is written.
     *
     * @param taskNumberText the task number as the user typed it, counting from 1
     *                       to match the numbering shown by {@link Command#LIST}.
     * @param command        the command to name in any error message.
     * @return the position of that task in {@link #tasks}, counting from 0.
     * @throws BobException if no task number was given, if what was given is not a
     *                      number, or if no task has that number.
     */
    private static int requireTaskIndex(String taskNumberText, Command command) throws BobException {
        String word = command.getKeyword();
        String listCommand = Command.LIST.getKeyword();
        if (taskNumberText.isEmpty()) {
            throw new BobException("Which task should I " + word + "?"
                    + "\nGive me its number from " + listCommand + ", for example: "
                    + word + " 2");
        }
        int taskNumber;
        try {
            // The user is free to type anything after the command word, so a
            // number is asked for again rather than allowed to crash the chatbot.
            taskNumber = Integer.parseInt(taskNumberText);
        } catch (NumberFormatException e) {
            throw new BobException("\"" + taskNumberText + "\" isn't a task number."
                    + "\nI need the number shown next to the task in " + listCommand
                    + ", for example: " + word + " 2");
        }
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
    private static void showTasks() {
        if (tasks.isEmpty()) {
            printLine("You haven't told me about any tasks yet.");
            return;
        }
        printLine("Here are the tasks in your list:");
        for (int i = 0; i < tasks.size(); i++) {
            printLine(formatNumberedTask(i));
        }
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
    private static void showTasksOn(String dayText) throws BobException {
        LocalDate day = requireDay(dayText, ON_EXAMPLE);
        String dayShown = TaskDate.formatDay(day);
        showMatchingTasks("Here is what you have on " + dayShown + ":",
                "You have nothing on " + dayShown + ".",
                task -> task.occursOn(day));
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
    private static void showTasksBefore(String dayText) throws BobException {
        LocalDate day = requireDay(dayText, BEFORE_EXAMPLE);
        String dayShown = TaskDate.formatDay(day);
        showMatchingTasks("Here is what you have before " + dayShown + ":",
                "You have nothing before " + dayShown + ".",
                task -> task.isBefore(day));
    }

    /**
     * Prints the tasks with the soonest dates on them, most urgent first.
     *
     * <p>Unlike the other two listings this one reorders what it shows, so the
     * positions of the dated tasks are sorted rather than the tasks themselves.
     * Sorting {@link #tasks} instead would answer the question just as well and
     * quietly renumber the user's whole list as a side effect.
     *
     * <p>Fewer tasks than asked for are shown without complaint when the list does
     * not hold that many, and the heading says how many are actually there.
     *
     * @param countText how many tasks to show, as the user typed it after {@code next}.
     * @throws BobException if no count was given, or what was given is not a count
     *                      of one or more.
     */
    private static void showNextTasks(String countText) throws BobException {
        int wantedCount = requireCount(countText);
        List<Integer> datedTaskIndexes = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getScheduledDate().isPresent()) {
                datedTaskIndexes.add(i);
            }
        }
        if (datedTaskIndexes.isEmpty()) {
            printLine("None of your tasks have a date on them yet.");
            return;
        }
        // orElseThrow cannot fire: only tasks that have a date are in this list.
        datedTaskIndexes.sort(Comparator.comparing(
                index -> tasks.get(index).getScheduledDate().orElseThrow()));
        int shownCount = Math.min(wantedCount, datedTaskIndexes.size());
        printLine(shownCount == 1
                ? "Here is your most urgent task:"
                : "Here are your " + shownCount + " most urgent tasks, soonest first:");
        for (int i = 0; i < shownCount; i++) {
            printLine(formatNumberedTask(datedTaskIndexes.get(i)));
        }
    }

    /**
     * Prints the tasks the caller wants, or says there are none.
     *
     * <p>{@link Command#ON} and {@link Command#BEFORE} differ only in which tasks
     * they want and what they call them, so the walking of the list and the choice
     * between a listing and an "I found nothing" line are written here once.
     *
     * <p>Which tasks are wanted arrives as a {@link Predicate}: a question about a
     * task that can be passed to a method and asked there. Passing the test itself
     * is what lets one method serve both commands; the alternative — a flag saying
     * which command called, and a {@code switch} on it here — would put each
     * command's meaning somewhere other than in the command.
     *
     * @param heading      the line introducing the tasks, printed only if there are any.
     * @param emptyMessage the line to print instead when no task matches.
     * @param isWanted     the test a task has to pass to be listed.
     */
    private static void showMatchingTasks(String heading, String emptyMessage,
            Predicate<Task> isWanted) {
        List<String> matchingLines = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) {
            if (isWanted.test(tasks.get(i))) {
                matchingLines.add(formatNumberedTask(i));
            }
        }
        if (matchingLines.isEmpty()) {
            printLine(emptyMessage);
            return;
        }
        printLine(heading);
        for (String line : matchingLines) {
            printLine(line);
        }
    }

    /**
     * Returns one task written as a line of a listing, for example
     * {@code 2.[D][ ] return book (by: Dec 02 2026)}.
     *
     * <p>The number is the task's place in the whole list, not its place among the
     * ones being shown. That is what makes a shortened listing useful: a number
     * read off it can be typed straight into {@code mark}, {@code unmark} or
     * {@code delete}, which is not true of numbers that count the matches.
     *
     * @param index the task's position in {@link #tasks}, counting from 0.
     */
    private static String formatNumberedTask(int index) {
        // The user counts from 1, the list counts from 0.
        return (index + 1) + "." + tasks.get(index);
    }

    /**
     * Returns the day the user asked about, having checked that they named one and
     * that it is a day this chatbot can read.
     *
     * <p>{@link Command#ON} and {@link Command#BEFORE} both take a day and reject
     * the same two mistakes, so the checking lives here once. Only the example
     * differs, and it is passed in so that each command is shown its own.
     *
     * @param dayText the day as the user typed it after the command word.
     * @param example a well-formed use of the command that asked, to show the user.
     * @return the day that text names.
     * @throws BobException if nothing was typed after the command word, or what was
     *                      typed is not a day.
     */
    private static LocalDate requireDay(String dayText, String example) throws BobException {
        if (dayText.isEmpty()) {
            throw new BobException("Which day should I look at?"
                    + "\nFor example: " + example);
        }
        return TaskDate.parseDay(dayText);
    }

    /**
     * Returns how many tasks {@link Command#NEXT} was asked for, having checked
     * that the user named a number and that it is a number of tasks worth showing.
     *
     * <p>Zero and negative numbers are refused rather than quietly showing nothing,
     * since a user who typed one has misunderstood the command and would learn
     * nothing from an empty answer.
     *
     * @param countText the count as the user typed it after {@code next}.
     * @return how many tasks to show, always one or more.
     * @throws BobException if nothing was typed after {@code next}, or what was
     *                      typed is not a whole number, or is less than one.
     */
    private static int requireCount(String countText) throws BobException {
        if (countText.isEmpty()) {
            throw new BobException("How many tasks should I show?"
                    + "\nFor example: " + NEXT_EXAMPLE);
        }
        int count;
        try {
            count = Integer.parseInt(countText);
        } catch (NumberFormatException e) {
            throw new BobException("\"" + countText + "\" isn't a number of tasks."
                    + "\nFor example: " + NEXT_EXAMPLE);
        }
        if (count < 1) {
            throw new BobException("I can show you one task or more, but not " + count + "."
                    + "\nFor example: " + NEXT_EXAMPLE);
        }
        return count;
    }

    /**
     * Prints the explanation carried by an error, one line per line of the message,
     * so that a message written as several lines is indented like any other output.
     */
    private static void showError(BobException error) {
        for (String line : error.getMessage().split("\n")) {
            printLine(line);
        }
    }

    /** Prints the sign-off message as one block. */
    private static void showFarewell() {
        openBlock();
        printLine("Bye. Hope to see you again soon!");
        closeBlock();
    }

    /** Starts a block of output with a divider. */
    private static void openBlock() {
        System.out.println(INDENT + DIVIDER);
    }

    /** Prints one indented line of chatbot output. */
    private static void printLine(String text) {
        System.out.println(INDENT + " " + text);
    }

    /** Ends a block with a divider, plus a blank line before whatever comes next. */
    private static void closeBlock() {
        System.out.println(INDENT + DIVIDER);
        System.out.println();
    }
}
