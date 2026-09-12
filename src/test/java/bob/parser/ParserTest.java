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
import bob.command.EditCommand;
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
 * Tests {@link Parser#parse}, which recognizes the command a line begins with and
 * builds it.
 *
 * <p>What follows the command word is read by {@link TaskParser},
 * {@link EditParser} and {@link ArgumentParser}, and the rules for it are tested
 * in their own test classes. The tests here are about the line as a whole: whether
 * it names a command at all, and whether each command word builds its own command.
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
        assertInstanceOf(EditCommand.class, Parser.parse("edit 2 /desc read book"));
    }

    @Test
    public void parse_bye_returnsTheOnlyCommandThatEndsTheConversation() throws BobException {
        assertTrue(Parser.parse("bye").isExit());
        assertFalse(Parser.parse("list").isExit());
        assertFalse(Parser.parse("todo read book").isExit());
    }

    @Test
    public void parse_spacesAroundArguments_trimmed() throws BobException {
        // The spaces are removed before the arguments reach TaskParser, so this is
        // checked through the whole line rather than in TaskParserTest.
        assertEquals("[T][ ] read book", firstTaskFrom("todo    read book   ").toString());
    }

    /**
     * Returns the task that {@code line} adds, by running the command it parses to
     * against an empty task list and taking the one task left in it.
     */
    private Task firstTaskFrom(String line) throws BobException {
        Command command = Parser.parse(line);
        TaskList tasks = new TaskList();
        // The collecting Ui, so that running the command leaves the test session's
        // own output alone. What it says is not what is being tested here.
        command.execute(tasks, Ui.forGui(), new Storage(tempDirectory.resolve("duke.txt")));
        return tasks.get(0);
    }
}
