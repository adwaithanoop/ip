import java.util.Scanner;

/**
 * A chatbot that greets the user, remembers the tasks the user types,
 * lists them back on request, marks them as done, and exits when the
 * user types {@code bye}.
 *
 * <p>Tasks come in three kinds — {@link Todo}, {@link Deadline} and
 * {@link Event} — each added with its own command word.
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

    /** Leading whitespace that sets the chatbot's output apart from the user's input. */
    private static final String INDENT = "    ";

    /**
     * Horizontal rule separating blocks of output.
     * Built with {@code repeat} so the width is stated as a number
     * rather than as a row of underscores that has to be counted by eye.
     */
    private static final String DIVIDER = "_".repeat(60);

    /** Maximum number of tasks the chatbot can remember, as allowed by the requirements. */
    private static final int MAX_TASKS = 100;

    /**
     * Tasks entered so far, stored in insertion order in slots {@code 0..taskCount - 1}.
     * A fixed-size array is enough here because the requirements cap the number of
     * tasks; a growable {@code ArrayList} would be the usual choice without that cap.
     *
     * <p>Each slot holds a {@link Task}. Because {@link Todo}, {@link Deadline}
     * and {@link Event} are all subclasses of {@link Task}, one array of
     * {@code Task} can hold any mixture of the three, and the code that lists or
     * marks them needs to know only that each is a task.
     */
    private static final Task[] tasks = new Task[MAX_TASKS];

    /** Number of slots of {@link #tasks} currently in use. */
    private static int taskCount = 0;

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
     */
    private static void handleCommandsUntilExit() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String command = scanner.nextLine();
                if (command.equals(EXIT_COMMAND)) {
                    return;
                }
                openBlock();
                handleCommand(command);
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
     * @param command one whole line as the user typed it
     */
    private static void handleCommand(String command) {
        if (command.equals(LIST_COMMAND)) {
            showTasks();
        } else if (command.startsWith(MARK_COMMAND + " ")) {
            setTaskDone(argumentsOf(command, MARK_COMMAND), true);
        } else if (command.startsWith(UNMARK_COMMAND + " ")) {
            setTaskDone(argumentsOf(command, UNMARK_COMMAND), false);
        } else if (isCommand(command, TODO_COMMAND)) {
            addTodo(argumentsOf(command, TODO_COMMAND));
        } else if (isCommand(command, DEADLINE_COMMAND)) {
            addDeadline(argumentsOf(command, DEADLINE_COMMAND));
        } else if (isCommand(command, EVENT_COMMAND)) {
            addEvent(argumentsOf(command, EVENT_COMMAND));
        } else {
            showUnknownCommand(command);
        }
    }

    /**
     * Returns whether {@code command} starts with the word {@code commandWord},
     * either on its own or followed by arguments.
     *
     * <p>Checking for the whole word rather than using {@code startsWith} alone
     * keeps a task such as {@code todolist} from being mistaken for the
     * {@value #TODO_COMMAND} command.
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
     */
    private static void addTodo(String arguments) {
        if (arguments.isEmpty()) {
            printLine("Please tell me what the todo is, for example: "
                    + TODO_COMMAND + " borrow book");
            return;
        }
        addTask(new Todo(arguments));
    }

    /**
     * Adds a {@link Deadline} from the text following {@value #DEADLINE_COMMAND},
     * which is the description and the due date separated by {@value #BY_KEYWORD}.
     */
    private static void addDeadline(String arguments) {
        int byIndex = arguments.indexOf(BY_KEYWORD);
        if (byIndex < 0) {
            printLine("Please say when it is due, for example: "
                    + DEADLINE_COMMAND + " return book " + BY_KEYWORD + " Sunday");
            return;
        }
        String description = arguments.substring(0, byIndex).trim();
        String by = arguments.substring(byIndex + BY_KEYWORD.length()).trim();
        if (description.isEmpty() || by.isEmpty()) {
            printLine("Please give both a description and a due date, for example: "
                    + DEADLINE_COMMAND + " return book " + BY_KEYWORD + " Sunday");
            return;
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
     */
    private static void addEvent(String arguments) {
        int fromIndex = arguments.indexOf(FROM_KEYWORD);
        int toIndex = fromIndex < 0 ? -1 : arguments.indexOf(TO_KEYWORD, fromIndex);
        if (fromIndex < 0 || toIndex < 0) {
            printLine("Please say when it starts and ends, for example: "
                    + EVENT_COMMAND + " project meeting "
                    + FROM_KEYWORD + " Mon 2pm " + TO_KEYWORD + " 4pm");
            return;
        }
        String description = arguments.substring(0, fromIndex).trim();
        String from = arguments.substring(fromIndex + FROM_KEYWORD.length(), toIndex).trim();
        String to = arguments.substring(toIndex + TO_KEYWORD.length()).trim();
        if (description.isEmpty() || from.isEmpty() || to.isEmpty()) {
            printLine("Please give a description, a start and an end, for example: "
                    + EVENT_COMMAND + " project meeting "
                    + FROM_KEYWORD + " Mon 2pm " + TO_KEYWORD + " 4pm");
            return;
        }
        addTask(new Event(description, from, to));
    }

    /**
     * Stores an already-built task and confirms it back to the user.
     * If the store is already full the task is refused rather than
     * letting the array write run off its end.
     *
     * <p>The parameter is a {@link Task}, so this one method stores todos,
     * deadlines and events alike; printing the task calls whichever
     * {@code toString} the actual object has.
     */
    private static void addTask(Task task) {
        if (taskCount == MAX_TASKS) {
            printLine("Sorry, I can only remember " + MAX_TASKS + " tasks.");
            return;
        }
        tasks[taskCount] = task;
        taskCount++;
        printLine("Got it. I've added this task:");
        printLine("  " + task);
        printLine("Now you have " + taskCount + " tasks in the list.");
    }

    /** Tells the user that a line was not one of the commands the chatbot knows. */
    private static void showUnknownCommand(String command) {
        printLine("Sorry, I don't know what \"" + command + "\" means.");
        printLine("Try one of: " + TODO_COMMAND + ", " + DEADLINE_COMMAND + ", "
                + EVENT_COMMAND + ", " + LIST_COMMAND + ", " + MARK_COMMAND + ", "
                + UNMARK_COMMAND + ", " + EXIT_COMMAND);
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
     */
    private static void setTaskDone(String taskNumberText, boolean done) {
        String command = done ? MARK_COMMAND : UNMARK_COMMAND;
        int taskNumber;
        try {
            taskNumber = Integer.parseInt(taskNumberText);
        } catch (NumberFormatException e) {
            // Reported rather than allowed to crash the chatbot, since the user
            // is free to type anything after the command word.
            printLine("Please tell me which task to " + command + ", for example: "
                    + command + " 2");
            return;
        }
        if (taskNumber < 1 || taskNumber > taskCount) {
            printLine("I don't have a task numbered " + taskNumber + ".");
            return;
        }
        Task task = tasks[taskNumber - 1];
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

    /** Prints the stored tasks as a numbered list, counting from 1 for readability. */
    private static void showTasks() {
        if (taskCount == 0) {
            printLine("You haven't told me about any tasks yet.");
            return;
        }
        printLine("Here are the tasks in your list:");
        for (int i = 0; i < taskCount; i++) {
            printLine((i + 1) + "." + tasks[i]);
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
