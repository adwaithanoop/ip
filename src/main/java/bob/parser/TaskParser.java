package bob.parser;

import bob.BobException;
import bob.command.CommandWord;
import bob.task.Deadline;
import bob.task.Event;
import bob.task.TaskDateTime;
import bob.task.Todo;

/**
 * Parses the text following {@link CommandWord#TODO}, {@link CommandWord#DEADLINE}
 * or {@link CommandWord#EVENT} into the task that command adds.
 *
 * <p>These are the commands with the most rules about how they are written: where
 * the markers sit, which parts may be missing, and whether the dates make sense
 * together. Keeping those rules here leaves {@link Parser} to decide only which
 * command a line is.
 *
 * <p>The markers are package-private so that {@link EditParser}, which changes the
 * same details of a task, recognizes them by the same text.
 */
class TaskParser {

    /** Marker separating a deadline's description from its due date. */
    static final String BY_KEYWORD = "/by";

    /** Marker separating an event's description from its start. */
    static final String FROM_KEYWORD = "/from";

    /** Marker separating an event's start from its end. */
    static final String TO_KEYWORD = "/to";

    /**
     * Example of a well-formed {@link CommandWord#DEADLINE} command, shown when one is malformed.
     *
     * <p>The date in it is taken from {@link TaskDateTime}, which is the class that
     * decides how a date may be written, so this example cannot drift out of step
     * with the dates the chatbot actually accepts.
     */
    private static final String DEADLINE_EXAMPLE =
            CommandWord.DEADLINE.getKeyword() + " return book " + BY_KEYWORD + " "
                    + TaskDateTime.EXAMPLE_DATE;

    /**
     * The end time shown in {@link #EVENT_EXAMPLE}, two hours after the start.
     *
     * <p>Written out here rather than taken from {@link TaskDateTime}, which offers a
     * single example date and not a pair of them. It is the one example date in
     * this package that is spelled out, so if the accepted form of a date ever
     * changes, this is the one line to change with it.
     */
    private static final String EVENT_EXAMPLE_END = "2026-12-02 2000";

    /** Example of a well-formed {@link CommandWord#EVENT} command, shown when one is malformed. */
    private static final String EVENT_EXAMPLE =
            CommandWord.EVENT.getKeyword() + " project meeting " + FROM_KEYWORD + " "
                    + TaskDateTime.EXAMPLE_DATE_TIME + " " + TO_KEYWORD + " " + EVENT_EXAMPLE_END;

    /**
     * Returns the {@link Todo} described by the text following {@link CommandWord#TODO}.
     * Everything the user typed is the description.
     *
     * @param arguments the text following the command word.
     * @throws BobException if no description was given.
     */
    static Todo parseTodo(String arguments) throws BobException {
        if (arguments.isEmpty()) {
            throw BobException.withExample("A todo needs a description — tell me what to do.",
                    CommandWord.TODO.getKeyword() + " borrow book");
        }
        return new Todo(arguments);
    }

    /**
     * Returns the {@link Deadline} described by the text following
     * {@link CommandWord#DEADLINE}, which is the description and the due date
     * separated by {@value #BY_KEYWORD}.
     *
     * <p>The three things that can be missing — the {@value #BY_KEYWORD} marker,
     * the description before it, the date after it — are reported separately, so
     * the user is told which one to add rather than just that the command is wrong.
     *
     * <p>The due date is handed to {@link TaskDateTime#parse} rather than stored as
     * typed, so a deadline is only built once its date has been understood. Text
     * that is not a date is refused there, with its own explanation.
     *
     * @param arguments the text following the command word.
     * @throws BobException if the marker, the description or the due date is
     *                      missing, or if the due date is not a date.
     */
    static Deadline parseDeadline(String arguments) throws BobException {
        int byIndex = arguments.indexOf(BY_KEYWORD);
        if (byIndex < 0) {
            throw BobException.withExample("A deadline needs a due date, written after "
                    + BY_KEYWORD + ".", DEADLINE_EXAMPLE);
        }
        String description = arguments.substring(0, byIndex).trim();
        String by = arguments.substring(byIndex + BY_KEYWORD.length()).trim();
        if (description.isEmpty()) {
            throw BobException.withExample("A deadline needs a description, written before "
                    + BY_KEYWORD + ".", DEADLINE_EXAMPLE);
        }
        if (by.isEmpty()) {
            throw BobException.withExample("You wrote " + BY_KEYWORD + " but not when it is due.",
                    DEADLINE_EXAMPLE);
        }
        return new Deadline(description, TaskDateTime.parse(by));
    }

