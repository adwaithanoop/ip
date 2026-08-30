package bob;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Keeps the task list on the hard disk, so the tasks typed in one run of the
 * chatbot are still there in the next one.
 *
 * <p>The file is plain text with one task per line, its fields separated by a
 * vertical bar, for example:
 *
 * <pre>
 * T | 1 | read book
 * D | 0 | return book | 2026-06-06
 * E | 0 | project meeting | 2026-08-06 1400 | 2026-08-06 1600
 * </pre>
 *
 * <p>The first field says which kind of task it is, the second whether it has
 * been done, the third what it is, and any field after that holds a date that
 * kind of task carries. Dates are written the way the user types them at the
 * chatbot, which is the way {@link TaskDate} reads them back. An event's start
 * and end are kept as two fields rather than as one, so that reading them back
 * is a matter of taking two fields apart.
 *
 * <p>A vertical bar or a backslash inside what the user typed is written as
 * {@code \|} or {@code \\}. Without that, a task such as
 * {@code todo tidy up | then rest} would be saved as a line with one field too
 * many, and would be unreadable on the way back in.
 *
 * <p>This class owns the layout of the file. Writing is shared with the tasks
 * themselves: each task says what its fields are in {@link Task#toSaveFields()}
 * and this class joins them, so adding a new kind of task does not mean editing
 * the writing code here. Reading cannot be shared that way — a line has to be
 * recognized before there is a task to ask — so it is all done here.
 *
 * <p>Nothing in this class prints anything. Whatever the user needs to be told
 * is either returned in a {@link LoadResult} or thrown as a {@link BobException},
 * leaving {@link Bob} as the only class that writes to the console.
 */
public class Storage {

    /**
     * Where the tasks are kept, relative to the directory the chatbot is run from.
     *
     * <p>The path is built from its parts rather than written out as one piece of
     * text, so that the separator between them is the one the operating system
     * running the chatbot uses. A path spelled {@code "data/duke.txt"} would carry
     * an assumption about that separator that this project has no reason to make.
     */
    public static final Path DEFAULT_FILE_PATH = Path.of("data", "duke.txt");

    /** Text written between the fields of a saved task. */
    private static final String FIELD_SEPARATOR = " | ";

    /** The character the fields of a saved task are separated by. */
    private static final char FIELD_SEPARATOR_CHARACTER = '|';

    /** The character marking the one after it as part of a field rather than as punctuation. */
    private static final char ESCAPE_CHARACTER = '\\';

    /** How many unreadable lines are reported one by one before the rest are just counted. */
    private static final int MAX_REPORTED_BAD_LINES = 5;

    /** Number of fields a saved {@link Todo} has: kind, status, description. */
    private static final int FIELD_COUNT_TODO = 3;

    /** Number of fields a saved {@link Deadline} has: a {@link Todo}'s three, plus the due date. */
    private static final int FIELD_COUNT_DEADLINE = 4;

    /** Number of fields a saved {@link Event} has: a {@link Todo}'s three, plus a start and an end. */
    private static final int FIELD_COUNT_EVENT = 5;

    /** The file the tasks are read from and written to. */
    private final Path filePath;

    /**
     * Creates storage backed by one file. The file does not have to exist yet:
     * it is created, along with any missing folders above it, the first time
     * the task list is saved.
     *
     * @param filePath where to keep the tasks, for example {@link #DEFAULT_FILE_PATH}.
     */
    public Storage(Path filePath) {
        this.filePath = filePath;
    }

    /**
     * What came of reading the save file: the tasks that could be read, and any
     * messages for the user about parts of the file that could not be.
     *
     * <p>Two things have to come back from a load, and a record is the shortest
     * honest way to return both. It is a plain data carrier, so the compiler
     * writes its constructor, accessors, {@code equals} and {@code toString}.
     *
     * @param tasks    the tasks read from the file, in the order they were saved.
     * @param messages what the user should be told about the load, if anything.
     */
    public record LoadResult(List<Task> tasks, List<String> messages) {
    }

    /**
     * Returns the tasks saved in the file, together with anything the user should
     * be told about reading it.
     *
     * <p>This method does not throw. A chatbot that cannot read its save file can
     * still be used, so every problem is turned into a message and reported rather
     * than being allowed to stop the program starting:
     *
     * <ul>
     *   <li>no file yet — the ordinary first run — is not a problem at all, and is
     *       reported as an empty list with nothing to say;</li>
     *   <li>a file that cannot be read at all gives an empty list and a warning
     *       that it will be overwritten, so the user can quit and rescue it
     *       before typing anything that changes the list;</li>
     *   <li>a line that cannot be understood is skipped and reported, so one
     *       damaged line does not cost the user the tasks on all the others.</li>
     * </ul>
     */
    public LoadResult load() {
        if (!Files.exists(filePath)) {
            return new LoadResult(List.of(), List.of());
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return new LoadResult(List.of(), List.of(
                    "I couldn't read " + filePath + " (" + describe(e) + ").",
                    "I'm starting with an empty list, and that file will be overwritten"
                            + " the next time the list changes."));
        }
        return readTasks(lines);
    }

    /**
     * Turns the lines of the save file into tasks, collecting a message about
     * every line that could not be turned into one.
     *
     * <p>Blank lines are passed over without comment: they are not damage, and a
     * file that a user has opened in an editor can easily end up with one.
     */
    private LoadResult readTasks(List<String> lines) {
        List<Task> loadedTasks = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        int badLineCount = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                loadedTasks.add(parseTask(line));
            } catch (BobException e) {
                badLineCount++;
                // Only the first few are quoted, so a thoroughly damaged file
                // does not bury the greeting under hundreds of lines.
                if (badLineCount <= MAX_REPORTED_BAD_LINES) {
                    // The user counts lines from 1, the list counts from 0.
                    messages.add("Line " + (i + 1) + " of " + filePath
                            + " isn't a task I can read: " + e.getMessage() + ".");
                }
            }
        }
        if (badLineCount > MAX_REPORTED_BAD_LINES) {
            messages.add("...and " + (badLineCount - MAX_REPORTED_BAD_LINES)
                    + " more lines I couldn't read.");
        }
        if (badLineCount > 0) {
            boolean isSingleBadLine = badLineCount == 1;
            messages.add(isSingleBadLine
                    ? "I've left that line out of your list."
                    : "I've left those " + badLineCount + " lines out of your list.");
            // Said plainly, because the next command that changes the list rewrites
            // the whole file, and these lines are not in it to be rewritten.
            messages.add(isSingleBadLine
                    ? "It will be lost the next time the list changes — fix the file to keep it."
                    : "They will be lost the next time the list changes — fix the file to keep them.");
        }
        return new LoadResult(loadedTasks, messages);
    }

    /**
     * Writes the whole task list to the file, replacing whatever was there before.
     *
     * <p>The whole list is rewritten on every change rather than the one changed
     * task being edited in place. That is more writing than is strictly needed,
     * but the file always says exactly what the list says, which is not true of
     * schemes that patch a file in place and can leave it half updated.
     *
     * @param tasks the task list as it now stands.
     * @throws BobException if the file or the folder holding it cannot be written.
     */
    public void save(List<Task> tasks) throws BobException {
        List<String> lines = new ArrayList<>();
        for (Task task : tasks) {
            lines.add(toSaveLine(task));
        }
        try {
            Path parentDirectory = filePath.getParent();
            if (parentDirectory != null) {
                // Does nothing if the folder is already there, so this is safe
                // to call on every save rather than only on the first one.
                Files.createDirectories(parentDirectory);
            }
            Files.write(filePath, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BobException("I couldn't save your tasks to " + filePath
                    + " (" + describe(e) + ")."
                    + "\nThe change is in this session's list, but it won't survive quitting.");
        }
    }

    /** Returns one task written as a line of the save file. */
    private static String toSaveLine(Task task) {
        StringJoiner line = new StringJoiner(FIELD_SEPARATOR);
        for (String field : task.toSaveFields()) {
            line.add(escape(field));
        }
        return line.toString();
    }

    /**
     * Returns the task written on one line of the save file.
     *
     * <p>Every way a line can be wrong is reported separately, because these
     * messages are read by someone looking at the file to repair it, and "line 4
     * is wrong" would leave them to find out how.
     *
     * @param line one line of the file, with surrounding spaces removed.
     * @throws BobException if the line is not a task this chatbot can read.
     */
    private static Task parseTask(String line) throws BobException {
        List<String> fields = splitFields(line);
        String typeIcon = fields.get(0);
        return switch (typeIcon) {
            case Todo.TYPE_ICON -> parseTodo(fields);
            case Deadline.TYPE_ICON -> parseDeadline(fields);
            case Event.TYPE_ICON -> parseEvent(fields);
            default -> throw new BobException("\"" + typeIcon + "\" is not a kind of task I know"
                    + " (I know " + Todo.TYPE_ICON + ", " + Deadline.TYPE_ICON
                    + " and " + Event.TYPE_ICON + ")");
        };
    }

    /** Returns the {@link Todo} written as {@code T | <done> | <description>}. */
    private static Todo parseTodo(List<String> fields) throws BobException {
        requireFieldCount(fields, FIELD_COUNT_TODO, "todo");
        Todo todo = new Todo(requireNonEmpty(fields.get(2), "description"));
        setDone(todo, fields.get(1));
        return todo;
    }

    /** Returns the {@link Deadline} written as {@code D | <done> | <description> | <by>}. */
    private static Deadline parseDeadline(List<String> fields) throws BobException {
        requireFieldCount(fields, FIELD_COUNT_DEADLINE, "deadline");
        Deadline deadline = new Deadline(
                requireNonEmpty(fields.get(2), "description"),
                requireDate(fields.get(3), "due date"));
        setDone(deadline, fields.get(1));
        return deadline;
    }

    /** Returns the {@link Event} written as {@code E | <done> | <description> | <from> | <to>}. */
    private static Event parseEvent(List<String> fields) throws BobException {
        requireFieldCount(fields, FIELD_COUNT_EVENT, "event");
        Event event = new Event(
                requireNonEmpty(fields.get(2), "description"),
                requireDate(fields.get(3), "start time"),
                requireDate(fields.get(4), "end time"));
        setDone(event, fields.get(1));
        return event;
    }

    /**
     * Marks a freshly read task as done if its saved status field says it was.
     *
     * @param task      the task just read from the line, still marked not done.
     * @param doneField the saved status field, which is the only thing it may be:
     *                  {@value Task#DONE_FLAG} or {@value Task#NOT_DONE_FLAG}.
     * @throws BobException if the field is anything else, since a status that
     *                      cannot be read is not safely assumed to mean "not done".
     */
    private static void setDone(Task task, String doneField) throws BobException {
        if (doneField.equals(Task.DONE_FLAG)) {
            task.markAsDone();
        } else if (!doneField.equals(Task.NOT_DONE_FLAG)) {
            throw new BobException("\"" + doneField + "\" doesn't say whether the task is done"
                    + " (it should be " + Task.DONE_FLAG + " or " + Task.NOT_DONE_FLAG + ")");
        }
    }

    /**
     * Checks that a saved line has exactly as many fields as its kind of task needs,
     * so that a field is never read from a position that holds something else.
     *
     * @throws BobException if the line has too few or too many fields.
     */
    private static void requireFieldCount(List<String> fields, int expectedCount, String taskKind)
            throws BobException {
        if (fields.size() != expectedCount) {
            throw new BobException("a saved " + taskKind + " has " + expectedCount
                    + " fields, but this line has " + fields.size());
        }
    }

    /**
     * Returns a field, having checked there is something in it.
     *
     * @throws BobException if the field is empty, which would load a task the user
     *                      could never have added in the first place.
     */
    private static String requireNonEmpty(String field, String fieldName) throws BobException {
        if (field.isEmpty()) {
            throw new BobException("the " + fieldName + " is empty");
        }
        return field;
    }

    /**
     * Returns the date written in a field, having checked that it is one.
     *
     * <p>{@link TaskDate#parse} already refuses text that is not a date, but its
     * complaint is written for someone typing a command and runs to three lines
     * of advice. It is replaced here by a shorter one, because these messages are
     * listed alongside every other complaint about the file and are read by
     * someone repairing it.
     *
     * @param field     the saved field that should hold a date.
     * @param fieldName what to call it in a message, for example {@code due date}.
     * @throws BobException if the field is empty or does not hold a date.
     */
    private static TaskDate requireDate(String field, String fieldName) throws BobException {
        requireNonEmpty(field, fieldName);
        try {
            return TaskDate.parse(field);
        } catch (BobException e) {
            throw new BobException("the " + fieldName + " \"" + field + "\" isn't a date"
                    + " (dates are saved as " + TaskDate.EXAMPLE_DATE + ", or "
                    + TaskDate.EXAMPLE_DATE_TIME + " with a time)");
        }
    }

    /**
     * Returns the text with the two characters that mean something in this file
     * format written as escape sequences, so that a task holding either of them
     * still saves as one readable line.
     *
     * <p>Backslashes are escaped first. The other way round, the backslash added
     * in front of a bar would itself be escaped a moment later, turning
     * {@code |} into {@code \\|} — an escaped backslash followed by a separator.
     */
    private static String escape(String field) {
        return field.replace("\\", "\\\\").replace("|", "\\|");
    }

    /** Returns the text with each escape sequence replaced by the character it stands for. */
    private static String unescape(String field) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < field.length(); i++) {
            char character = field.charAt(i);
            // A backslash is punctuation, so what is kept is the character after it.
            if (character == ESCAPE_CHARACTER && i + 1 < field.length()) {
                i++;
                character = field.charAt(i);
            }
            text.append(character);
        }
        return text.toString();
    }

    /**
     * Returns the fields of one saved line, split at the separators that are
     * separators — an escaped bar belongs to the field it sits in.
     *
     * <p>The line is walked a character at a time rather than being handed to
     * {@code String.split}, which has no way to know that the bar in
     * {@code tidy up \| then rest} is part of the text.
     *
     * <p>Surrounding spaces are trimmed from each field, so that a file written
     * with the separator padded as {@code " | "}, and one written or hand-edited
     * without the padding, both read the same way.
     */
    private static List<String> splitFields(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (character == ESCAPE_CHARACTER && i + 1 < line.length()) {
                // Kept escaped for now; unescaping the whole field comes after the split.
                field.append(character).append(line.charAt(i + 1));
                i++;
            } else if (character == FIELD_SEPARATOR_CHARACTER) {
                fields.add(unescape(field.toString().trim()));
                field.setLength(0);
            } else {
                field.append(character);
            }
        }
        fields.add(unescape(field.toString().trim()));
        return fields;
    }

    /**
     * Returns a short explanation of a file error to show the user.
     *
     * <p>Some file errors carry no message of their own, so the name of the error
     * is shown instead of an empty pair of brackets.
     */
    private static String describe(IOException error) {
        String reason = error.getMessage();
        if (reason == null || reason.isBlank()) {
            return error.getClass().getSimpleName();
        }
        return reason;
    }
}
