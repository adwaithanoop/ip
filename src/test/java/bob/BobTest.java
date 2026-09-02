package bob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the three methods a window holds its conversation through —
 * {@link Bob#getGreeting()}, {@link Bob#getResponse} and {@link Bob#isExit()}.
 *
 * <p>The rest of this class is tested through the text UI test plan, which runs
 * the console chatbot end to end and checks everything it prints. These three
 * cannot be: they print nothing at all. What they do instead — collect the lines
 * of one answer, hand them back, and start again empty for the next — is the whole
 * of what the window depends on, and none of it is reached when the program is run
 * at a console.
 *
 * <p>The mistake worth guarding against here is a chatbot that never forgets: if
 * an answer were not cleared once it had been handed over, every reply would
 * arrive with the whole conversation so far stuck to the front of it. That is what
 * {@link #getResponse_severalCommands_eachAnswerHeldOnItsOwn()} is for.
 *
 * <p>Every test gets a save file in a folder of its own, made and thrown away by
 * JUnit, so none of them touches the real one.
 */
public class BobTest {

    /** A folder to keep each test's save file in, thrown away after the test. */
    @TempDir
    private Path tempDirectory;

    @Test
    public void getGreeting_noSaveFile_greetsWithoutMentioningTasks() {
        String greeting = bobWithSavedLines().getGreeting();

        // No banner: it is drawn in columns, which a window's font would not keep.
        assertEquals("Hello! I'm Bob.\nWhat can I do for you?", greeting);
    }

    @Test
    public void getGreeting_savedTasks_reportsWhatWasPickedUp() {
        String greeting = bobWithSavedLines("T | 0 | read book", "T | 1 | return book").getGreeting();

        assertTrue(greeting.startsWith("Hello! I'm Bob."));
        assertTrue(greeting.contains("I've picked up 2 tasks you saved earlier."));
    }

    @Test
    public void getResponse_addThenList_taskAddedAndListedBack() {
        Bob bob = bobWithSavedLines();

        String added = bob.getResponse("todo read book");
        String listed = bob.getResponse("list");

        assertEquals("Got it. I've added this task:\n  [T][ ] read book\n"
                + "Now you have 1 tasks in the list.", added);
        assertEquals("Here are the tasks in your list:\n1.[T][ ] read book", listed);
    }

    @Test
    public void getResponse_severalCommands_eachAnswerHeldOnItsOwn() {
        Bob bob = bobWithSavedLines();
        bob.getGreeting();
        bob.getResponse("todo read book");

        String second = bob.getResponse("todo return book");

        // Nothing said earlier — not the greeting, not the first confirmation —
        // may still be hanging around in front of this answer.
        assertTrue(second.startsWith("Got it."));
        assertFalse(second.contains("Hello!"));
        assertFalse(second.contains("read book"));
    }

    @Test
    public void getResponse_commandThatCannotBeCarriedOut_explanationReturnedNotThrown() {
        Bob bob = bobWithSavedLines();

        String response = bob.getResponse("sing me a song");

        // A window has nowhere to put an exception, so the complaint comes back as
        // the answer and the conversation carries on.
        assertTrue(response.contains("sing"));
        assertFalse(bob.isExit());
    }

    @Test
    public void getResponse_emptyLine_answeredRatherThanIgnored() {
        // The window declines to send an empty box, but a line of only spaces
        // reaches here as an empty one, and is answered like any other bad input.
        String response = bobWithSavedLines().getResponse("");

        assertTrue(response.startsWith("You didn't type anything."));
    }

    @Test
    public void getResponse_changedList_writtenToTheSaveFile() throws IOException {
        Bob bob = bobWithSavedLines();

        bob.getResponse("todo read book");

        // The window's chatbot saves as the console's does; a task added in one
        // session has to be there in the next.
        assertEquals("T | 0 | read book", Files.readString(saveFile(), StandardCharsets.UTF_8).strip());
    }

    @Test
    public void isExit_onlyAfterGoodbye() {
        Bob bob = bobWithSavedLines();

        assertFalse(bob.isExit());
        bob.getResponse("list");
        assertFalse(bob.isExit());

        String farewell = bob.getResponse("bye");

        assertEquals("Bye. Hope to see you again soon!", farewell);
        assertTrue(bob.isExit());
    }

    /** Returns the save file this test's chatbots use. */
    private Path saveFile() {
        return tempDirectory.resolve("duke.txt");
    }

    /**
     * Returns a chatbot for a window, started on a save file holding the given
     * lines — or on no save file at all when given none, which is the first run.
     */
    private Bob bobWithSavedLines(String... lines) {
        try {
            if (lines.length > 0) {
                Files.write(saveFile(), List.of(lines), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new AssertionError("could not write the test's save file", e);
        }
        return Bob.forGui(saveFile());
    }
}
