import java.util.ArrayList;
import java.util.Scanner;

/**
 * A chatbot that greets the user, remembers the tasks the user types,
 * lists them back on request, marks them as done, removes the ones the user
 * no longer wants, and exits when the user types {@code bye}.
 *
 * <p>Tasks come in three kinds — {@link Todo}, {@link Deadline} and
 * {@link Event} — each added with its own command word.
 *
 * <p>Anything the user types that cannot be carried out — an unknown command,
 * a missing description, a task number that does not exist — is reported by
 * throwing a {@link BobException} carrying the explanation. The command loop
 * catches it and prints it, so a mistyped command never ends the conversation.
 */
public class Bob {

    /** Name the chatbot introduces itself with. */
    private static final String NAME = "Bob";

    /** Command that ends the conversation. */
    private static final String EXIT_COMMAND = "bye";

    /** Command that prints everything stored so far. */
    private static final String LIST_COMMAND = "list";

    /** Command that marks a task as done; used as {@code mark <task number>}. */
    private static final String MARK_COMMAND = "mark";

    /** Command that marks a task as not done again; used as {@code unmark <task number>}. */
    private static final String UNMARK_COMMAND = "unmark";

    /** Command that removes a task from the list; used as {@code delete <task number>}. */
    private static final String DELETE_COMMAND = "delete";

    /** Command that adds a task with no date attached; used as {@code todo <description>}. */
    private static final String TODO_COMMAND = "todo";

    /** Command that adds a task with a due date; used as {@code deadline <description> /by <when>}. */
    private static final String DEADLINE_COMMAND = "deadline";

    /** Command that adds a task with a start and end; used as {@code event <description> /from <when> /to <when>}. */
    private static final String EVENT_COMMAND = "event";

    /** Marker separating a deadline's description from its due date. */
    private static final String BY_KEYWORD = "/by";

    /** Marker separating an event's description from its start. */
    private static final String FROM_KEYWORD = "/from";

    /** Marker separating an event's start from its end. */
    private static final String TO_KEYWORD = "/to";

    /** Example of a well-formed {@value #DEADLINE_COMMAND} command, shown when one is malformed. */
    private static final String DEADLINE_EXAMPLE =
            DEADLINE_COMMAND + " return book " + BY_KEYWORD + " Sunday";

    /** Example of a well-formed {@value #EVENT_COMMAND} command, shown when one is malformed. */
    private static final String EVENT_EXAMPLE =
            EVENT_COMMAND + " project meeting " + FROM_KEYWORD + " Mon 2pm " + TO_KEYWORD + " 4pm";

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
     * ASCII-art banner spelling out the chatbot's name, in the figlet "standard" font.
     * Stored as one line per array element so each line can be indented
     * the same way as every other line the chatbot prints.
     */
    private static final String[] BANNER = {
            " ____        _     ",
            "| __ )  ___ | |__  ",
            "|  _ \\ / _ \\| '_ \\ ",
            "| |_) | (_) | |_) |",
            "|____/ \\___/|_.__/ ",
    };

