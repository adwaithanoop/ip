import java.util.Optional;
import java.util.StringJoiner;

/**
 * The words the chatbot understands at the start of a line, and the rules for
 * recognising each one.
 *
 * <p>These were eight separate {@code String} constants in {@link Bob}. An enum
 * suits them better because they are a fixed, known-in-advance set of values
 * that belong together: the compiler now knows the whole set, so a command can
 * be passed around as a {@code Command} rather than as a {@code String} that
 * might hold any text at all, and a typo like {@code "delet"} becomes a
 * compile error instead of a command that silently never matches.
 *
 * <p>Two things follow from the set being known in one place. {@link #of} finds
 * the command a line begins with by walking {@link #values()}, so recognising a
 * command is no longer a chain of {@code else if} branches that has to be
 * extended by hand. And {@link #allKeywords} builds the list of commands shown
 * to a user who typed something unrecognised, so that message can no longer
 * fall out of step with the commands that actually exist — which is exactly the
 * kind of mistake that is easy to make when adding a command.
 */
public enum Command {

    /** Adds a task with no date attached; used as {@code todo <description>}. */
    TODO("todo", true),

    /** Adds a task with a due date; used as {@code deadline <description> /by <when>}. */
    DEADLINE("deadline", true),

    /** Adds a task with a start and an end; used as {@code event <description> /from <when> /to <when>}. */
    EVENT("event", true),

    /** Prints everything stored so far; used on its own. */
    LIST("list", false),

    /** Marks a task as done; used as {@code mark <task number>}. */
    MARK("mark", true),

    /** Marks a task as not done again; used as {@code unmark <task number>}. */
    UNMARK("unmark", true),

    /** Removes a task from the list; used as {@code delete <task number>}. */
    DELETE("delete", true),

    /** Ends the conversation; used on its own. */
    BYE("bye", false);

    /** The word the user types to invoke this command. */
    private final String keyword;

    /**
     * Whether anything may follow the keyword on the same line.
     *
     * <p>This is what tells {@code list} and {@code bye}, which stand alone,
     * apart from the six that are followed by a description or a task number.
     * The rule was previously implicit — some commands were compared with
     * {@code equals} and the rest with a separate helper — which made it
     * something a reader had to notice rather than something the code states.
     */
    private final boolean takesArguments;

    /**
     * Creates a command. The constructor is private, as every enum constructor
     * is: the constants declared above are the only instances there will ever be.
     *
     * @param keyword        the word the user types
     * @param takesArguments whether text may follow that word
     */
    Command(String keyword, boolean takesArguments) {
        this.keyword = keyword;
        this.takesArguments = takesArguments;
    }

    /** Returns the word the user types to invoke this command, for example {@code delete}. */
    public String getKeyword() {
        return keyword;
    }

    /**
     * Returns whether {@code line} is this command.
     *
     * <p>A command that takes arguments matches its keyword either alone or
     * followed by a space and the arguments. Requiring that space is what keeps
     * {@code todolist} from being read as {@code todo} with the description
     * {@code list}. Matching the bare keyword as well is what lets {@code mark}
     * on its own be answered with "which task?" rather than "I don't know what
     * that means": the command is recognised first, and only then is it found to
     * be incomplete.
     *
     * <p>A command that takes no arguments matches only its exact keyword, so
     * {@code bye now} is not treated as {@code bye}.
     *
     * @param line one whole line as the user typed it, with surrounding spaces removed
     */
    public boolean matches(String line) {
        if (takesArguments) {
            return line.equals(keyword) || line.startsWith(keyword + " ");
        }
        return line.equals(keyword);
    }

    /**
     * Returns everything the user typed after the keyword, with surrounding
     * spaces removed, or an empty string if they typed nothing after it.
     *
     * @param line a line that {@link #matches} has already accepted
     */
    public String argumentsIn(String line) {
        if (line.length() <= keyword.length()) {
            return "";
        }
        return line.substring(keyword.length() + 1).trim();
    }

    /**
     * Returns the command that {@code line} begins with, if it begins with one.
     *
     * <p>An {@link Optional} is returned rather than {@code null} so that the
     * caller cannot forget the "no such command" case: the result has to be
     * unwrapped before the command inside can be used, and the compiler enforces
     * that. Returning {@code null} would let an unrecognised line travel on
     * unnoticed and fail later as a {@code NullPointerException}, well away from
     * the line that caused it.
     *
     * @param line one whole line as the user typed it, with surrounding spaces removed
     * @return the matching command, or an empty {@code Optional} if there is none
     */
    public static Optional<Command> of(String line) {
        for (Command command : values()) {
            if (command.matches(line)) {
                return Optional.of(command);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns every command word, separated by commas, in the order the constants
     * are declared above — for example {@code todo, deadline, event, ...}.
     *
     * <p>Built from {@link #values()} rather than written out by hand, so adding
     * a command to this enum is all it takes for the chatbot to start offering it.
     */
    public static String allKeywords() {
        StringJoiner keywords = new StringJoiner(", ");
        for (Command command : values()) {
            keywords.add(command.keyword);
        }
        return keywords.toString();
    }
}
