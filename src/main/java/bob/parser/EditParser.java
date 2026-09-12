package bob.parser;

import static bob.parser.TaskParser.BY_KEYWORD;
import static bob.parser.TaskParser.FROM_KEYWORD;
import static bob.parser.TaskParser.TO_KEYWORD;

import java.util.List;
import java.util.Optional;

import bob.BobException;
import bob.command.CommandWord;
import bob.command.EditCommand;
import bob.task.TaskDateTime;
import bob.task.TaskEdit;

/**
 * Parses the text following {@link CommandWord#EDIT} into the {@link EditCommand}
 * it asks for.
 *
 * <p>An edit uses the same markers as a new task, plus {@value #DESC_KEYWORD}, but
 * lets them come in any order and lets any of them be left out. That takes rules a
 * new task does not need — where each value ends, and which missing value to
 * report first — so they are kept here rather than in {@link TaskParser}, which
 * reads the markers in the one order a new task is written in.
 */
class EditParser {

    /** Marker introducing a task's new description. */
    private static final String DESC_KEYWORD = "/desc";

    /**
     * Every marker an edit may use, in the order in which complaints about them are made.
     *
     * <p>This is not an order the user has to type them in. Each marker says which
     * detail follows it, so the markers of an edit may come in any order.
     */
    private static final List<String> EDIT_KEYWORDS =
            List.of(DESC_KEYWORD, BY_KEYWORD, FROM_KEYWORD, TO_KEYWORD);

    /**
     * Returns the {@link EditCommand} described by the text following
     * {@link CommandWord#EDIT}: a task number, then one or more markers, each
     * followed by the new value of one detail of that task.
     *
     * <p>Each value runs from its marker to whichever marker comes next in the text,
     * so the order carries no meaning that a rule about it would protect. The price,
     * shared with {@link TaskParser#parseDeadline} and {@link TaskParser#parseEvent},
     * is that a new description cannot itself contain a marker.
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
    static EditCommand parseEdit(String arguments) throws BobException {
        // The task number is everything up to the first space; the changes are the rest.
        String[] parts = arguments.split("\\s+", 2);
        int taskNumber = ArgumentParser.parseTaskNumber(parts[0], CommandWord.EDIT);
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
}