    public static void main(String[] args) {
        showGreeting();
        handleCommandsUntilExit();
        showFarewell();
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
     * stopping when the user types {@value #EXIT_COMMAND}.
     *
     * <p>The loop is guarded by {@code hasNextLine} rather than looping forever,
     * so the program also ends cleanly if the input runs out (for example when
     * input is piped in from a file that has no {@value #EXIT_COMMAND} line).
     *
     * <p>This is also the one place where a {@link BobException} is caught. Any
     * command that cannot be carried out reports itself by throwing, the message
     * is printed here, and the loop simply goes on to read the next line — so a
     * mistake costs the user a line, not the whole conversation.
     */
    private static void handleCommandsUntilExit() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String command = scanner.nextLine().trim();
                if (command.equals(EXIT_COMMAND)) {
                    return;
                }
                openBlock();
                try {
                    handleCommand(command);
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
     * @param command one whole line as the user typed it, with surrounding spaces removed
     * @throws BobException if the line is not a command the chatbot knows, or is
     *                      one it knows but cannot carry out as written
     */
    private static void handleCommand(String command) throws BobException {
        if (command.isEmpty()) {
            throw new BobException("You didn't type anything."
                    + "\nTell me about a task, or type " + LIST_COMMAND
                    + " to see the ones I already have.");
        } else if (command.equals(LIST_COMMAND)) {
            showTasks();
        } else if (isCommand(command, MARK_COMMAND)) {
            setTaskDone(argumentsOf(command, MARK_COMMAND), true);
        } else if (isCommand(command, UNMARK_COMMAND)) {
            setTaskDone(argumentsOf(command, UNMARK_COMMAND), false);
        } else if (isCommand(command, DELETE_COMMAND)) {
            deleteTask(argumentsOf(command, DELETE_COMMAND));
        } else if (isCommand(command, TODO_COMMAND)) {
            addTodo(argumentsOf(command, TODO_COMMAND));
        } else if (isCommand(command, DEADLINE_COMMAND)) {
            addDeadline(argumentsOf(command, DEADLINE_COMMAND));
        } else if (isCommand(command, EVENT_COMMAND)) {
            addEvent(argumentsOf(command, EVENT_COMMAND));
        } else {
            throw unknownCommand(command);
        }
    }

    /**
     * Returns whether {@code command} starts with the word {@code commandWord},
     * either on its own or followed by arguments.
     *
     * <p>Checking for the whole word rather than using {@code startsWith} alone
     * keeps a task such as {@code todolist} from being mistaken for the
     * {@value #TODO_COMMAND} command.
     *
     * <p>Matching the bare word too, and not only the word with arguments after
     * it, is what lets {@code mark} on its own be answered with "which task?"
     * rather than with "I don't know what that means": the chatbot recognises
     * the command and can then explain what is missing from it.
     */
    private static boolean isCommand(String command, String commandWord) {
        return command.equals(commandWord) || command.startsWith(commandWord + " ");
    }

    /**
     * Returns everything the user typed after the command word, with surrounding
     * spaces removed, or an empty string if they typed nothing after it.
     */
    private static String argumentsOf(String command, String commandWord) {
        if (command.length() <= commandWord.length()) {
            return "";
        }
        return command.substring(commandWord.length() + 1).trim();
    }

    /**
     * Adds a {@link Todo} from the text following {@value #TODO_COMMAND}.
     * Everything the user typed is the description.
     *
     * @throws BobException if no description was given
     */
    private static void addTodo(String arguments) throws BobException {
        if (arguments.isEmpty()) {
            throw new BobException("A todo needs a description — tell me what to do."
                    + "\nFor example: " + TODO_COMMAND + " borrow book");
        }
        addTask(new Todo(arguments));
    }

    /**
     * Adds a {@link Deadline} from the text following {@value #DEADLINE_COMMAND},
     * which is the description and the due date separated by {@value #BY_KEYWORD}.
     *
     * <p>The three things that can be missing — the {@value #BY_KEYWORD} marker,
     * the description before it, the date after it — are reported separately, so
     * the user is told which one to add rather than just that the command is wrong.
     *
     * @throws BobException if the marker, the description or the due date is missing
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
        addTask(new Deadline(description, by));
    }

    /**
     * Adds an {@link Event} from the text following {@value #EVENT_COMMAND},
     * which is the description, then {@value #FROM_KEYWORD} and the start,
     * then {@value #TO_KEYWORD} and the end.
     *
     * <p>The end marker is looked for after the start marker, so that a
     * {@value #TO_KEYWORD} appearing earlier in the description is not
     * mistaken for the separator.
     *
     * @throws BobException if a marker, the description, the start or the end is missing
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
        addTask(new Event(description, from, to));
    }

    /**
     * Stores an already-built task and confirms it back to the user.
     *
     * <p>The parameter is a {@link Task}, so this one method stores todos,
     * deadlines and events alike; printing the task calls whichever
     * {@code toString} the actual object has.
     */
    private static void addTask(Task task) {
        tasks.add(task);
        printLine("Got it. I've added this task:");
        printLine("  " + task);
        printLine("Now you have " + tasks.size() + " tasks in the list.");
    }

    /**
     * Removes the task the user named and shows it one last time, so the user can
     * see which task is gone rather than having to work it out from the numbering.
     *
     * <p>The tasks after it move up to fill the gap, so the numbers shown by
     * {@value #LIST_COMMAND} stay a run of 1, 2, 3 with nothing missing. That
     * renumbering is why the confirmation shows the task itself: after a deletion
     * the number the user typed refers to a different task than it did before.
     *
     * @param taskNumberText the task number as the user typed it, counting from 1
     * @throws BobException if the number is missing, is not a number, or names no task
     */
    private static void deleteTask(String taskNumberText) throws BobException {
        int taskIndex = requireTaskIndex(taskNumberText, DELETE_COMMAND);
        // remove returns the task it took out, so it can be shown without
        // having to be fetched separately beforehand.
        Task removed = tasks.remove(taskIndex);
        printLine("Noted. I've removed this task:");
        printLine("  " + removed);
        printLine("Now you have " + tasks.size() + " tasks in the list.");
    }

    /**
     * Returns the error to throw for a line that is not one of the commands the
     * chatbot knows, listing the ones it does know so the user can pick one.
     */
    private static BobException unknownCommand(String command) {
        return new BobException("Sorry, I don't know what \"" + command + "\" means."
                + "\nTry one of: " + TODO_COMMAND + ", " + DEADLINE_COMMAND + ", "
                + EVENT_COMMAND + ", " + LIST_COMMAND + ", " + MARK_COMMAND + ", "
                + UNMARK_COMMAND + ", " + DELETE_COMMAND + ", " + EXIT_COMMAND);
    }

    /**
     * Sets the done status of the task the user named and shows it back to them.
     * Both {@value #MARK_COMMAND} and {@value #UNMARK_COMMAND} share this method,
     * since they differ only in the status they set and the wording they report.
     *
     * @param taskNumberText the task number as the user typed it, counting from 1
     *                       to match the numbering shown by {@value #LIST_COMMAND}
     * @param done           {@code true} to mark the task as done,
     *                       {@code false} to mark it as not done yet
     * @throws BobException if no task number was given, if what was given is not a
     *                      number, or if no task has that number
     */
    private static void setTaskDone(String taskNumberText, boolean done) throws BobException {
        String command = done ? MARK_COMMAND : UNMARK_COMMAND;
        Task task = tasks.get(requireTaskIndex(taskNumberText, command));
        if (done) {
            task.markAsDone();
        } else {
            task.markAsNotDone();
        }
        printLine(done
                ? "Nice! I've marked this task as done:"
                : "OK, I've marked this task as not done yet:");
        printLine("  " + task);
    }

    /**
     * Returns the position in {@link #tasks} of the task the user named, having
     * first checked that they named one and that it exists.
     *
     * <p>{@value #MARK_COMMAND}, {@value #UNMARK_COMMAND} and
     * {@value #DELETE_COMMAND} all take a task number and all reject the same
     * three mistakes, so the checking lives here once instead of being repeated
     * in each of them. The command word is passed in only so that the messages
     * can name the command the user actually typed.
     *
     * @param taskNumberText the task number as the user typed it, counting from 1
     *                       to match the numbering shown by {@value #LIST_COMMAND}
     * @param command        the command word to name in any error message
     * @return the position of that task in {@link #tasks}, counting from 0
     * @throws BobException if no task number was given, if what was given is not a
     *                      number, or if no task has that number
     */
    private static int requireTaskIndex(String taskNumberText, String command) throws BobException {
        if (taskNumberText.isEmpty()) {
            throw new BobException("Which task should I " + command + "?"
                    + "\nGive me its number from " + LIST_COMMAND + ", for example: "
                    + command + " 2");
        }
        int taskNumber;
        try {
            // The user is free to type anything after the command word, so a
            // number is asked for again rather than allowed to crash the chatbot.
            taskNumber = Integer.parseInt(taskNumberText);
        } catch (NumberFormatException e) {
            throw new BobException("\"" + taskNumberText + "\" isn't a task number."
                    + "\nI need the number shown next to the task in " + LIST_COMMAND
                    + ", for example: " + command + " 2");
        }
        if (tasks.isEmpty()) {
            throw new BobException("There is nothing to " + command
                    + " yet — your list is empty.");
        }
        if (taskNumber < 1 || taskNumber > tasks.size()) {
            throw new BobException("I don't have a task numbered " + taskNumber + "."
                    + "\nYour list runs from 1 to " + tasks.size() + "; type " + LIST_COMMAND
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
            printLine((i + 1) + "." + tasks.get(i));
        }
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
