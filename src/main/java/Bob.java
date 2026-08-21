import java.util.Scanner;

/**
 * A chatbot that greets the user, remembers the text the user types,
 * lists it back on request, and exits when the user types {@code bye}.
 */
public class Bob {

    /** Name the chatbot introduces itself with. */
    private static final String NAME = "Bob";

    /** Command that ends the conversation. */
    private static final String EXIT_COMMAND = "bye";

    /** Command that prints everything stored so far. */
    private static final String LIST_COMMAND = "list";

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
     */
    private static final String[] tasks = new String[MAX_TASKS];

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
     * <p>{@value #LIST_COMMAND} prints the stored tasks; any other line is
     * stored as a new task.
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
                if (command.equals(LIST_COMMAND)) {
                    showTasks();
                } else {
                    addTask(command);
                }
                closeBlock();
            }
        }
    }

    /**
     * Stores {@code task} and confirms it back to the user.
     * If the store is already full the task is refused rather than
     * letting the array write run off its end.
     */
    private static void addTask(String task) {
        if (taskCount == MAX_TASKS) {
            printLine("Sorry, I can only remember " + MAX_TASKS + " tasks.");
            return;
        }
        tasks[taskCount] = task;
        taskCount++;
        printLine("added: " + task);
    }

    /** Prints the stored tasks as a numbered list, counting from 1 for readability. */
    private static void showTasks() {
        if (taskCount == 0) {
            printLine("You haven't told me about any tasks yet.");
            return;
        }
        for (int i = 0; i < taskCount; i++) {
            printLine((i + 1) + ". " + tasks[i]);
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
