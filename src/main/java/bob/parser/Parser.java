package bob.parser;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import bob.BobException;
import bob.command.AddCommand;
import bob.command.AfterCommand;
import bob.command.BeforeCommand;
import bob.command.Command;
import bob.command.CommandWord;
import bob.command.DeleteCommand;
import bob.command.EditCommand;
import bob.command.ExitCommand;
import bob.command.FindCommand;
import bob.command.ListCommand;
import bob.command.MarkCommand;
import bob.command.NextCommand;
import bob.command.OnCommand;
import bob.task.Deadline;
import bob.task.Event;
import bob.task.TaskDateTime;
import bob.task.TaskEdit;
import bob.task.Todo;

/**
 * Turns the lines the user types into the things the chatbot acts on.
 *
 * <p>This was spread through {@link bob.Bob Bob}, where each command began by picking its
 * own arguments apart before doing anything with them. Adding a deadline, for
 * instance, was one method that found {@value #BY_KEYWORD}, complained about the
 * three ways the text around it could be wrong, read the date, and only then
 * built the task. Two jobs were being done in the one method: working out what
 * the user asked for, and carrying it out.
 *
 * <p>Separating them puts every rule about how a command is <em>written</em> in
 * one file. The markers, the examples quoted back when a command is malformed,
 * and the wording of every "that isn't a date" complaint now live together, so
 * changing what the chatbot accepts is a change here and nowhere else. The other
 * job, carrying the command out, belongs to the {@link bob.command.Command Command} classes, each
 * handed what it needs, such as a finished {@link bob.task.Task Task}, a number or a day.
 *
 * <p>This class makes sense of <em>text</em>, and of nothing else. Whether the
 * number 7 names a task that exists is a fact about the task list rather than
 * about what the user typed, so that check belongs to
 * {@link bob.command.TaskNumberCommand TaskNumberCommand}, which is given the list. The dividing
 * line is worth stating because both kinds of complaint read alike to the user:
 * {@code mark seven} is refused here, {@code mark 7} with four tasks is refused
 * there.
 *
 * <p>Every method is {@code static}, unlike {@link bob.ui.Ui Ui}, {@link bob.storage.Storage Storage} and
 * {@link bob.task.TaskList TaskList}, which are all made and kept. There is nothing for a parser to
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

    /** Marker introducing a task's new description in an {@link CommandWord#EDIT} command. */
    private static final String DESC_KEYWORD = "/desc";

    /**
     * Every marker an {@link CommandWord#EDIT} command may use, in the order in which
     * complaints about them are made.
     *
     * <p>This is not an order the user has to type them in. Each marker says which
     * detail follows it, so the markers of an edit may come in any order.
     */
    private static final List<String> EDIT_KEYWORDS =
            List.of(DESC_KEYWORD, BY_KEYWORD, FROM_KEYWORD, TO_KEYWORD);

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
     * this class that is spelled out, so if the accepted form of a date ever
     * changes, this is the one line to change with it.
     */
    private static final String EVENT_EXAMPLE_END = "2026-12-02 2000";

    /** Example of a well-formed {@link CommandWord#EVENT} command, shown when one is malformed. */
    private static final String EVENT_EXAMPLE =
            CommandWord.EVENT.getKeyword() + " project meeting " + FROM_KEYWORD + " "
                    + TaskDateTime.EXAMPLE_DATE_TIME + " " + TO_KEYWORD + " " + EVENT_EXAMPLE_END;

    /** How many tasks {@link #NEXT_EXAMPLE} asks for. */
    private static final int NEXT_EXAMPLE_COUNT = 3;

    /** Example of a well-formed {@link CommandWord#NEXT} command, shown when one is malformed. */
    private static final String NEXT_EXAMPLE =
            CommandWord.NEXT.getKeyword() + " " + NEXT_EXAMPLE_COUNT;

    /** Example of a well-formed {@link CommandWord#FIND} command, shown when one is malformed. */
    private static final String FIND_EXAMPLE = CommandWord.FIND.getKeyword() + " book";

    /**
     * Returns the command one line of the user's asks for, ready to be run.
     *
     * <p>This is the only way in. Everything below it is private, because the
     * halfway houses — a date read out of some text, a task number, a description
     * — are steps on the way to a command and not answers anybody outside wants.
     *
     * <p>Which word is which command is {@link CommandWord}'s own business; what
     * this method adds is the two ways a line can fail to name one at all, each
     * with its own explanation rather than a shared "bad command".
     *
     * <p>The {@code switch} below is the one place left that names every command
     * in a list. That much is unavoidable: something has to turn a word into an
     * object, and doing it here means it happens once. What has gone is the other
     * kind of list — the one that said what each command <em>does</em> — because
     * that is now in the commands themselves.
     *
     * @param line one whole line as the user typed it, with surrounding spaces removed.
     * @return the command that line asks for.
     * @throws BobException if the line is empty, does not begin with a command the
     *                      chatbot knows, or is one it knows but cannot carry out
     *                      as written.
     */
    public static Command parse(String line) throws BobException {
        if (line.isEmpty()) {
            throw new BobException("You didn't type anything."
                    + "\nTell me about a task, or type " + CommandWord.LIST.getKeyword()
                    + " to see the ones I already have.");
        }
        // orElseThrow unwraps the Optional when a command was recognized, and
        // throws the "I don't know what that means" error when none was.
        CommandWord word = CommandWord.parse(line).orElseThrow(() -> createUnknownCommandError(line));
        String arguments = word.getArgumentsIn(line);
        return switch (word) {
            case TODO -> new AddCommand(parseTodo(arguments));
            case DEADLINE -> new AddCommand(parseDeadline(arguments));
            case EVENT -> new AddCommand(parseEvent(arguments));
            case LIST -> new ListCommand();
            case ON -> new OnCommand(parseDay(arguments, word));
            case BEFORE -> new BeforeCommand(parseDay(arguments, word));
            case AFTER -> new AfterCommand(parseDay(arguments, word));
            case NEXT -> new NextCommand(parseCount(arguments));
            case FIND -> new FindCommand(parseKeyword(arguments));
            case MARK -> new MarkCommand(parseTaskNumber(arguments, word), true);
            case UNMARK -> new MarkCommand(parseTaskNumber(arguments, word), false);
            case DELETE -> new DeleteCommand(parseTaskNumber(arguments, word));
            case EDIT -> parseEdit(arguments);
            case BYE -> new ExitCommand();
        };
    }

    /**
     * Returns the {@link Todo} described by the text following {@link CommandWord#TODO}.
     * Everything the user typed is the description.
     *
     * @param arguments the text following the command word.
     * @throws BobException if no description was given.
     */
    private static Todo parseTodo(String arguments) throws BobException {
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
    private static Deadline parseDeadline(String arguments) throws BobException {
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
    private static Event parseEvent(String arguments) throws BobException {
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
     * Returns the {@link EditCommand} described by the text following
     * {@link CommandWord#EDIT}: a task number, then one or more markers, each
     * followed by the new value of one detail of that task.
     *
     * <p>The markers may come in any order. Each value runs from its marker to
     * whichever marker comes next in the text, so the order carries no meaning that
     * a rule about it would protect. The price, shared with {@link #parseDeadline}
     * and {@link #parseEvent}, is that a new description cannot itself contain a
     * marker.
     *
     * <p>Only what can be judged from the text is checked here. Whether the task has
     * the dates being changed, and whether an event would still end after it starts,
     * depend on the task, and are left to {@link bob.task.Task#withEdit Task.withEdit}.
     *
     * @param arguments the text following the command word.
     * @throws BobException if the task number is missing or is not a number, if the
     *                      text after it does not begin with a marker, if a marker
     *                      has nothing after it, or if a new date is not a date.
     */
    private static EditCommand parseEdit(String arguments) throws BobException {
        // The task number is everything up to the first space; the changes are the rest.
        String[] parts = arguments.split("\\s+", 2);
        int taskNumber = parseTaskNumber(parts[0], CommandWord.EDIT);
        String changes = (parts.length == 2) ? parts[1] : "";
        if (EDIT_KEYWORDS.stream().noneMatch(changes::startsWith)) {
            throw BobException.withExample("What should I change about task " + taskNumber + "?"
                    + "\nUse " + DESC_KEYWORD + ", " + BY_KEYWORD + ", " + FROM_KEYWORD
                    + " or " + TO_KEYWORD + ".",
                    CommandWord.EDIT.getKeyword() + " " + taskNumber + " " + DESC_KEYWORD + " read book");
        }

        // Every marker is checked for a value before any date is read, so a missing
        // value is reported ahead of a date that could not be read.
        for (String keyword : EDIT_KEYWORDS) {
            if (findEditedText(changes, keyword).filter(String::isEmpty).isPresent()) {
                throw new BobException("You wrote " + keyword + " but nothing after it.");
            }
        }
        return new EditCommand(taskNumber, new TaskEdit(
                findEditedText(changes, DESC_KEYWORD).orElse(null),
                parseEditedDate(changes, BY_KEYWORD),
                parseEditedDate(changes, FROM_KEYWORD),
                parseEditedDate(changes, TO_KEYWORD)));
    }

    /**
     * Returns the text following {@code keyword} in the changes of an edit, up to the
     * next marker or the end, with surrounding spaces removed — or an empty
     * {@link Optional} if that marker was not used.
     *
     * <p>The value ends at whichever marker comes next in the text, rather than at the
     * next one in {@link #EDIT_KEYWORDS}, which is what lets the markers come in any
     * order.
     *
     * @param changes the text following the task number.
     * @param keyword the marker whose value to find.
     */
    private static Optional<String> findEditedText(String changes, String keyword) {
        int keywordIndex = changes.indexOf(keyword);
        if (keywordIndex < 0) {
            return Optional.empty();
        }
        int valueEnd = EDIT_KEYWORDS.stream()
                .mapToInt(changes::indexOf)
                .filter(index -> index > keywordIndex)
                .min()
                .orElse(changes.length());
        return Optional.of(changes.substring(keywordIndex + keyword.length(), valueEnd).trim());
    }

    /**
     * Returns the new date following {@code keyword} in the changes of an edit, or
     * {@code null} if that marker was not used and the date is to be kept.
     *
     * <p>{@code null} rather than an empty {@link Optional}, because the answer goes
     * straight into a {@link TaskEdit}, where a detail left unchanged is {@code null}.
     *
     * @param changes the text following the task number.
     * @param keyword the marker whose date to read.
     * @throws BobException if the text following the marker is not a date.
     */
    private static TaskDateTime parseEditedDate(String changes, String keyword) throws BobException {
        Optional<String> dateText = findEditedText(changes, keyword);
        if (dateText.isEmpty()) {
            return null;
        }
        return TaskDateTime.parse(dateText.get());
    }

    /**
     * Returns the task number the user typed, counting from 1 to match the
     * numbering shown by {@link CommandWord#LIST}.
     *
     * <p>Only the two mistakes that are visible in the text itself are caught
     * here: nothing typed after the command word, and something typed that is not
     * a number. Whether a number that <em>is</em> a number names a task the user
     * has is left to the caller, which is the one holding the list.
     *
     * <p>The command is passed as a {@link CommandWord} rather than as its keyword, so
     * a caller cannot name a command in the message that does not exist. The
     * keyword is read off it here, where the message is written.
     *
     * @param taskNumberText the task number as the user typed it.
     * @param command        the command to name in any error message.
     * @return the number typed, which may still be larger than the list.
     * @throws BobException if no task number was given, or what was given is not
     *                      a number.
     */
    private static int parseTaskNumber(String taskNumberText, CommandWord command) throws BobException {
        String word = command.getKeyword();
        String listCommand = CommandWord.LIST.getKeyword();
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
     * <p>{@link CommandWord#ON}, {@link CommandWord#BEFORE} and {@link CommandWord#AFTER} all
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
    private static LocalDate parseDay(String dayText, CommandWord command) throws BobException {
        if (dayText.isEmpty()) {
            throw BobException.withExample("Which day should I look at?", getDayExample(command));
        }
        return TaskDateTime.parseDay(dayText);
    }

    /**
     * Returns how many tasks {@link CommandWord#NEXT} was asked for, having checked
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
    private static int parseCount(String countText) throws BobException {
        if (countText.isEmpty()) {
            throw BobException.withExample("How many tasks should I show?", NEXT_EXAMPLE);
        }
        int count;
        try {
            count = Integer.parseInt(countText);
        } catch (NumberFormatException e) {
            throw BobException.withExample("\"" + countText + "\" isn't a number of tasks.",
                    NEXT_EXAMPLE);
        }
        if (count < 1) {
            throw BobException.withExample("I can show you one task or more, but not " + count + ".",
                    NEXT_EXAMPLE);
        }
        return count;
    }

    /**
     * Returns the text {@link CommandWord#FIND} was asked to search for, having
     * checked that the user typed something to search for at all.
     *
     * <p>Everything after the command word is the keyword, spaces and all, so a
     * user may search for a phrase such as {@code find sports club}. Nothing else
     * is checked: any text at all is a search someone might mean, and one that
     * matches nothing is answered by the command rather than refused here.
     *
     * @param keywordText the text as the user typed it after {@code find}.
     * @return the text to look for in each description.
     * @throws BobException if nothing was typed after {@code find}.
     */
    private static String parseKeyword(String keywordText) throws BobException {
        if (keywordText.isEmpty()) {
            throw new BobException("What should I look for?"
                    + "\nGive me a word from the task, for example: " + FIND_EXAMPLE);
        }
        return keywordText;
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
    private static String getDayExample(CommandWord command) {
        return command.getKeyword() + " " + TaskDateTime.EXAMPLE_DATE;
    }

    /**
     * Checks that an event does not end before it starts.
     *
     * <p>Two dates that are each perfectly readable can still be an impossible
     * pair, and the pair is only worth checking once both have been understood —
     * which is why this is a step of its own after {@link TaskDateTime#parse} rather
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
    private static void requireEndNotBeforeStart(TaskDateTime from, TaskDateTime to) throws BobException {
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
    private static BobException createUnknownCommandError(String line) {
        return new BobException("Sorry, I don't know what \"" + line + "\" means."
                + "\nTry one of: " + CommandWord.getAllKeywords());
    }
}
