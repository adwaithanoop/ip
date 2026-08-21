/**
 * A chatbot that greets the user and then exits.
 *
 * <p>This is the starting point of the project: it does not read any input yet.
 */
public class Bob {

    /** Name the chatbot introduces itself with. */
    private static final String NAME = "Bob";

    /**
     * Horizontal rule separating blocks of output.
     * Built with {@code repeat} so the width is stated as a number
     * rather than as a row of underscores that has to be counted by eye.
     */
    private static final String DIVIDER = "_".repeat(60);

    /**
     * ASCII-art banner spelling out the chatbot's name, in the figlet "standard" font.
     * Has no trailing newline, because {@code println} supplies one.
     */
    private static final String BANNER = " ____        _     \n"
            + "| __ )  ___ | |__  \n"
            + "|  _ \\ / _ \\| '_ \\ \n"
            + "| |_) | (_) | |_) |\n"
            + "|____/ \\___/|_.__/ ";

    public static void main(String[] args) {
        showGreeting();
        showFarewell();
    }

    /** Prints the banner and welcome message, enclosed by dividers. */
    private static void showGreeting() {
        System.out.println(DIVIDER);
        System.out.println(BANNER);
        System.out.println("Hello! I'm " + NAME + ".");
        System.out.println("What can I do for you?");
        System.out.println(DIVIDER);
    }

    /** Prints the sign-off message, followed by a closing divider. */
    private static void showFarewell() {
        System.out.println("Bye. Hope to see you again soon!");
        System.out.println(DIVIDER);
    }
}
