package bob.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import bob.BobException;

/**
 * Tests {@link TaskList}, the list the whole chatbot is about.
 *
 * <p>Most of it is a thin cover over an {@link ArrayList}, and the tests for
 * those parts are short. What is worth real attention is the part that is not
 * thin: {@link TaskList#findIndexes} and {@link TaskList#findIndexesSoonestFirst}
 * return <em>positions</em> rather than tasks, because the number the user types
 * into {@code mark} or {@code delete} is a position in the whole list. A
 * shortened listing that renumbered its matches would hand the user numbers that
 * do the wrong thing, so the tests below check the positions that come back and
 * that the underlying list is left in the order the user added things.
 */
public class TaskListTest {

    @Test
    public void constructor_noArguments_emptyList() {
        TaskList tasks = new TaskList();

        assertEquals(0, tasks.size());
        assertTrue(tasks.isEmpty());
    }

    @Test
    public void constructor_givenTasks_tasksCopiedNotShared() throws BobException {
        List<Task> given = new ArrayList<>(List.of(new Todo("read book")));
        TaskList tasks = new TaskList(given);

        given.add(new Todo("return book"));

        // The list was copied, so whoever passed it in cannot change this one
        // behind its back.
        assertEquals(1, tasks.size());
    }

    @Test
    public void add_task_appendedAtTheEnd() {
        TaskList tasks = new TaskList();

        tasks.add(new Todo("read book"));
        tasks.add(new Todo("return book"));

        // Adding at the end leaves the numbers already shown to the user alone.
        assertEquals("[T][ ] read book", tasks.get(0).toString());
        assertEquals("[T][ ] return book", tasks.get(1).toString());
        assertFalse(tasks.isEmpty());
    }

    @Test
    public void delete_middleTask_returnedAndLaterTasksMoveUp() {
        TaskList tasks = listOf(new Todo("first"), new Todo("second"), new Todo("third"));

        Task removed = tasks.delete(1);

        assertEquals("[T][ ] second", removed.toString());
        assertEquals(2, tasks.size());
        // The numbers stay a run of 1, 2, 3 with nothing missing.
        assertEquals("[T][ ] third", tasks.get(1).toString());
    }

    @Test
    public void asList_taskList_returnsAnUnchangeableCopy() {
        TaskList tasks = listOf(new Todo("read book"));

        List<Task> copy = tasks.asList();

        assertEquals(1, copy.size());
        // Saving the list must not be able to alter what is being saved.
        assertThrows(UnsupportedOperationException.class, () -> copy.add(new Todo("sneaky")));
    }

    @Test
    public void asList_afterTheListChanges_earlierCopyUnaffected() {
        TaskList tasks = listOf(new Todo("read book"));
        List<Task> copy = tasks.asList();

        tasks.add(new Todo("return book"));

        assertEquals(1, copy.size());
    }

    @Test
    public void findIndexes_someTasksMatch_positionsInTheWholeList() throws BobException {
        TaskList tasks = listOf(
                new Todo("read book"),
                deadlineOn("return book", "2026-12-02"),
                new Todo("water plants"),
                deadlineOn("pay bill", "2026-12-05"));

        List<Integer> found = tasks.findIndexes(task -> task.getScheduledDate().isPresent());

        // Positions 1 and 3, not 0 and 1: these are the numbers the user types.
        assertEquals(List.of(1, 3), found);
    }

    @Test
    public void findIndexes_nothingMatches_emptyList() {
        TaskList tasks = listOf(new Todo("read book"));

        assertEquals(List.of(), tasks.findIndexes(task -> false));
    }

    @Test
    public void findIndexes_emptyList_emptyList() {
        assertEquals(List.of(), new TaskList().findIndexes(task -> true));
    }

    @Test
    public void allIndexes_severalTasks_everyPositionInOrder() {
        TaskList tasks = listOf(new Todo("first"), new Todo("second"), new Todo("third"));

        assertEquals(List.of(0, 1, 2), tasks.allIndexes());
    }

    @Test
    public void allIndexes_emptyList_emptyList() {
        assertEquals(List.of(), new TaskList().allIndexes());
    }

