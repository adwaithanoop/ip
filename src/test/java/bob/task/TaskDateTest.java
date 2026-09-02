package bob.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import bob.BobException;

/**
 * Tests {@link TaskDate#parse}, the one way text typed by the user becomes a
 * date the chatbot can reason about.
 *
 * <p>It is tested rather than the other methods because it is where the chatbot
 * decides what it will and will not accept: everything downstream — listing by
 * day, sorting, saving — assumes a {@code TaskDate} only exists for text that
 * was understood, and this is the method that has to make that true.
 *
 * <p>{@code TaskDate} has no {@code equals}, so a parsed date is checked through
 * what it produces: {@link TaskDate#toSaveField()} for the form written to the
 * save file, and {@link TaskDate#toString()} for the form shown to the user.
 */
public class TaskDateTest {

    @Test
    public void parse_dayOnly_dayStoredWithoutTime() throws BobException {
        TaskDate date = TaskDate.parse("2026-12-02");

        assertEquals("2026-12-02", date.toSaveField());
        assertEquals("Dec 02 2026", date.toString());
    }

    @Test
    public void parse_dayAndTime_bothStored() throws BobException {
        TaskDate date = TaskDate.parse("2026-12-02 1800");

        assertEquals("2026-12-02 1800", date.toSaveField());
        assertEquals("Dec 02 2026 18:00", date.toString());
    }

    @Test
    public void parse_surroundingAndRepeatedWhitespace_ignored() throws BobException {
        TaskDate date = TaskDate.parse("  2026-12-02   1800  ");

        assertEquals("2026-12-02 1800", date.toSaveField());
    }

    @Test
    public void parse_midnight_keptAsATime() throws BobException {
        TaskDate withMidnight = TaskDate.parse("2026-12-02 0000");
        TaskDate withoutTime = TaskDate.parse("2026-12-02");

        // A task due "on the 2nd" and one due "at midnight on the 2nd" are
        // different things, so the second must not lose its time.
        assertEquals("2026-12-02 0000", withMidnight.toSaveField());
        assertNotEquals(withoutTime.toString(), withMidnight.toString());
    }

    @Test
    public void parse_boundaryTimes_accepted() throws BobException {
        assertEquals("Dec 02 2026 23:59", TaskDate.parse("2026-12-02 2359").toString());
        assertEquals("Dec 02 2026 00:01", TaskDate.parse("2026-12-02 0001").toString());
    }

    @Test
    public void parse_leapDayInLeapYear_accepted() throws BobException {
        assertEquals("Feb 29 2028", TaskDate.parse("2028-02-29").toString());
    }

    @Test
    public void parse_dayThatDoesNotExist_exceptionThrown() {
        // The 30th of February never happens, and neither does the 29th in a
        // year that is not a leap year.
        assertThrows(BobException.class, () -> TaskDate.parse("2026-02-30"));
        assertThrows(BobException.class, () -> TaskDate.parse("2026-02-29"));
        assertThrows(BobException.class, () -> TaskDate.parse("2026-13-01"));
    }

    @Test
    public void parse_dayInAnotherFormat_exceptionThrown() {
        // Only yyyy-mm-dd is accepted, so that 2/12/2026 does not have to be
        // guessed at as either the 2nd of December or the 12th of February.
        assertThrows(BobException.class, () -> TaskDate.parse("02/12/2026"));
        assertThrows(BobException.class, () -> TaskDate.parse("2026-2-2"));
        assertThrows(BobException.class, () -> TaskDate.parse("Dec 02 2026"));
    }

    @Test
    public void parse_timeInAnotherFormat_exceptionThrown() {
        assertThrows(BobException.class, () -> TaskDate.parse("2026-12-02 18:00"));
        assertThrows(BobException.class, () -> TaskDate.parse("2026-12-02 6pm"));
        assertThrows(BobException.class, () -> TaskDate.parse("2026-12-02 800"));
    }

    @Test
    public void parse_timeThatDoesNotExist_exceptionThrown() {
        assertThrows(BobException.class, () -> TaskDate.parse("2026-12-02 2500"));
        assertThrows(BobException.class, () -> TaskDate.parse("2026-12-02 1860"));
    }

    @Test
    public void parse_moreThanTwoParts_exceptionThrown() {
        assertThrows(BobException.class, () -> TaskDate.parse("2026-12-02 1800 1900"));
    }

    @Test
    public void parse_emptyOrBlankText_exceptionThrown() {
        assertThrows(BobException.class, () -> TaskDate.parse(""));
        assertThrows(BobException.class, () -> TaskDate.parse("   "));
    }

