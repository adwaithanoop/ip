package bob;

import java.time.LocalDate;

/**
 * Turns the lines the user types into the things the chatbot acts on.
 *
 * <p>This was spread through {@link Bob}, where each command began by picking its
 * own arguments apart before doing anything with them. Adding a deadline, for
 * instance, was one method that found {@value #BY_KEYWORD}, complained about the
 * three ways the text around it could be wrong, read the date, and only then
 * built the task. Two jobs were being done in the one method: working out what
 * the user asked for, and carrying it out.
 *
 * <p>Separating them puts every rule about how a command is <em>written</em> in
 * one file. The markers, the examples quoted back when a command is malformed,
 * and the wording of every "that isn't a date" complaint now live together, so
 * changing what the chatbot accepts is a change here and nowhere else. What is
 * left in {@code Bob} is a set of methods that are handed a finished
 * {@link Task}, or a number, or a day, and get on with using it.
 *
 * <p>This class makes sense of <em>text</em>, and of nothing else. Whether the
 * number 7 names a task that exists is a fact about the task list rather than
 * about what the user typed, so that check stays in {@code Bob}. The dividing
 * line is worth stating because both kinds of complaint read alike to the user:
 * {@code mark seven} is refused here, {@code mark 7} with four tasks is refused
 * there.
 *
 * <p>Every method is {@code static}, unlike {@link Ui}, {@link Storage} and
 * {@link TaskList}, which are all made and kept. There is nothing for a parser to
 * remember between one line and the next — each line is understood on its own —
 * so an instance of this class would carry no state and be no more useful than
 * the class itself.
 */
public class Parser {

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

    /** How many tasks {@link #NEXT_EXAMPLE} asks for. */
    private static final int NEXT_EXAMPLE_COUNT = 3;

    /** Example of a well-formed {@link Command#NEXT} command, shown when one is malformed. */
    private static final String NEXT_EXAMPLE =
            Command.NEXT.getKeyword() + " " + NEXT_EXAMPLE_COUNT;

    /**
     * One line of the user's, made sense of: which command it is, and what was
     * written after the command word.
     *
     * <p>Both halves are wanted together and neither is much use alone, so they
     * come back as one value. A record is the shortest honest way to return a
     * pair: it is a plain data carrier, so the compiler writes its constructor,
     * accessors, {@code equals} and {@code toString}. {@link Storage.LoadResult}
     * is returned the same way, for the same reason.
     *
     * @param command   the command the line asks for.
     * @param arguments everything written after the command word, trimmed, which
     *                  is an empty string when nothing was.
     */
    public record ParsedCommand(Command command, String arguments) {
    }

    /**
     * Returns the command a line asks for, together with its arguments.
     *
     * <p>Which word is which command is {@link Command}'s own business; what this
     * method adds is the two ways a line can fail to name one at all, each with
     * its own explanation rather than a shared "bad command".
     *
     * @param line one whole line as the user typed it, with surrounding spaces removed.
     * @throws BobException if the line is empty, or does not begin with a command
     *                      the chatbot knows.
     */
    public static ParsedCommand parseCommand(String line) throws BobException {
        if (line.isEmpty()) {
            throw new BobException("You didn't type anything."
                    + "\nTell me about a task, or type " + Command.LIST.getKeyword()
                    + " to see the ones I already have.");
        }
        // orElseThrow unwraps the Optional when a command was recognized, and
        // throws the "I don't know what that means" error when none was.
        Command command = Command.of(line).orElseThrow(() -> unknownCommand(line));
        return new ParsedCommand(command, command.argumentsIn(line));
    }

    /**
     * Returns the {@link Todo} described by the text following {@link Command#TODO}.
     * Everything the user typed is the description.
     *
     * @param arguments the text following the command word.
     * @throws BobException if no description was given.
     */
    public static Todo parseTodo(String arguments) throws BobException {
        if (arguments.isEmpty()) {
            throw new BobException("A todo needs a description — tell me what to do."
                    + "\nFor example: " + Command.TODO.getKeyword() + " borrow book");
        }
        return new Todo(arguments);
    }

    /**
     * Returns the {@link Deadline} described by the text following
     * {@link Command#DEADLINE}, which is the description and the due date
     * separated by {@value #BY_KEYWORD}.
     *
     * <p>The three things that can be missing — the {@value #BY_KEYWORD} marker,
     * the description before it, the date after it — are reported separately, so
     * the user is told which one to add rather than just that the command is wrong.
     *
     * <p>The due date is handed to {@link TaskDate#parse} rather than stored as
     * typed, so a deadline is only built once its date has been understood. Text
     * that is not a date is refused there, with its own explanation.
     *
     * @param arguments the text following the command word.
     * @throws BobException if the marker, the description or the due date is
     *                      missing, or if the due date is not a date.
     */
    public static Deadline parseDeadline(String arguments) throws BobException {
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
        return new Deadline(description, TaskDate.parse(by));
    }

