package bob;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * A point in time attached to a task: the day it falls on, and the time of day
 * when the user gave one.
 *
 * <p>{@link Deadline} and {@link Event} used to keep whatever the user typed as
 * plain text, so {@code 2026-12-02}, {@code Sunday} and {@code no idea :-p} were
 * all equally acceptable and all equally meaningless to the chatbot. Holding a
 * real {@link LocalDate} instead means the date has been understood rather than
 * merely copied: it can be shown back in a friendlier form than it was typed in,
 * and a later increment can compare two of them to answer questions such as
 * which task is due first — neither of which can be done with a string.
 *
 * <p>The price is that text which is not a date can no longer be accepted, so
 * {@code no idea :-p} is now refused with an explanation instead of being stored.
 * Keeping both — a date when the text parses as one, free text when it does not —
 * is possible, but it means every part of the program that reads a date has to
 * cope with two kinds of answer, which is a lot of complication to buy back a
 * habit the requirements are asking to drop.
 *
 * <p>This class is immutable: both fields are {@code final} and neither
 * {@link LocalDate} nor {@link LocalTime} can be changed after it is made. A
 * {@code TaskDate} can therefore be passed around freely, with no risk of one
 * holder of it seeing another holder's change.
 */
public class TaskDate {

    /** A day on its own, written the way the user types it. */
    public static final String EXAMPLE_DATE = "2026-12-02";

    /** A day and a time of day, written the way the user types them. */
    public static final String EXAMPLE_DATE_TIME = "2026-12-02 1800";

    /**
     * How the time of day is typed, and how it is written to the save file:
     * four digits on a 24-hour clock, so {@code 1800} is six in the evening.
     */
    private static final DateTimeFormatter INPUT_TIME_FORMAT = DateTimeFormatter.ofPattern("HHmm");

    /**
     * How the day is shown back to the user, for example {@code Dec 02 2026}.
     *
     * <p>The language is stated here rather than left to the computer the chatbot
     * runs on, so that the month is always named in English instead of changing
     * with that computer's regional settings.
     */
    private static final DateTimeFormatter OUTPUT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.ENGLISH);

    /**
     * How the time of day is shown back to the user, for example {@code 18:00}.
     *
     * <p>Shown on a 24-hour clock, as it is typed. A 12-hour {@code 6:00 pm} would
     * read more naturally, but it would also mean choosing how to spell "pm" for
     * every language the chatbot might be run in.
     */
    private static final DateTimeFormatter OUTPUT_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    /** The day this date falls on. */
    private final LocalDate date;

    /**
     * The time of day, or {@code null} when the user gave a day and no time.
     *
     * <p>A missing time is left as {@code null} rather than filled in with
     * midnight, because a task due "on the 2nd" and a task due "at midnight on
     * the 2nd" are different things, and only the second one should be shown
     * with a time. {@link java.util.Optional} would say the same thing without
     * {@code null}, but it is meant for return values rather than for fields, so
     * the simpler field is used and the meaning is written down here instead.
     */
    private final LocalTime time;

    /**
     * Creates a date from parts that have already been checked.
     *
     * <p>Private, so that {@link #parse} is the only way in: every
     * {@code TaskDate} that exists has therefore come from text this class was
     * able to understand.
     *
     * @param date the day this falls on.
     * @param time the time of day, or {@code null} if there is none.
     */
    private TaskDate(LocalDate date, LocalTime time) {
        this.date = date;
        this.time = time;
    }

    /**
     * Returns the date written in {@code text}: a day as {@code yyyy-mm-dd},
     * optionally followed by a time of day as four digits on a 24-hour clock —
     * for example {@value #EXAMPLE_DATE} or {@value #EXAMPLE_DATE_TIME}.
     *
     * <p>One way of writing a date is accepted rather than several. Accepting
     * {@code 2/12/2026} as well would mean deciding whether that is the 2nd of
     * December or the 12th of February, and either answer is wrong for half the
     * world; asking for the year first leaves nothing to guess.
     *
     * @param text what the user typed after {@code /by}, {@code /from} or {@code /to}.
     * @return the date that text names.
     * @throws BobException if the text is not a day, or a day and a time, in that
     *                      form — including a day that does not exist, such as
     *                      {@code 2026-02-30}.
     */
    public static TaskDate parse(String text) throws BobException {
        String[] parts = text.trim().split("\\s+");
        if (parts.length > 2) {
            throw cannotRead(text);
        }
        try {
            // LocalDate reads the yyyy-mm-dd form by itself, and refuses a day
            // that never happened, such as the 30th of February.
            LocalDate date = LocalDate.parse(parts[0]);
            LocalTime time = (parts.length == 2)
                    ? LocalTime.parse(parts[1], INPUT_TIME_FORMAT)
                    : null;
            return new TaskDate(date, time);
        } catch (DateTimeParseException e) {
            throw cannotRead(text);
        }
    }

    /**
     * Returns the error to report for text that is not a date this chatbot can read.
     *
     * <p>The text is quoted back so that a user who mistyped one character can see
     * which of several dates on the line was not understood.
     */
    private static BobException cannotRead(String text) {
        return new BobException("I don't understand \"" + text + "\" as a date."
                + "\nWrite the day as yyyy-mm-dd, and add a 24-hour time if the hour matters."
                + "\nFor example: " + EXAMPLE_DATE + " or " + EXAMPLE_DATE_TIME);
    }

    /**
     * Returns this date as the single field {@link Storage} writes to the save file.
     *
     * <p>It is saved in the same form the user types it, rather than in the
     * friendlier form {@link #toString()} prints. The saved text is read back by
     * {@link #parse}, so writing it this way means one format has to be read, and
     * a user editing the file by hand writes dates there exactly as they would
     * type them at the chatbot.
     */
    public String toSaveField() {
        if (time == null) {
            return date.toString();
        }
        return date + " " + time.format(INPUT_TIME_FORMAT);
    }

    /** Returns for example {@code Dec 02 2026}, or {@code Dec 02 2026 18:00} with a time. */
    @Override
    public String toString() {
        String day = date.format(OUTPUT_DATE_FORMAT);
        if (time == null) {
            return day;
        }
        return day + " " + time.format(OUTPUT_TIME_FORMAT);
    }
}
