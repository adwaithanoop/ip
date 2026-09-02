package bob.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import bob.BobException;

/**
 * Tests {@link Event}, and above all {@link Event#occursOn}.
 *
 * <p>An event is the one kind of task that covers a stretch of time rather than
 * a point in it, and {@code occursOn} is where that difference is written down:
 * it is widened from the inherited version so that asking what is on the
 * Wednesday of a week-long orientation finds it. That widening is easy to get
 * wrong at the ends of the stretch, so the day it starts, the day it ends, and
 * the days on either side are all checked.
 *
 * <p>The other date questions are deliberately <em>not</em> widened: an event is
 * placed by its start for ordering and for "before" and "after". The test at the
 * end pins that down, since it looks like an oversight until it is stated.
 */
public class EventTest {

    @Test
    public void getTypeIcon_anyEvent_returnsE() throws BobException {
        assertEquals("E", eventFrom("2026-12-01", "2026-12-05").getTypeIcon());
        assertEquals(Event.TYPE_ICON, eventFrom("2026-12-01", "2026-12-05").getTypeIcon());
    }

    @Test
    public void occursOn_dayInTheMiddleOfTheEvent_true() throws BobException {
        Event event = eventFrom("2026-12-01", "2026-12-05");

        assertTrue(event.occursOn(LocalDate.of(2026, 12, 3)));
    }

    @Test
    public void occursOn_firstAndLastDayOfTheEvent_true() throws BobException {
        Event event = eventFrom("2026-12-01", "2026-12-05");

        // Both ends count as days the event is running.
        assertTrue(event.occursOn(LocalDate.of(2026, 12, 1)));
        assertTrue(event.occursOn(LocalDate.of(2026, 12, 5)));
    }

    @Test
    public void occursOn_dayJustOutsideTheEvent_false() throws BobException {
        Event event = eventFrom("2026-12-01", "2026-12-05");

        assertFalse(event.occursOn(LocalDate.of(2026, 11, 30)));
        assertFalse(event.occursOn(LocalDate.of(2026, 12, 6)));
    }

    @Test
    public void occursOn_eventWithinOneDay_thatDayOnly() throws BobException {
        Event event = eventFrom("2026-12-02 1800", "2026-12-02 2000");

        assertTrue(event.occursOn(LocalDate.of(2026, 12, 2)));
        assertFalse(event.occursOn(LocalDate.of(2026, 12, 1)));
        assertFalse(event.occursOn(LocalDate.of(2026, 12, 3)));
    }

    @Test
    public void occursOn_timesOfDayAtTheEnds_ignored() throws BobException {
        // The question is about a day, so an event that ends at nine in the
        // morning is still running on the day it ends.
        Event event = eventFrom("2026-12-01 2100", "2026-12-05 0900");

        assertTrue(event.occursOn(LocalDate.of(2026, 12, 1)));
        assertTrue(event.occursOn(LocalDate.of(2026, 12, 5)));
    }

    @Test
    public void getScheduledDate_anyEvent_returnsTheStart() throws BobException {
        Event event = eventFrom("2026-12-01 1400", "2026-12-05 1600");

        // The start is when the event first wants the user's attention.
        assertEquals("2026-12-01 1400", event.getScheduledDate().orElseThrow().toSaveField());
    }

    @Test
    public void dateChecks_eventAlreadyRunning_measuredFromItsStart() throws BobException {
        Event event = eventFrom("2026-12-01", "2026-12-05");
        LocalDate day = LocalDate.of(2026, 12, 3);

        // An event already running is not still to come, so it is not "after"
        // today; occursOn is the question that finds it.
        assertFalse(event.isAfter(day));
        assertTrue(event.isBefore(day));
        assertTrue(event.occursOn(day));
    }

    @Test
    public void dateChecks_eventStillToCome_isAfter() throws BobException {
        Event event = eventFrom("2026-12-10", "2026-12-12");

        assertTrue(event.isAfter(LocalDate.of(2026, 12, 3)));
        assertFalse(event.isBefore(LocalDate.of(2026, 12, 3)));
    }

    @Test
    public void toString_newEvent_showsBothEndsInTheFriendlyForm() throws BobException {
        Event event = new Event("project meeting",
                TaskDate.parse("2026-12-02 1800"), TaskDate.parse("2026-12-02 2000"));

        assertEquals("[E][ ] project meeting (from: Dec 02 2026 18:00 to: Dec 02 2026 20:00)",
                event.toString());
    }

    @Test
    public void toString_doneEvent_showsCrossInStatusBox() throws BobException {
        Event event = eventFrom("2026-12-01", "2026-12-05");
        event.markAsDone();

        assertEquals("[E][X] orientation (from: Dec 01 2026 to: Dec 05 2026)", event.toString());
    }

    @Test
    public void toSaveFields_anyEvent_addsStartAndEndAsTwoFields() throws BobException {
        Event event = new Event("project meeting",
                TaskDate.parse("2026-12-02 1800"), TaskDate.parse("2026-12-02 2000"));

        // Two fields rather than one, so reading them back is a matter of taking
        // two fields apart rather than splitting one down the middle.
        assertEquals(List.of("E", Task.NOT_DONE_FLAG, "project meeting",
                "2026-12-02 1800", "2026-12-02 2000"), event.toSaveFields());
    }

    @Test
    public void toSaveFields_doneEvent_returnsDoneFlag() throws BobException {
        Event event = eventFrom("2026-12-01", "2026-12-05");
        event.markAsDone();

        assertEquals(Task.DONE_FLAG, event.toSaveFields().get(1));
    }

    /** Returns an event called {@code orientation} running between two dates. */
    private static Event eventFrom(String from, String to) throws BobException {
        return new Event("orientation", TaskDate.parse(from), TaskDate.parse(to));
    }
}