    @Test
    public void findIndexesSoonestFirst_mixedTasks_datedOnesInDateOrder() throws BobException {
        TaskList tasks = listOf(
                new Todo("read book"),
                deadlineOn("pay bill", "2026-12-05"),
                eventFrom("orientation", "2026-12-01", "2026-12-07"),
                deadlineOn("return book", "2026-12-03"));

        List<Integer> found = tasks.findIndexesSoonestFirst();

        // The todo is left out — there is no answer to where an undated task
        // belongs among dated ones — and an event is placed by its start.
        assertEquals(List.of(2, 3, 1), found);
    }

    @Test
    public void findIndexesSoonestFirst_sameDayDifferentTimes_earlierTimeFirst() throws BobException {
        TaskList tasks = listOf(
                deadlineOn("evening", "2026-12-02 1800"),
                deadlineOn("morning", "2026-12-02 0900"));

        assertEquals(List.of(1, 0), tasks.findIndexesSoonestFirst());
    }

    @Test
    public void findIndexesSoonestFirst_sameDayOneWithoutATime_undatedTimeFirst() throws BobException {
        TaskList tasks = listOf(
                deadlineOn("at six", "2026-12-02 1800"),
                deadlineOn("that day", "2026-12-02"));

        // A date given without a time counts as the start of its day, which is
        // the cautious way round for someone deciding what to do next.
        assertEquals(List.of(1, 0), tasks.findIndexesSoonestFirst());
    }

    @Test
    public void findIndexesSoonestFirst_onlyTodos_emptyList() {
        TaskList tasks = listOf(new Todo("read book"), new Todo("return book"));

        assertEquals(List.of(), tasks.findIndexesSoonestFirst());
    }

    @Test
    public void findIndexesSoonestFirst_emptyList_emptyList() {
        assertEquals(List.of(), new TaskList().findIndexesSoonestFirst());
    }

    @Test
    public void findIndexesSoonestFirst_calledOnAList_doesNotReorderTheListItself() throws BobException {
        TaskList tasks = listOf(
                deadlineOn("later", "2026-12-05"),
                deadlineOn("sooner", "2026-12-01"));

        tasks.findIndexesSoonestFirst();

        // Sorting the positions rather than the tasks is what keeps the user's
        // own numbering from being renumbered as a side effect.
        assertEquals("[D][ ] later (by: Dec 05 2026)", tasks.get(0).toString());
        assertEquals("[D][ ] sooner (by: Dec 01 2026)", tasks.get(1).toString());
    }

    @Test
    public void findIndexes_dayTests_usedByTheDateCommands() throws BobException {
        LocalDate day = LocalDate.of(2026, 12, 3);
        TaskList tasks = listOf(
                new Todo("read book"),
                deadlineOn("on the day", "2026-12-03"),
                deadlineOn("before it", "2026-12-01"),
                eventFrom("running through it", "2026-12-01", "2026-12-05"));

        assertEquals(List.of(1, 3), tasks.findIndexes(task -> task.occursOn(day)));
        assertEquals(List.of(2, 3), tasks.findIndexes(task -> task.isBefore(day)));
        assertEquals(List.of(), tasks.findIndexes(task -> task.isAfter(day)));
    }

    @Test
    public void findIndexes_keywordTest_usedByTheFindCommand() throws BobException {
        TaskList tasks = listOf(
                new Todo("read book"),
                deadlineOn("return book", "2026-12-02"),
                new Todo("join sports club"),
                eventFrom("book club", "2026-12-01", "2026-12-01"));

        // Unlike the day tests above, this one finds todos too: every task has a
        // description, so no kind of task is left out of a search.
        assertEquals(List.of(0, 1, 3), tasks.findIndexes(task -> task.matchesKeyword("book")));
        assertEquals(List.of(2, 3), tasks.findIndexes(task -> task.matchesKeyword("club")));
        assertEquals(List.of(), tasks.findIndexes(task -> task.matchesKeyword("swim")));
    }

    /** Returns a task list holding the given tasks, in the order given. */
    private static TaskList listOf(Task... tasks) {
        return new TaskList(List.of(tasks));
    }

    /** Returns a deadline due at {@code date}, written as the user would type it. */
    private static Deadline deadlineOn(String description, String date) throws BobException {
        return new Deadline(description, TaskDate.parse(date));
    }

    /** Returns an event running from {@code from} to {@code to}. */
    private static Event eventFrom(String description, String from, String to) throws BobException {
        return new Event(description, TaskDate.parse(from), TaskDate.parse(to));
    }
}
