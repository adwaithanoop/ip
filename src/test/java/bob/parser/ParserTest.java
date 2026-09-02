package bob.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import bob.BobException;
import bob.command.AddCommand;
import bob.command.AfterCommand;
import bob.command.BeforeCommand;
import bob.command.Command;
import bob.command.DeleteCommand;
import bob.command.ExitCommand;
import bob.command.FindCommand;
import bob.command.ListCommand;
import bob.command.MarkCommand;
import bob.command.NextCommand;
import bob.command.OnCommand;
import bob.storage.Storage;
import bob.task.Task;
import bob.task.TaskList;
import bob.ui.Ui;

/**
 * Tests {@link Parser#parse}, which turns a line the user typed into the command
 * the chatbot runs.
 *
 * <p>This is the class with the most rules in it, and all of them are about text
 * the user is free to type wrongly: which word starts the line, where
 * {@code /by}, {@code /from} and {@code /to} sit, whether what follows them is a
 * date, and whether a task number is a number at all. Every one of those rules is
 * a way the chatbot can be made to misbehave by typing, so they are worth pinning
 * down here rather than only through the text UI.
 *
 * <p>Two things are checked for each line: which command came back, and — for the
 * commands that build a task — what that task turned out to be. The command
 * classes keep what they were built with to themselves, so the task is looked at
 * by running the command against a real {@link TaskList} and reading the task out
 * of it. That does mean these tests run a little more than the parser, but the
 * alternative is asserting only that some {@link AddCommand} came back, which
 * would leave the description and date parsing — the part actually worth testing
 * — unchecked.
 */
public class ParserTest {

    /** A folder for the save file the commands write to, thrown away after each test. */
    @TempDir
    private Path tempDirectory;

    @Test
    public void parse_emptyLine_exceptionThrown() {
        BobException exception = assertThrows(BobException.class, () -> Parser.parse(""));

        assertTrue(exception.getMessage().contains("didn't type anything"));
    }

    @Test
    public void parse_unknownCommand_exceptionListsTheKnownOnes() {
        BobException exception = assertThrows(BobException.class, () -> Parser.parse("blah"));

        assertTrue(exception.getMessage().contains("\"blah\""));
        assertTrue(exception.getMessage().contains("todo"));
        assertTrue(exception.getMessage().contains("delete"));
    }

    @Test
    public void parse_commandWordRunTogetherWithItsArgument_exceptionThrown() {
        // "todolist" is not "todo" with the description "list", because a command
        // word only counts when the line goes on with a space after it.
        assertThrows(BobException.class, () -> Parser.parse("todolist"));
    }

    @Test
    public void parse_argumentAfterCommandThatTakesNone_exceptionThrown() {
        assertThrows(BobException.class, () -> Parser.parse("bye now"));
        assertThrows(BobException.class, () -> Parser.parse("list all"));
    }

    @Test
    public void parse_commandInCapitals_exceptionThrown() {
        // The chatbot's vocabulary is lower case, and nothing quietly folds case.
        assertThrows(BobException.class, () -> Parser.parse("TODO read book"));
    }

    @Test
    public void parse_eachCommandWord_returnsItsOwnCommand() throws BobException {
        assertInstanceOf(ListCommand.class, Parser.parse("list"));
        assertInstanceOf(ExitCommand.class, Parser.parse("bye"));
        assertInstanceOf(AddCommand.class, Parser.parse("todo read book"));
        assertInstanceOf(OnCommand.class, Parser.parse("on 2026-12-02"));
        assertInstanceOf(BeforeCommand.class, Parser.parse("before 2026-12-02"));
        assertInstanceOf(AfterCommand.class, Parser.parse("after 2026-12-02"));
        assertInstanceOf(NextCommand.class, Parser.parse("next 3"));
        assertInstanceOf(FindCommand.class, Parser.parse("find book"));
        assertInstanceOf(MarkCommand.class, Parser.parse("mark 2"));
        assertInstanceOf(MarkCommand.class, Parser.parse("unmark 2"));
        assertInstanceOf(DeleteCommand.class, Parser.parse("delete 2"));
    }

    @Test
    public void parse_bye_returnsTheOnlyCommandThatEndsTheConversation() throws BobException {
        assertTrue(Parser.parse("bye").isExit());
        assertFalse(Parser.parse("list").isExit());
        assertFalse(Parser.parse("todo read book").isExit());
    }