    /**
     * Returns the {@link Event} described by the text following
     * {@link CommandWord#EVENT}, which is the description, then {@value #FROM_KEYWORD}
     * and the start, then {@value #TO_KEYWORD} and the end.
     *
     * <p>The end marker is looked for after the start marker, so that a
     * {@value #TO_KEYWORD} appearing earlier in the description is not
     * mistaken for the separator.
     *
     * <p>As with a deadline, both times are handed to {@link TaskDateTime#parse}, so
     * an event is only built once the chatbot has understood when it runs. Having
     * understood both, it can also check that they make sense together, which is
     * what {@link #requireEndNotBeforeStart} does.
     *
     * @param arguments the text following the command word.
     * @throws BobException if a marker, the description, the start or the end is
     *                      missing, if the start or the end is not a date, or if
     *                      the end comes before the start.
     */
    static Event parseEvent(String arguments) throws BobException {
        int fromIndex = arguments.indexOf(FROM_KEYWORD);
        if (fromIndex < 0) {
            throw BobException.withExample("An event needs a start time, written after "
                    + FROM_KEYWORD + ".", EVENT_EXAMPLE);
        }
        int toIndex = arguments.indexOf(TO_KEYWORD, fromIndex);
        if (toIndex < 0) {
            throw BobException.withExample("An event needs an end time, written after " + TO_KEYWORD
                    + " at the end.", EVENT_EXAMPLE);
        }
        String description = arguments.substring(0, fromIndex).trim();
        String fromText = arguments.substring(fromIndex + FROM_KEYWORD.length(), toIndex).trim();
        String toText = arguments.substring(toIndex + TO_KEYWORD.length()).trim();
        if (description.isEmpty()) {
            throw BobException.withExample("An event needs a description, written before "
                    + FROM_KEYWORD + ".", EVENT_EXAMPLE);
        }
        if (fromText.isEmpty() || toText.isEmpty()) {
            throw BobException.withExample("An event needs a time on both sides: one after "
                    + FROM_KEYWORD + " and one after " + TO_KEYWORD + ".", EVENT_EXAMPLE);
        }
        TaskDateTime from = TaskDateTime.parse(fromText);
        TaskDateTime to = TaskDateTime.parse(toText);
        requireEndNotBeforeStart(from, to);
        return new Event(description, from, to);
    }

    /**
     * Checks that an event does not end before it starts.
     *
     * <p>Two dates that are each perfectly readable can still be an impossible
     * pair, and the pair is only worth checking once both have been understood —
     * which is why this is a step of its own after {@link TaskDateTime#parse}.
     *
     * <p>Both dates are shown back in the message, in the friendly form rather than
     * as typed, so a user who typed them the wrong way round can see which the
     * chatbot read as the start and which as the end.
     *
     * <p>An event that starts and ends at the same moment is allowed: it is a point
     * in time rather than a contradiction.
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
    private static void requireEndNotBeforeStart(TaskDateTime from, TaskDateTime to) throws BobException {
        if (to.compareTo(from) >= 0) {
            return;
        }
        throw new BobException("An event can't end before it starts."
                + "\nYou wrote " + FROM_KEYWORD + " " + from + " and " + TO_KEYWORD + " " + to
                + " — check whether they are the wrong way round.");
    }
}
