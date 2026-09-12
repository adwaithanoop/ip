package bob.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import bob.BobException;
import bob.task.Deadline;
import bob.task.Event;
import bob.task.Task;
import bob.task.TaskDateTime;
import bob.task.Todo;

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
 * chatbot, which is the way {@link TaskDateTime} reads them back. An event's start
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
 * leaving {@link bob.ui.Ui Ui} as the only class that writes to the console.
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

    /**
     * Added to the save file's name to name the temporary file a save writes first, so
     * that {@code data/duke.txt} is saved by way of {@code data/duke.txt.tmp}.
     */
    private static final String TEMP_FILE_SUFFIX = ".tmp";

    /** How many unreadable lines are reported one by one before the rest are just counted. */
    private static final int MAX_REPORTED_BAD_LINES = 5;

    /** Position of the field saying which kind of task a saved line holds. */
    private static final int FIELD_INDEX_TYPE = 0;

    /** Position of the field saying whether a saved task has been done. */
    private static final int FIELD_INDEX_DONE = 1;

    /** Position of the field holding what a saved task is. */
    private static final int FIELD_INDEX_DESCRIPTION = 2;

    /** Position of a saved {@link Deadline}'s due date. */
    private static final int FIELD_INDEX_BY = 3;

    /**
     * Position of a saved {@link Event}'s start.
     *
     * <p>The same position as {@link #FIELD_INDEX_BY}, since both are the first field
     * after the three every task has. It is named separately so that each reading
     * says which date it expects to find there.
     */
    private static final int FIELD_INDEX_FROM = 3;

    /** Position of a saved {@link Event}'s end. */
    private static final int FIELD_INDEX_TO = 4;

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
     * Represents what came of reading the save file: the tasks that could be read,
     * and any messages for the user about parts of the file that could not be.
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
     *       damaged line does not cost the user the tasks on all the others;</li>
     *   <li>lines holding the same task are all loaded, and reported together, so
     *       the user can decide which of them to delete.</li>
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
     * every line that could not be turned into one, and about lines that hold the
     * same task.
     *
     * <p>Blank lines are passed over without comment: they are not damage, and a
     * file that a user has opened in an editor can easily end up with one.
     */
    private LoadResult readTasks(List<String> lines) {
        List<Task> loadedTasks = new ArrayList<>();
        // The line each loaded task came from, kept so that a report names the file's
        // own line even when blank or damaged lines have been passed over.
        List<Integer> loadedLineNumbers = new ArrayList<>();
        List<String> badLineReports = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            // The user counts lines from 1, the list counts from 0.
            int lineNumber = i + 1;
            try {
                loadedTasks.add(parseTask(line));
                loadedLineNumbers.add(lineNumber);
            } catch (BobException e) {
                badLineReports.add("Line " + lineNumber + " of " + filePath
                        + " isn't a task I can read: " + e.getMessage() + ".");
            }
        }
        List<String> messages = new ArrayList<>(summarizeBadLines(badLineReports));
        messages.addAll(reportSameTaskLines(loadedTasks, loadedLineNumbers));
        return new LoadResult(loadedTasks, messages);
    }

    /**
     * Returns a message for each group of lines in the save file that hold the same
     * task, in the sense of {@link Task#isSameTaskAs}.
     *
     * <p>Every line of a group is loaded, rather than all but the first being left out.
     * The lines may differ in ways the user cares about — one marked done and the other
     * not, or the description written in different capitals — so which to keep is
     * theirs to decide. Adding such a task at the chatbot is refused, so only a file
     * edited by hand can hold two.
     *
     * <p>Each task is compared with every task after it, which is plenty fast for a
     * list a person keeps by hand.
     *
     * @param tasks       the tasks read from the file, in the order they were saved.
     * @param lineNumbers the line each of those tasks was read from, counting from 1.
     * @return the messages to show, which are none at all when no two lines hold the same task.
     */
    private List<String> reportSameTaskLines(List<Task> tasks, List<Integer> lineNumbers) {
        List<String> messages = new ArrayList<>();
        // A task already named in a group must not start a second group of its own.
        boolean[] isAlreadyReported = new boolean[tasks.size()];
        for (int i = 0; i < tasks.size(); i++) {
            if (isAlreadyReported[i]) {
                continue;
            }
            List<Integer> sameTaskLineNumbers = new ArrayList<>(List.of(lineNumbers.get(i)));
            for (int j = i + 1; j < tasks.size(); j++) {
                if (tasks.get(i).isSameTaskAs(tasks.get(j))) {
                    sameTaskLineNumbers.add(lineNumbers.get(j));
                    isAlreadyReported[j] = true;
                }
            }
            if (sameTaskLineNumbers.size() > 1) {
                messages.add(describeSameTaskLines(sameTaskLineNumbers));
            }
        }
        return messages;
    }

    /**
     * Returns the message about one group of lines holding the same task, for example
     * {@code Lines 2 and 5 of data/duke.txt are the same task.}, followed by a note
     * that all of them were kept.
     *
     * @param lineNumbers the lines holding that task, at least two, in the order they
     *                    appear in the file.
     */
    private String describeSameTaskLines(List<Integer> lineNumbers) {
        int lineCount = lineNumbers.size();
        assert lineCount >= 2 : "A group of the same task has at least two lines, not " + lineCount;
        List<String> numberTexts = lineNumbers.stream()
                .map(String::valueOf)
                .toList();
        String namedLines = String.join(", ", numberTexts.subList(0, lineCount - 1))
                + " and " + numberTexts.get(lineCount - 1);
        String keptNote = (lineCount == 2)
                ? "I've kept both — delete the one you don't need."
                : "I've kept all " + lineCount + " — delete the ones you don't need.";
        return "Lines " + namedLines + " of " + filePath + " are the same task. " + keptNote;
    }

    /**
     * Returns what the user should be told about the lines of the save file that
     * could not be read, given a report on each of them.
     *
     * <p>Kept apart from {@link #readTasks}, which walks the file, because this is
     * different work: deciding how many of the reports to quote, and how to word
     * what is said about the rest.
     *
     * @param badLineReports one report per unreadable line, in the order the lines
     *                       appear in the file.
     * @return the messages to show, which are none at all when every line was read.
     */
    private static List<String> summarizeBadLines(List<String> badLineReports) {
        int badLineCount = badLineReports.size();
        if (badLineCount == 0) {
            return List.of();
        }

        // Only the first few are quoted, so a thoroughly damaged file
        // does not bury the greeting under hundreds of lines.
        int quotedCount = Math.min(badLineCount, MAX_REPORTED_BAD_LINES);
        List<String> messages = new ArrayList<>(badLineReports.subList(0, quotedCount));
        int unquotedLineCount = badLineCount - quotedCount;
        if (unquotedLineCount > 0) {
            messages.add("...and " + unquotedLineCount
                    + (unquotedLineCount == 1 ? " more line" : " more lines")
                    + " I couldn't read.");
        }

        boolean isSingleBadLine = badLineCount == 1;
        messages.add(isSingleBadLine
                ? "I've left that line out of your list."
                : "I've left those " + badLineCount + " lines out of your list.");
        // Said plainly, because the next command that changes the list rewrites
        // the whole file, and these lines are not in it to be rewritten.
        messages.add(isSingleBadLine
                ? "It will be lost the next time the list changes — fix the file to keep it."
                : "They will be lost the next time the list changes — fix the file to keep them.");
        return messages;
    }

    /**
     * Writes the whole task list to the file, replacing whatever was there before.
     *
     * <p>The whole list is rewritten on every change rather than the one changed
     * task being edited in place. That is more writing than is strictly needed,
     * but it keeps the file saying exactly what the list says.
     *
     * <p>The lines are not written into the save file itself. They go to a temporary
     * file beside it, which is then moved over it. Writing straight into the save file
     * empties it first, so a disk filling up, or the chatbot being stopped, partway
     * through would leave it half written and every task after that point gone. Moved
     * into place, the file holds the old list or the new one, never part of each. A
     * temporary file left by a save that failed is removed, and one left by a chatbot
     * that was stopped is simply overwritten by the next save.
     *
     * <p>What this does not guard against is a power cut just after a save, before the
     * operating system has put the new contents on the disk. Forcing them there before
     * the move, through a {@link java.nio.channels.FileChannel}, would close that gap at
     * the price of slower saves, which a task list kept by hand does not need.
     *
     * <p>The lines are built with a stream because each task turns into exactly
     * one line, independently of the others, which is what {@code map} describes.
     *
     * @param tasks the task list as it now stands.
     * @throws BobException if the file or the folder holding it cannot be written.
     */
    public void save(List<Task> tasks) throws BobException {
        List<String> lines = tasks.stream()
                .map(Storage::toSaveLine)
                .toList();
        Path tempFilePath = filePath.resolveSibling(filePath.getFileName() + TEMP_FILE_SUFFIX);
        try {
            Path parentDirectory = filePath.getParent();
            if (parentDirectory != null) {
                // Does nothing if the folder is already there, so this is safe
                // to call on every save rather than only on the first one.
                Files.createDirectories(parentDirectory);
            }
            Files.write(tempFilePath, lines, StandardCharsets.UTF_8);
            moveIntoPlace(tempFilePath);
        } catch (IOException e) {
            deleteLeftover(tempFilePath);
            throw new BobException("I couldn't save your tasks to " + filePath
                    + " (" + describe(e) + ")."
                    + "\nThe change is in this session's list, but it won't survive quitting.");
        }
    }

    /**
     * Moves a fully written temporary file over the save file.
     *
     * <p>An atomic move is asked for first, which swaps the file in one step, so there
     * is no moment at which the save file is missing or partly replaced. A file system
     * that cannot move a file that way is given an ordinary replacing move instead,
     * which is the best it offers.
     *
     * @param tempFilePath the temporary file holding the whole new list.
     * @throws IOException if the file cannot be moved into place.
     */
    private void moveIntoPlace(Path tempFilePath) throws IOException {
        try {
            Files.move(tempFilePath, filePath, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tempFilePath, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Removes the temporary file a failed save may have left beside the save file.
     *
     * <p>A failure to remove it is ignored. The save has already failed, which is what
     * the user is told, and a temporary file that could not be removed is overwritten
     * by the next save that works.
     */
    private static void deleteLeftover(Path tempFilePath) {
        try {
            Files.deleteIfExists(tempFilePath);
        } catch (IOException e) {
            // Deliberately ignored, for the reason given above.
        }
    }

    /** Returns one task written as a line of the save file. */
    private static String toSaveLine(Task task) {
        return task.toSaveFields().stream()
                .map(Storage::escape)
                .collect(Collectors.joining(FIELD_SEPARATOR));
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
        // splitFields adds a field after the last separator whether or not there is
        // anything in it, so even a line with nothing on it comes back as one field.
        assert !fields.isEmpty() : "A saved line always splits into at least one field";
        String typeIcon = fields.get(FIELD_INDEX_TYPE);
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
        Todo todo = new Todo(requireNonEmpty(fields.get(FIELD_INDEX_DESCRIPTION), "description"));
        setDone(todo, fields.get(FIELD_INDEX_DONE));
        return todo;
    }

    /** Returns the {@link Deadline} written as {@code D | <done> | <description> | <by>}. */
    private static Deadline parseDeadline(List<String> fields) throws BobException {
        requireFieldCount(fields, FIELD_COUNT_DEADLINE, "deadline");
        Deadline deadline = new Deadline(
                requireNonEmpty(fields.get(FIELD_INDEX_DESCRIPTION), "description"),
                requireDate(fields.get(FIELD_INDEX_BY), "due date"));
        setDone(deadline, fields.get(FIELD_INDEX_DONE));
        return deadline;
    }

    /**
     * Returns the {@link Event} written as {@code E | <done> | <description> | <from> | <to>}.
     *
     * <p>The start and the end are checked as a pair by {@link Event#requireValidPeriod},
     * the rule an event typed at the chatbot meets, so a hand-edited file cannot load an
     * event the user could never have added. Its message is replaced by a shorter one,
     * for the reason given at {@link #requireDate}.
     */
    private static Event parseEvent(List<String> fields) throws BobException {
        requireFieldCount(fields, FIELD_COUNT_EVENT, "event");
        String description = requireNonEmpty(fields.get(FIELD_INDEX_DESCRIPTION), "description");
        TaskDateTime from = requireDate(fields.get(FIELD_INDEX_FROM), "start time");
        TaskDateTime to = requireDate(fields.get(FIELD_INDEX_TO), "end time");
        try {
            Event.requireValidPeriod(from, to);
        } catch (BobException e) {
            // The rule has already decided the pair is refused; comparing the two
            // again only picks which of its two reasons to give.
            throw new BobException(to.compareTo(from) < 0
                    ? "the event ends before it starts"
                    : "the event starts and ends at the same moment");
        }
        Event event = new Event(description, from, to);
        setDone(event, fields.get(FIELD_INDEX_DONE));
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
     * <p>{@link TaskDateTime#parse} already refuses text that is not a date, but its
     * complaint is written for someone typing a command and runs to three lines
     * of advice. It is replaced here by a shorter one, because these messages are
     * listed alongside every other complaint about the file and are read by
     * someone repairing it.
     *
     * @param field     the saved field that should hold a date.
     * @param fieldName what to call it in a message, for example {@code due date}.
     * @throws BobException if the field is empty or does not hold a date.
     */
    private static TaskDateTime requireDate(String field, String fieldName) throws BobException {
        requireNonEmpty(field, fieldName);
        try {
            return TaskDateTime.parse(field);
        } catch (BobException e) {
            throw new BobException("the " + fieldName + " \"" + field + "\" isn't a date"
                    + " (dates are saved as " + TaskDateTime.EXAMPLE_DATE + ", or "
                    + TaskDateTime.EXAMPLE_DATE_TIME + " with a time)");
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
        boolean isEscaped = false;
        for (char character : field.toCharArray()) {
            // A backslash is punctuation, so what is kept is the character after it.
            if (character == ESCAPE_CHARACTER && !isEscaped) {
                isEscaped = true;
            } else {
                text.append(character);
                isEscaped = false;
            }
        }
        if (isEscaped) {
            // A backslash at the very end escapes nothing, so it is kept as written.
            text.append(ESCAPE_CHARACTER);
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
        boolean isEscaped = false;
        for (char character : line.toCharArray()) {
            if (isEscaped) {
                // Kept escaped for now; unescaping the whole field comes after the split.
                field.append(ESCAPE_CHARACTER).append(character);
                isEscaped = false;
            } else if (character == ESCAPE_CHARACTER) {
                isEscaped = true;
            } else if (character == FIELD_SEPARATOR_CHARACTER) {
                fields.add(unescape(field.toString().trim()));
                field.setLength(0);
            } else {
                field.append(character);
            }
        }
        if (isEscaped) {
            // A backslash at the very end escapes nothing, so it is kept as written.
            field.append(ESCAPE_CHARACTER);
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