    @Test
    public void parseTodo_description_todoBuilt() throws BobException {
        assertEquals("[T][ ] read book", firstTaskFrom("todo read book").toString());
    }

    @Test
    public void parseTodo_spacesAroundDescription_trimmed() throws BobException {
        assertEquals("[T][ ] read book", firstTaskFrom("todo    read book   ").toString());
    }

    @Test
    public void parseTodo_noDescription_exceptionThrown() {
        assertThrows(BobException.class, () -> Parser.parse("todo"));
        assertThrows(BobException.class, () -> Parser.parse("todo    "));
    }

    @Test
    public void parseDeadline_descriptionAndDate_deadlineBuilt() throws BobException {
        Task task = firstTaskFrom("deadline return book /by 2026-12-02");

        assertEquals("[D][ ] return book (by: Dec 02 2026)", task.toString());
    }

    @Test
    public void parseDeadline_dateWithTime_timeKept() throws BobException {
        Task task = firstTaskFrom("deadline return book /by 2026-12-02 1800");

        assertEquals("[D][ ] return book (by: Dec 02 2026 18:00)", task.toString());
    }

    @Test
    public void parseDeadline_missingMarker_exceptionThrown() {
        assertThrows(BobException.class, () -> Parser.parse("deadline return book"));
    }

    @Test
    public void parseDeadline_missingDescription_exceptionThrown() {
        assertThrows(BobException.class, () -> Parser.parse("deadline /by 2026-12-02"));
    }

    @Test
    public void parseDeadline_missingDate_exceptionThrown() {
        assertThrows(BobException.class, () -> Parser.parse("deadline return book /by"));
        assertThrows(BobException.class, () -> Parser.parse("deadline return book /by   "));
    }

    @Test
    public void parseDeadline_dateThatIsNotADate_exceptionThrown() {
        assertThrows(BobException.class, () -> Parser.parse("deadline return book /by Sunday"));
        assertThrows(BobException.class, () -> Parser.parse("deadline return book /by 2026-02-30"));
    }

    @Test
    public void parseEvent_descriptionStartAndEnd_eventBuilt() throws BobException {
        Task task = firstTaskFrom("event project meeting /from 2026-12-02 1800 /to 2026-12-02 2000");

        assertEquals("[E][ ] project meeting (from: Dec 02 2026 18:00 to: Dec 02 2026 20:00)",
                task.toString());
    }

    @Test
    public void parseEvent_endMarkerTextInsideDescription_laterMarkerUsed() throws BobException {
        // The /to that separates the times is the one after /from, so the /to in
        // the description is left where it is.
        Task task = firstTaskFrom("event walk /to town /from 2026-12-02 /to 2026-12-03");

        assertEquals("[E][ ] walk /to town (from: Dec 02 2026 to: Dec 03 2026)", task.toString());
    }

    @Test
    public void parseEvent_endSameAsStart_eventBuilt() throws BobException {
        // A moment in time is a thing a user may mean, so it is not refused.
        Task task = firstTaskFrom("event photo /from 2026-12-02 1800 /to 2026-12-02 1800");

        assertEquals("[E][ ] photo (from: Dec 02 2026 18:00 to: Dec 02 2026 18:00)", task.toString());
    }

    @Test
    public void parseEvent_endBeforeStart_exceptionThrown() {
        BobException exception = assertThrows(BobException.class, () ->
                Parser.parse("event meeting /from 2026-12-02 2000 /to 2026-12-02 1800"));

        assertTrue(exception.getMessage().contains("can't end before it starts"));
    }

    @Test
    public void parseEvent_missingMarker_exceptionThrown() {
        assertThrows(BobException.class, () -> Parser.parse("event meeting /to 2026-12-02"));
        assertThrows(BobException.class, () -> Parser.parse("event meeting /from 2026-12-02"));
    }

    @Test
    public void parseEvent_missingDescription_exceptionThrown() {
        assertThrows(BobException.class, () ->
                Parser.parse("event /from 2026-12-02 /to 2026-12-03"));
    }

    @Test
    public void parseEvent_missingStartOrEnd_exceptionThrown() {
        assertThrows(BobException.class, () -> Parser.parse("event meeting /from /to 2026-12-03"));
        assertThrows(BobException.class, () -> Parser.parse("event meeting /from 2026-12-02 /to"));
    }

