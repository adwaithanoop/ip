package bob.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import bob.BobException;
import bob.task.Deadline;
import bob.task.Event;
import bob.task.Todo;

/**
 * Tests {@link TaskParser}, which reads the description and dates of a task being
 * added.
 *
 * <p>Each method under test returns the task it built, so the task is checked
 * through its {@code toString}, which shows the description and every date that
 * was read.
 */
public class TaskParserTest {

    @Test
    public void parseTodo_description_todoBuilt() throws BobException {
        Todo todo = TaskParser.parseTodo("read book");

        assertEquals("[T][ ] read book", todo.toString());
    }

    @Test
    public void parseTodo_noDescription_exceptionThrown() {
        assertThrows(BobException.class, () -> TaskParser.parseTodo(""));
    }

    @Test
    public void parseDeadline_descriptionAndDate_deadlineBuilt() throws BobException {
        Deadline deadline = TaskParser.parseDeadline("return book /by 2026-12-02");

        assertEquals("[D][ ] return book (by: Dec 02 2026)", deadline.toString());
    }

    @Test
    public void parseDeadline_dateWithTime_timeKept() throws BobException {
        Deadline deadline = TaskParser.parseDeadline("return book /by 2026-12-02 1800");

        assertEquals("[D][ ] return book (by: Dec 02 2026 18:00)", deadline.toString());
    }

    @Test
    public void parseDeadline_missingMarker_exceptionThrown() {
        assertThrows(BobException.class, () -> TaskParser.parseDeadline("return book"));
    }

    @Test
    public void parseDeadline_missingDescription_exceptionThrown() {
        assertThrows(BobException.class, () -> TaskParser.parseDeadline("/by 2026-12-02"));
    }

    @Test
    public void parseDeadline_missingDate_exceptionThrown() {
        assertThrows(BobException.class, () -> TaskParser.parseDeadline("return book /by"));
        assertThrows(BobException.class, () -> TaskParser.parseDeadline("return book /by   "));
    }

    @Test
    public void parseDeadline_dateThatIsNotADate_exceptionThrown() {
        assertThrows(BobException.class, () -> TaskParser.parseDeadline("return book /by Sunday"));
        assertThrows(BobException.class, () -> TaskParser.parseDeadline("return book /by 2026-02-30"));
    }

    @Test
    public void parseEvent_descriptionStartAndEnd_eventBuilt() throws BobException {
        Event event = TaskParser.parseEvent("project meeting /from 2026-12-02 1800 /to 2026-12-02 2000");

        assertEquals("[E][ ] project meeting (from: Dec 02 2026 18:00 to: Dec 02 2026 20:00)",
                event.toString());
    }

    @Test
    public void parseEvent_endMarkerTextInsideDescription_laterMarkerUsed() throws BobException {
        // The /to that separates the times is the one after /from, so the /to in
        // the description is left where it is.
        Event event = TaskParser.parseEvent("walk /to town /from 2026-12-02 /to 2026-12-03");

        assertEquals("[E][ ] walk /to town (from: Dec 02 2026 to: Dec 03 2026)", event.toString());
    }

    @Test
    public void parseEvent_endSameAsStart_eventBuilt() throws BobException {
        // A moment in time is a thing a user may mean, so it is not refused.
        Event event = TaskParser.parseEvent("photo /from 2026-12-02 1800 /to 2026-12-02 1800");

        assertEquals("[E][ ] photo (from: Dec 02 2026 18:00 to: Dec 02 2026 18:00)", event.toString());
    }

    @Test
    public void parseEvent_endBeforeStart_exceptionThrown() {
        BobException exception = assertThrows(BobException.class, () ->
                TaskParser.parseEvent("meeting /from 2026-12-02 2000 /to 2026-12-02 1800"));

        assertTrue(exception.getMessage().contains("can't end before it starts"));
    }

    @Test
    public void parseEvent_missingMarker_exceptionThrown() {
        assertThrows(BobException.class, () -> TaskParser.parseEvent("meeting /to 2026-12-02"));
        assertThrows(BobException.class, () -> TaskParser.parseEvent("meeting /from 2026-12-02"));
    }

    @Test
    public void parseEvent_missingDescription_exceptionThrown() {
        assertThrows(BobException.class, () -> TaskParser.parseEvent("/from 2026-12-02 /to 2026-12-03"));
    }

    @Test
    public void parseEvent_missingStartOrEnd_exceptionThrown() {
        assertThrows(BobException.class, () -> TaskParser.parseEvent("meeting /from /to 2026-12-03"));
        assertThrows(BobException.class, () -> TaskParser.parseEvent("meeting /from 2026-12-02 /to"));
    }

    @Test
    public void parseEvent_timeThatIsNotADate_exceptionThrown() {
        assertThrows(BobException.class, () -> TaskParser.parseEvent("meeting /from soon /to 2026-12-03"));
    }
}
