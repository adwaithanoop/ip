package bob.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link CommandWord}, which decides whether a line begins with a command
 * the chatbot knows and, if so, where that command's arguments start.
 *
 * <p>The rules here are small but easy to get subtly wrong, and getting them
 * wrong shows up as the chatbot misreading something the user typed rather than
 * as a crash. Two of them are worth stating in tests: a command word only counts
 * when the line ends there or goes on with a space, so {@code todolist} is not
 * {@code todo}; and a command that takes no arguments matches nothing but itself,
 * so {@code bye now} is not {@code bye}.
 */
public class CommandWordTest {

    @Test
    public void of_keywordAlone_commandFound() {
        assertEquals(Optional.of(CommandWord.LIST), CommandWord.of("list"));
        assertEquals(Optional.of(CommandWord.BYE), CommandWord.of("bye"));
        assertEquals(Optional.of(CommandWord.MARK), CommandWord.of("mark"));
    }

    @Test
    public void of_keywordWithArguments_commandFound() {
        assertEquals(Optional.of(CommandWord.TODO), CommandWord.of("todo read book"));
        assertEquals(Optional.of(CommandWord.DELETE), CommandWord.of("delete 2"));
        assertEquals(Optional.of(CommandWord.ON), CommandWord.of("on 2026-12-02"));
    }

    @Test
    public void of_keywordRunTogetherWithItsArgument_noCommandFound() {
        // Requiring the space is what keeps "todolist" from being read as "todo"
        // with the description "list".
        assertEquals(Optional.empty(), CommandWord.of("todolist"));
        assertEquals(Optional.empty(), CommandWord.of("marked 2"));
    }

    @Test
    public void of_argumentAfterACommandThatTakesNone_noCommandFound() {
        assertEquals(Optional.empty(), CommandWord.of("bye now"));
        assertEquals(Optional.empty(), CommandWord.of("list everything"));
    }

    @Test
    public void of_emptyLine_noCommandFound() {
        assertEquals(Optional.empty(), CommandWord.of(""));
    }

    @Test
    public void of_wordInCapitals_noCommandFound() {
        assertEquals(Optional.empty(), CommandWord.of("List"));
        assertEquals(Optional.empty(), CommandWord.of("TODO read book"));
    }

    @Test
    public void of_wordThatIsNotACommand_noCommandFound() {
        assertEquals(Optional.empty(), CommandWord.of("blah"));
    }

    @Test
    public void matches_commandTakingArguments_keywordAloneOrFollowedByASpace() {
        assertTrue(CommandWord.MARK.matches("mark"));
        assertTrue(CommandWord.MARK.matches("mark 2"));
        // Recognized first and found incomplete afterwards, so "mark" on its own
        // can be answered with "which task?" rather than "I don't know that".
        assertFalse(CommandWord.MARK.matches("marker 2"));
        assertFalse(CommandWord.MARK.matches("unmark 2"));
    }

    @Test
    public void matches_commandTakingNoArguments_exactKeywordOnly() {
        assertTrue(CommandWord.BYE.matches("bye"));
        assertFalse(CommandWord.BYE.matches("bye now"));
        assertFalse(CommandWord.BYE.matches("byebye"));
    }

    @Test
    public void argumentsIn_argumentsGiven_returnedWithoutSurroundingSpaces() {
        assertEquals("2", CommandWord.MARK.argumentsIn("mark 2"));
        assertEquals("read book", CommandWord.TODO.argumentsIn("todo    read book   "));
    }

    @Test
    public void argumentsIn_spacesInsideTheArguments_keptAsTyped() {
        // Only the ends are trimmed; what is between words is the user's text.
        assertEquals("read  book", CommandWord.TODO.argumentsIn("todo read  book"));
    }

    @Test
    public void argumentsIn_nothingAfterTheKeyword_emptyString() {
        assertEquals("", CommandWord.MARK.argumentsIn("mark"));
        assertEquals("", CommandWord.TODO.argumentsIn("todo "));
    }

    @Test
    public void getKeyword_eachCommand_returnsTheWordTheUserTypes() {
        assertEquals("todo", CommandWord.TODO.getKeyword());
        assertEquals("unmark", CommandWord.UNMARK.getKeyword());
    }

    @Test
    public void allKeywords_calledOnce_listsEveryCommandInDeclarationOrder() {
        String keywords = CommandWord.allKeywords();

        assertEquals("todo, deadline, event, list, on, before, after, next, find, mark,"
                + " unmark, delete, bye", keywords);
    }

    @Test
    public void allKeywords_calledOnce_namesEveryConstantThatExists() {
        // Built from values(), so a command added to the enum cannot be left out
        // of the list the chatbot offers a puzzled user.
        String keywords = CommandWord.allKeywords();

        for (CommandWord command : CommandWord.values()) {
            assertTrue(keywords.contains(command.getKeyword()));
        }
    }
}