    @Test
    public void parseEvent_timeThatIsNotADate_exceptionThrown() {
        assertThrows(BobException.class, () ->
                Parser.parse("event meeting /from soon /to 2026-12-03"));
    }

    @Test
    public void parseTaskNumber_noNumber_exceptionThrown() {
        assertThrows(BobException.class, () -> Parser.parse("mark"));
        assertThrows(BobException.class, () -> Parser.parse("unmark"));
        assertThrows(BobException.class, () -> Parser.parse("delete"));
    }

    @Test
    public void parseTaskNumber_notANumber_exceptionThrown() {
        BobException exception = assertThrows(BobException.class, () -> Parser.parse("mark seven"));

        assertTrue(exception.getMessage().contains("\"seven\""));
    }

    @Test
    public void parseTaskNumber_numberOutsideTheList_accepted() throws BobException {
        // Whether a number names a task the user actually has is a fact about the
        // list, not about the text, so it is not this class's complaint to make.
        assertInstanceOf(MarkCommand.class, Parser.parse("mark 0"));
        assertInstanceOf(MarkCommand.class, Parser.parse("mark 99"));
        assertInstanceOf(DeleteCommand.class, Parser.parse("delete -1"));
    }

    @Test
    public void parseDay_noDay_exceptionThrown() {
        assertThrows(BobException.class, () -> Parser.parse("on"));
        assertThrows(BobException.class, () -> Parser.parse("before"));
        assertThrows(BobException.class, () -> Parser.parse("after"));
    }

    @Test
    public void parseDay_dayThatIsNotADay_exceptionThrown() {
        assertThrows(BobException.class, () -> Parser.parse("on Sunday"));
        assertThrows(BobException.class, () -> Parser.parse("before 02/12/2026"));
    }

    @Test
    public void parseDay_timeAfterTheDay_exceptionThrown() {
        // Refused rather than quietly read as the whole day, since the chatbot
        // does not answer questions about part of a day.
        assertThrows(BobException.class, () -> Parser.parse("on 2026-12-02 1800"));
    }

    @Test
    public void parseCount_noCount_exceptionThrown() {
        assertThrows(BobException.class, () -> Parser.parse("next"));
    }

    @Test
    public void parseCount_notANumber_exceptionThrown() {
        assertThrows(BobException.class, () -> Parser.parse("next a few"));
    }

    @Test
    public void parseCount_lessThanOne_exceptionThrown() {
        // Showing nothing would leave a user who typed this none the wiser.
        assertThrows(BobException.class, () -> Parser.parse("next 0"));
        assertThrows(BobException.class, () -> Parser.parse("next -3"));
    }

    @Test
    public void parseCount_oneOrMore_accepted() throws BobException {
        assertInstanceOf(NextCommand.class, Parser.parse("next 1"));
        assertInstanceOf(NextCommand.class, Parser.parse("next 100"));
    }

    @Test
    public void parseKeyword_noKeyword_exceptionThrown() {
        // Matching every task would be a listing the user already has in "list".
        BobException exception = assertThrows(BobException.class, () -> Parser.parse("find"));

        assertTrue(exception.getMessage().contains("What should I look for?"));
    }

    @Test
    public void parseKeyword_severalWords_acceptedAsOnePhrase() throws BobException {
        // Everything after the command word is the keyword, so a phrase is a
        // search the user may make.
        assertInstanceOf(FindCommand.class, Parser.parse("find sports club"));
    }

    @Test
    public void parseKeyword_textThatMatchesNothing_accepted() throws BobException {
        // Whether anything matches is a fact about the list, not about the text,
        // so a search that finds nothing is answered rather than refused here.
        assertInstanceOf(FindCommand.class, Parser.parse("find zzz"));
    }

    /**
     * Returns the task that {@code line} adds, by running the command it parses to
     * against an empty task list and taking the one task left in it.
     */
    private Task firstTaskFrom(String line) throws BobException {
        Command command = Parser.parse(line);
        TaskList tasks = new TaskList();
        command.execute(tasks, new Ui(), new Storage(tempDirectory.resolve("duke.txt")));
        return tasks.get(0);
    }
}