    /**
     * Returns the {@link Event} described by the text following
     * {@link Command#EVENT}, which is the description, then {@value #FROM_KEYWORD}
     * and the start, then {@value #TO_KEYWORD} and the end.
     *
     * <p>The end marker is looked for after the start marker, so that a
     * {@value #TO_KEYWORD} appearing earlier in the description is not
     * mistaken for the separator.
     *
     * <p>As with a deadline, both times are handed to {@link TaskDate#parse}, so
     * an event is only built once the chatbot has understood when it runs. Having
     * understood both, it can also check that they make sense together, which is
     * what {@link #requireEndNotBeforeStart} does.
     *
     * @param arguments the text following the command word.
     * @throws BobException if a marker, the description, the start or the end is
     *                      missing, if the start or the end is not a date, or if
     *                      the end comes before the start.
     */
    public static Event parseEvent(String arguments) throws BobException {
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
        String fromText = arguments.substring(fromIndex + FROM_KEYWORD.length(), toIndex).trim();
        String toText = arguments.substring(toIndex + TO_KEYWORD.length()).trim();
        if (description.isEmpty()) {
            throw new BobException("An event needs a description, written before "
                    + FROM_KEYWORD + "." + "\nFor example: " + EVENT_EXAMPLE);
        }
        if (fromText.isEmpty() || toText.isEmpty()) {
            throw new BobException("An event needs a time on both sides: one after "
                    + FROM_KEYWORD + " and one after " + TO_KEYWORD + "."
                    + "\nFor example: " + EVENT_EXAMPLE);
        }
        TaskDate from = TaskDate.parse(fromText);
        TaskDate to = TaskDate.parse(toText);
        requireEndNotBeforeStart(from, to);
        return new Event(description, from, to);
    }

    /**
     * Returns the task number the user typed, counting from 1 to match the
     * numbering shown by {@link Command#LIST}.
     *
     * <p>Only the two mistakes that are visible in the text itself are caught
     * here: nothing typed after the command word, and something typed that is not
     * a number. Whether a number that <em>is</em> a number names a task the user
     * has is left to the caller, which is the one holding the list.
     *
     * <p>The command is passed as a {@link Command} rather than as its keyword, so
     * a caller cannot name a command in the message that does not exist. The
     * keyword is read off it here, where the message is written.
     *
     * @param taskNumberText the task number as the user typed it.
     * @param command        the command to name in any error message.
     * @return the number typed, which may still be larger than the list.
     * @throws BobException if no task number was given, or what was given is not
     *                      a number.
     */
    public static int parseTaskNumber(String taskNumberText, Command command) throws BobException {
        String word = command.getKeyword();
        String listCommand = Command.LIST.getKeyword();
        if (taskNumberText.isEmpty()) {
            throw new BobException("Which task should I " + word + "?"
                    + "\nGive me its number from " + listCommand + ", for example: "
                    + word + " 2");
        }
        try {
            // The user is free to type anything after the command word, so a
            // number is asked for again rather than allowed to crash the chatbot.
            return Integer.parseInt(taskNumberText);
        } catch (NumberFormatException e) {
            throw new BobException("\"" + taskNumberText + "\" isn't a task number."
                    + "\nI need the number shown next to the task in " + listCommand
                    + ", for example: " + word + " 2");
        }
    }

    /**
     * Returns the day the user asked about, having checked that they named one and
     * that it is a day this chatbot can read.
     *
     * <p>{@link Command#ON}, {@link Command#BEFORE} and {@link Command#AFTER} all
     * take a day and reject the same two mistakes, so the checking is written here
     * once. Only the example differs, and it is built from the command that asked,
     * so each of the three is shown its own.
     *
     * @param dayText the day as the user typed it after the command word.
     * @param command the command that asked, used to write its example.
     * @return the day that text names.
     * @throws BobException if nothing was typed after the command word, or what was
     *                      typed is not a day.
     */
    public static LocalDate parseDay(String dayText, Command command) throws BobException {
        if (dayText.isEmpty()) {
            throw new BobException("Which day should I look at?"
                    + "\nFor example: " + dayExample(command));
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
    public static int parseCount(String countText) throws BobException {
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
     * Returns a well-formed use of a command that takes a day, for example
     * {@code before 2026-12-02}.
     *
     * <p>Built from the command word rather than written out once per command,
     * since the three commands that take a day are written alike. A constant
     * apiece would be three chances for one of them to be forgotten when a fourth
     * such command is added.
     */
    private static String dayExample(Command command) {
        return command.getKeyword() + " " + TaskDate.EXAMPLE_DATE;
    }

    /**
     * Checks that an event does not end before it starts.
     *
     * <p>Two dates that are each perfectly readable can still be an impossible
     * pair, and the pair is only worth checking once both have been understood —
     * which is why this is a step of its own after {@link TaskDate#parse} rather
     * than something the parsing could have caught.
     *
     * <p>Both dates are shown back in the message, so a user who typed them the
     * wrong way round can see which the chatbot read as the start and which as the
     * end. They are shown in the friendly form rather than as typed, since that is
     * the form that makes the ordering plain.
     *
     * <p>An event that starts and ends at the same moment is allowed: it is a point
     * in time rather than a contradiction, and refusing it would mean telling a user
     * who meant it that they may not say so.
     *
     * <p>This is checked here, while the command is being read, rather than in
     * {@link Event} itself, because here there is still a user to tell. An event
     * read from a hand-edited save file does not come through this method, and so
     * is loaded as written.
     *
     * @param from when the event starts.
     * @param to   when it ends.
     * @throws BobException if the end comes before the start.
     */
    private static void requireEndNotBeforeStart(TaskDate from, TaskDate to) throws BobException {
        if (to.compareTo(from) >= 0) {
            return;
        }
        throw new BobException("An event can't end before it starts."
                + "\nYou wrote " + FROM_KEYWORD + " " + from + " and " + TO_KEYWORD + " " + to
                + " — check whether they are the wrong way round.");
    }

    /**
     * Returns the error to throw for a line that is not one of the commands the
     * chatbot knows, listing the ones it does know so the user can pick one.
     */
    private static BobException unknownCommand(String line) {
        return new BobException("Sorry, I don't know what \"" + line + "\" means."
                + "\nTry one of: " + Command.allKeywords());
    }
}
