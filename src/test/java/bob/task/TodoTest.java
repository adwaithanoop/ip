package bob.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link Todo}: the letter it answers with, and the behavior it takes
 * unchanged from {@link Task}.
 *
 * <p>{@code Todo} declares little of its own, so most of what is checked here is
 * inherited. It is still worth checking through a {@code Todo} rather than
 * skipped as "someone else's code": a todo is the simplest concrete task, so it
 * is the cheapest place to pin down the parts every task shares — how a task is
 * shown, what it saves, and how marking it done changes both. It is also the
 * one kind of task that has no date, and the promise that a dateless task is
 * left out of the date commands rather than counted as due at the beginning of
 * time is a promise {@code Todo} is making.
 */
public class TodoTest {

    /** A day used wherever a test needs some day to ask about. */
    private static final LocalDate SOME_DAY = LocalDate.of(2026, 12, 2);

    @Test
    public void getTypeIcon_anyTodo_returnsT() {
        Todo todo = new Todo("read book");

        assertEquals("T", todo.getTypeIcon());
        // Storage recognizes a saved todo by this same constant, so the two
        // must not be allowed to drift apart.
        assertEquals(Todo.TYPE_ICON, todo.getTypeIcon());
    }

    @Test
    public void getStatusIcon_newTodo_returnsBlank() {
        // A task the user has just mentioned is something still to do.
        assertEquals(" ", new Todo("read book").getStatusIcon());
    }

    @Test
    public void getStatusIcon_markedAsDone_returnsX() {
        Todo todo = new Todo("read book");
        todo.markAsDone();

        assertEquals("X", todo.getStatusIcon());
    }

    @Test
    public void getStatusIcon_markedAsNotDoneAgain_returnsBlank() {
        Todo todo = new Todo("read book");
        todo.markAsDone();
        todo.markAsNotDone();

        assertEquals(" ", todo.getStatusIcon());
    }

    @Test
    public void markAsDone_calledTwice_staysDone() {
        Todo todo = new Todo("read book");
        todo.markAsDone();
        todo.markAsDone();

        assertEquals("X", todo.getStatusIcon());
    }

    @Test
    public void toString_newTodo_showsTypeStatusAndDescription() {
        assertEquals("[T][ ] read book", new Todo("read book").toString());
    }

    @Test
    public void toString_doneTodo_showsCrossInStatusBox() {
        Todo todo = new Todo("read book");
        todo.markAsDone();

        assertEquals("[T][X] read book", todo.toString());
    }

    @Test
    public void toString_todo_addsNothingAfterTheDescription() {
        // Unlike a deadline or an event, a todo has nothing more to show, so
        // nothing should be appended to the shared form.
        assertEquals("[T][ ] borrow book", new Todo("borrow book").toString());
    }

    @Test
    public void toSaveFields_newTodo_returnsTypeNotDoneFlagAndDescription() {
        List<String> fields = new Todo("read book").toSaveFields();

        assertEquals(List.of(Todo.TYPE_ICON, Task.NOT_DONE_FLAG, "read book"), fields);
    }

    @Test
    public void toSaveFields_doneTodo_returnsDoneFlag() {
        Todo todo = new Todo("read book");
        todo.markAsDone();

        assertEquals(List.of(Todo.TYPE_ICON, Task.DONE_FLAG, "read book"), todo.toSaveFields());
    }

    @Test
    public void toSaveFields_descriptionWithSeparator_keptWhole() {
        // Splitting a description that contains the save file's separator is
        // Storage's problem; the task hands the text over untouched.
        List<String> fields = new Todo("read book | volume 2").toSaveFields();

        assertEquals("read book | volume 2", fields.get(2));
    }

    @Test
    public void getScheduledDate_anyTodo_returnsEmpty() {
        assertTrue(new Todo("read book").getScheduledDate().isEmpty());
    }

    @Test
    public void dateChecks_anyTodo_allReturnFalse() {
        Todo todo = new Todo("read book");

        // A todo is pinned to no day, so it belongs in no day's listing, and is
        // neither overdue nor still to come.
        assertFalse(todo.occursOn(SOME_DAY));
        assertFalse(todo.isBefore(SOME_DAY));
        assertFalse(todo.isAfter(SOME_DAY));
    }

    @Test
    public void matchesKeyword_wordInTheDescription_returnsTrue() {
        Todo todo = new Todo("read book");

        assertTrue(todo.matchesKeyword("read"));
        assertTrue(todo.matchesKeyword("book"));
    }

    @Test
    public void matchesKeyword_wordNotInTheDescription_returnsFalse() {
        assertFalse(new Todo("read book").matchesKeyword("swim"));
    }

    @Test
    public void matchesKeyword_differentCase_returnsTrue() {
        Todo todo = new Todo("Read Book");

        // Neither side's capitalization is the user's problem when searching.
        assertTrue(todo.matchesKeyword("read book"));
        assertTrue(new Todo("read book").matchesKeyword("BOOK"));
    }

    @Test
    public void matchesKeyword_partOfAWord_returnsTrue() {
        // Half a word is a search someone means, so the keyword is looked for
        // anywhere in the description rather than as a whole word.
        assertTrue(new Todo("visit the bookshop").matchesKeyword("book"));
        assertTrue(new Todo("read book").matchesKeyword("ad bo"));
    }

    @Test
    public void matchesKeyword_todoFoundUnlikeInTheDateListings_returnsTrue() {
        Todo todo = new Todo("read book");

        // A todo has no date and so is left out of on, before and after; it has
        // a description like any other task, so find must not leave it out too.
        assertFalse(todo.occursOn(SOME_DAY));
        assertTrue(todo.matchesKeyword("read"));
    }
}