    @Test
    public void parse_textThatIsNotADate_exceptionThrown() {
        assertThrows(BobException.class, () -> TaskDate.parse("no idea :-p"));
        assertThrows(BobException.class, () -> TaskDate.parse("Sunday"));
    }

    @Test
    public void parse_unreadableText_messageQuotesTheText() {
        BobException exception = assertThrows(BobException.class, () -> TaskDate.parse("Sunday"));

        // The offending text is quoted back so that a user who mistyped one of
        // several dates on a line can see which one was not understood.
        assertTrue(exception.getMessage().contains("\"Sunday\""));
    }

    @Test
    public void parse_dayOnly_comparesAgainstTheRightDay() throws BobException {
        TaskDate date = TaskDate.parse("2026-12-02");

        assertTrue(date.isOn(LocalDate.of(2026, 12, 2)));
        assertTrue(date.isBefore(LocalDate.of(2026, 12, 3)));
        assertTrue(date.isAfter(LocalDate.of(2026, 12, 1)));
    }

    @Test
    public void parseDay_dayInTheAcceptedForm_dayReturned() throws BobException {
        assertEquals(LocalDate.of(2026, 12, 2), TaskDate.parseDay("2026-12-02"));
        assertEquals(LocalDate.of(2026, 12, 2), TaskDate.parseDay("  2026-12-02  "));
    }

    @Test
    public void parseDay_timeAfterTheDay_exceptionThrown() {
        // Refused rather than quietly treated as the whole day, since a user who
        // typed a time is asking something this chatbot does not answer.
        assertThrows(BobException.class, () -> TaskDate.parseDay("2026-12-02 1800"));
    }

    @Test
    public void parseDay_textThatIsNotADay_exceptionThrown() {
        assertThrows(BobException.class, () -> TaskDate.parseDay("Sunday"));
        assertThrows(BobException.class, () -> TaskDate.parseDay("02/12/2026"));
        assertThrows(BobException.class, () -> TaskDate.parseDay("2026-02-30"));
        assertThrows(BobException.class, () -> TaskDate.parseDay(""));
    }

    @Test
    public void formatDay_anyDay_writtenTheWayDatesAreShownBack() {
        assertEquals("Dec 02 2026", TaskDate.formatDay(LocalDate.of(2026, 12, 2)));
        // Single-figure days are padded, so a column of dates lines up.
        assertEquals("Jan 05 2026", TaskDate.formatDay(LocalDate.of(2026, 1, 5)));
    }

    @Test
    public void compareTo_differentDays_earlierDayFirst() throws BobException {
        TaskDate earlier = TaskDate.parse("2026-12-02");
        TaskDate later = TaskDate.parse("2026-12-03");

        assertTrue(earlier.compareTo(later) < 0);
        assertTrue(later.compareTo(earlier) > 0);
    }

    @Test
    public void compareTo_sameDayDifferentTimes_earlierTimeFirst() throws BobException {
        TaskDate morning = TaskDate.parse("2026-12-02 0900");
        TaskDate evening = TaskDate.parse("2026-12-02 1800");

        assertTrue(morning.compareTo(evening) < 0);
        assertTrue(evening.compareTo(morning) > 0);
    }

    @Test
    public void compareTo_sameDayOneWithoutATime_theOneWithoutATimeFirst() throws BobException {
        TaskDate wholeDay = TaskDate.parse("2026-12-02");
        TaskDate atNine = TaskDate.parse("2026-12-02 0900");

        // A date given without a time counts as the start of its day. That is an
        // ordering rule, not a claim that the task is due at midnight.
        assertTrue(wholeDay.compareTo(atNine) < 0);
    }

    @Test
    public void compareTo_samePointInTime_zero() throws BobException {
        assertEquals(0, TaskDate.parse("2026-12-02").compareTo(TaskDate.parse("2026-12-02")));
        assertEquals(0, TaskDate.parse("2026-12-02 1800")
                .compareTo(TaskDate.parse("2026-12-02 1800")));
        // A day with no time and the same day at midnight sort together.
        assertEquals(0, TaskDate.parse("2026-12-02").compareTo(TaskDate.parse("2026-12-02 0000")));
    }

    @Test
    public void isOnBeforeAfter_dateWithATime_answeredByItsDayAlone() throws BobException {
        TaskDate date = TaskDate.parse("2026-12-02 1800");

        // The time of day never decides which day a task belongs to.
        assertTrue(date.isOn(LocalDate.of(2026, 12, 2)));
        assertFalse(date.isBefore(LocalDate.of(2026, 12, 2)));
        assertFalse(date.isAfter(LocalDate.of(2026, 12, 2)));
        assertTrue(date.isBefore(LocalDate.of(2026, 12, 3)));
        assertTrue(date.isAfter(LocalDate.of(2026, 12, 1)));
    }
}
