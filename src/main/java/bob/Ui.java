package bob;

import java.util.List;
import java.util.Scanner;

/**
 * Everything the chatbot says to the user and everything it hears back.
 *
 * <p>This was part of {@link Bob}, mixed in with the code that decides what each
 * command does. Two quite different questions were being answered in the one
 * class: <em>what</em> to tell the user, and <em>how</em> a line of chatbot
 * output looks on screen. This class now answers the second one alone.
 *
 * <p>What that buys is that the shape of the conversation is stated in one
 * place. The indent, the horizontal rules framing each block, the banner and the
 * greeting all live here, so changing how the chatbot looks is a change to this
 * file and to no other. {@link Bob} is left saying what it wants said, in lines
 * of plain text, and never touches {@code System.out} itself.
 *
 * <p>Reading the user's input belongs here for the same reason: it is the other
 * half of the same conversation, and keeping it here means no other class has to
 * know that the input happens to arrive on {@code System.in} through a
 * {@link Scanner}.
 *
 * <p>Output is built up as a <em>block</em>: {@link #openBlock()}, then one
 * {@link #showLine} per line, then {@link #closeBlock()}. The block is what
 * separates one answer from the next on screen, and letting the caller print its
 * lines one at a time is what lets an answer be as long as it needs to be —
 * a listing of every task, say — without this class having to know in advance
 * how many lines that will take.
 */
public class Ui {

    /** Name the chatbot introduces itself with. */
    private static final String NAME = "Bob";

    /** Leading whitespace that sets the chatbot's output apart from the user's input. */
    private static final String INDENT = "    ";

    /**
     * Horizontal rule separating blocks of output.
     * Built with {@code repeat} so the width is stated as a number
     * rather than as a row of underscores that has to be counted by eye.
     */
    private static final String DIVIDER = "_".repeat(60);

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
     * Where the user's typing is read from.
     *
     * <p>Made once and kept, rather than a new one per line: a {@link Scanner}
     * reads ahead into a buffer of its own, so a second one made partway through
     * the conversation could find the text it wants already taken by the first.
     */
    private final Scanner scanner = new Scanner(System.in);

    /** Prints the banner and welcome message as one block. */
    public void showGreeting() {
        openBlock();
        for (String bannerLine : BANNER) {
            showLine(bannerLine);
        }
        showLine("Hello! I'm " + NAME + ".");
        showLine("What can I do for you?");
        closeBlock();
    }

    /** Prints the sign-off message as one block. */
    public void showFarewell() {
        openBlock();
        showLine("Bye. Hope to see you again soon!");
        closeBlock();
    }

    /**
     * Returns whether the user has typed another line for the chatbot to read.
     *
     * <p>This is asked rather than assumed, so the chatbot also ends cleanly when
     * the input runs out — for example when it is piped in from a file that has
     * no {@code bye} line on the end.
     */
    public boolean hasNextCommand() {
        return scanner.hasNextLine();
    }

    /**
     * Returns the next line the user typed, with surrounding spaces removed.
     *
     * <p>Call only when {@link #hasNextCommand()} has just said there is one.
     *
     * @return one whole line as the user typed it, trimmed.
     */
    public String readCommand() {
        return scanner.nextLine().trim();
    }

    /**
     * Prints an explanation of something that went wrong, one line per line of
     * the message, so that a message written as several lines is indented like
     * any other output.
     *
     * <p>Printed inside whatever block is already open, so an error reads as the
     * answer to the command that caused it rather than as an interruption.
     *
     * @param message the explanation to show, which may span several lines.
     */
    public void showError(String message) {
        for (String line : message.split("\n")) {
            showLine(line);
        }
    }

    /** Starts a block of output with a divider. */
    public void openBlock() {
        System.out.println(INDENT + DIVIDER);
    }

    /**
     * Prints one indented line of chatbot output.
     *
     * @param text what the line should say, without any indent of its own.
     */
    public void showLine(String text) {
        System.out.println(INDENT + " " + text);
    }

    /** Ends a block with a divider, plus a blank line before whatever comes next. */
    public void closeBlock() {
        System.out.println(INDENT + DIVIDER);
        System.out.println();
    }

    /**
     * Stops reading the user's input, releasing the {@link Scanner} behind it.
     *
     * <p>Called once, when the conversation is over. Only reading stops: the
     * farewell is printed after this, and printing is unaffected.
     *
     * <p>A more advanced alternative is to make this class {@code AutoCloseable}
     * and have the caller wrap it in a try-with-resources, which would close it
     * even if the chatbot stopped part-way with an error. That is not worth its
     * keep here, where the only resource is the standard input of a program that
     * is about to end anyway.
     */
    public void close() {
        scanner.close();
    }
}
