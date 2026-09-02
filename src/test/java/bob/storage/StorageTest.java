package bob.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import bob.BobException;
import bob.task.Deadline;
import bob.task.Event;
import bob.task.Task;
import bob.task.TaskDate;
import bob.task.Todo;

/**
 * Tests {@link Storage#save} and {@link Storage#load}, which are the whole of
 * the chatbot's memory between one run and the next.
 *
 * <p>These are worth testing closely for two reasons. The first is that the two
 * have to agree exactly: a task saved in one run and misread in the next is lost
 * without anybody being told, and the escaping of bars and backslashes is where
 * that is most easily got wrong. The second is that {@code load} is the one part
 * of the chatbot that has to cope with a file somebody has edited by hand, so
 * every way a line can be damaged is a case it promises to survive.
 *
 * <p>Every test writes to a folder of its own, made and thrown away by JUnit, so
 * nothing here touches the real save file.
 */
public class StorageTest {

    /** A folder to keep each test's save file in, thrown away after the test. */
    @TempDir
    private Path tempDirectory;

    @Test
    public void load_noFileYet_emptyListAndNothingToSay() {
        Storage.LoadResult result = storageAt("duke.txt").load();

        // The ordinary first run is not a problem, so it is not reported as one.
        assertEquals(List.of(), result.tasks());
        assertEquals(List.of(), result.messages());
    }

    @Test
    public void load_emptyFile_emptyListAndNothingToSay() throws IOException {
        Storage storage = storageWithLines();

        Storage.LoadResult result = storage.load();

        assertEquals(List.of(), result.tasks());
        assertEquals(List.of(), result.messages());
    }

    @Test
    public void load_oneOfEachKindOfTask_allReadBackInOrder() throws IOException {
        Storage storage = storageWithLines(
                "T | 1 | read book",
                "D | 0 | return book | 2026-12-02",
                "E | 0 | project meeting | 2026-12-02 1800 | 2026-12-02 2000");

        Storage.LoadResult result = storage.load();

        assertEquals(List.of(), result.messages());
        assertEquals(3, result.tasks().size());
        assertEquals("[T][X] read book", result.tasks().get(0).toString());
        assertEquals("[D][ ] return book (by: Dec 02 2026)", result.tasks().get(1).toString());
        assertEquals("[E][ ] project meeting (from: Dec 02 2026 18:00 to: Dec 02 2026 20:00)",
                result.tasks().get(2).toString());
    }

    @Test
    public void load_blankLines_skippedWithoutComment() throws IOException {
        // A file opened in an editor easily picks one up; that is not damage.
        Storage storage = storageWithLines("", "T | 0 | read book", "   ", "");

        Storage.LoadResult result = storage.load();

        assertEquals(1, result.tasks().size());
        assertEquals(List.of(), result.messages());
    }

    @Test
    public void load_separatorsWithoutPadding_readTheSameWay() throws IOException {
        Storage storage = storageWithLines("T|0|read book");

        Storage.LoadResult result = storage.load();

        assertEquals("[T][ ] read book", result.tasks().get(0).toString());
    }

    @Test
    public void load_unknownKindOfTask_lineReportedAndSkipped() throws IOException {
        Storage storage = storageWithLines("T | 0 | read book", "X | 0 | who knows");

        Storage.LoadResult result = storage.load();

        // The good line is kept: one damaged line should not cost the user the rest.
        assertEquals(1, result.tasks().size());
        assertTrue(result.messages().get(0).contains("Line 2"));
        assertTrue(result.messages().get(0).contains("not a kind of task I know"));
    }

    @Test
    public void load_statusThatIsNeitherFlag_lineReportedAndSkipped() throws IOException {
        // A status that cannot be read is not quietly assumed to mean "not done".
        Storage storage = storageWithLines("T | maybe | read book");

        Storage.LoadResult result = storage.load();

        assertEquals(List.of(), result.tasks());
        assertTrue(result.messages().get(0).contains("doesn't say whether the task is done"));
    }

    @Test
    public void load_wrongNumberOfFields_lineReportedAndSkipped() throws IOException {
        Storage storage = storageWithLines(
                "T | 0",
                "T | 0 | read book | 2026-12-02",
                "D | 0 | return book");

        Storage.LoadResult result = storage.load();

        assertEquals(List.of(), result.tasks());
        assertTrue(result.messages().get(0).contains("fields"));
    }

    @Test
    public void load_emptyDescription_lineReportedAndSkipped() throws IOException {
        // A task with no description is one the user could never have added.
        Storage storage = storageWithLines("T | 0 | ");

        Storage.LoadResult result = storage.load();

        assertEquals(List.of(), result.tasks());
        assertTrue(result.messages().get(0).contains("description is empty"));
    }

    @Test
    public void load_dateThatIsNotADate_lineReportedAndSkipped() throws IOException {
        Storage storage = storageWithLines("D | 0 | return book | Sunday");

        Storage.LoadResult result = storage.load();

        assertEquals(List.of(), result.tasks());
        assertTrue(result.messages().get(0).contains("isn't a date"));
    }

    @Test
    public void load_oneBadLine_messagesWordedInTheSingular() throws IOException {
        Storage storage = storageWithLines("nonsense");

        List<String> messages = storage.load().messages();

        assertTrue(messages.contains("I've left that line out of your list."));
    }

    @Test
    public void load_severalBadLines_messagesWordedInThePlural() throws IOException {
        Storage storage = storageWithLines("nonsense", "more nonsense");

        List<String> messages = storage.load().messages();

        assertTrue(messages.contains("I've left those 2 lines out of your list."));
    }

    @Test
    public void load_moreBadLinesThanAreReported_theRestAreCounted() throws IOException {
        // Seven damaged lines: five are quoted, and the other two are counted, so
        // a thoroughly damaged file does not bury the greeting.
        Storage storage = storageWithLines("a", "b", "c", "d", "e", "f", "g");

        List<String> messages = storage.load().messages();

        assertEquals(5, messages.stream().filter(message -> message.startsWith("Line ")).count());
        assertTrue(messages.contains("...and 2 more lines I couldn't read."));
    }

    @Test
    public void load_fileThatCannotBeRead_emptyListAndAWarning() throws IOException {
        // A folder where the save file should be: it exists, and reading it fails.
        Files.createDirectory(tempDirectory.resolve("duke.txt"));

        Storage.LoadResult result = storageAt("duke.txt").load();

        assertEquals(List.of(), result.tasks());
        assertTrue(result.messages().get(0).contains("couldn't read"));
        // The user is warned before typing anything that would overwrite it.
        assertTrue(result.messages().get(1).contains("overwritten"));
    }

    @Test
    public void save_tasks_writtenOneToALineWithBarsBetweenFields() throws BobException, IOException {
        Storage storage = storageAt("duke.txt");
        Todo todo = new Todo("read book");
        todo.markAsDone();

        storage.save(List.of(todo, new Deadline("return book", TaskDate.parse("2026-12-02"))));

        assertEquals(List.of("T | 1 | read book", "D | 0 | return book | 2026-12-02"),
                Files.readAllLines(tempDirectory.resolve("duke.txt"), StandardCharsets.UTF_8));
    }

    @Test
    public void save_missingFolder_folderCreated() throws BobException {
        Storage storage = storageAt("data", "nested", "duke.txt");

        storage.save(List.of(new Todo("read book")));

        assertTrue(Files.exists(tempDirectory.resolve("data").resolve("nested").resolve("duke.txt")));
    }

    @Test
    public void save_shorterListThanBefore_oldLinesGone() throws BobException, IOException {
        Storage storage = storageAt("duke.txt");
        storage.save(List.of(new Todo("read book"), new Todo("return book")));

        storage.save(List.of(new Todo("read book")));

        // The whole file is rewritten, so it always says exactly what the list says.
        assertEquals(List.of("T | 0 | read book"),
                Files.readAllLines(tempDirectory.resolve("duke.txt"), StandardCharsets.UTF_8));
    }

    @Test
    public void save_emptyList_fileIsEmpty() throws BobException, IOException {
        Storage storage = storageAt("duke.txt");
        storage.save(List.of(new Todo("read book")));

        storage.save(List.of());

        assertEquals(List.of(),
                Files.readAllLines(tempDirectory.resolve("duke.txt"), StandardCharsets.UTF_8));
    }

    @Test
    public void saveThenLoad_descriptionHoldingASeparator_readBackUnchanged() throws BobException {
        assertSurvivesRoundTrip("tidy up | then rest");
    }

    @Test
    public void saveThenLoad_descriptionHoldingABackslash_readBackUnchanged() throws BobException {
        assertSurvivesRoundTrip("find C:\\notes");
    }

    @Test
    public void saveThenLoad_descriptionEndingInABackslash_readBackUnchanged() throws BobException {
        // The awkward one: the backslash is escaped before the bar is, so the
        // escape it adds is not itself escaped a moment later.
        assertSurvivesRoundTrip("read book\\");
    }

    @Test
    public void saveThenLoad_descriptionHoldingAnEscapedBar_readBackUnchanged() throws BobException {
        assertSurvivesRoundTrip("what \\| means");
    }

    @Test
    public void saveThenLoad_everyKindOfTask_readBackUnchanged() throws BobException {
        Storage storage = storageAt("duke.txt");
        Event event = new Event("project meeting",
                TaskDate.parse("2026-12-02 1800"), TaskDate.parse("2026-12-03 2000"));
        Deadline deadline = new Deadline("return book", TaskDate.parse("2026-12-02"));
        deadline.markAsDone();
        List<Task> saved = List.of(new Todo("read book"), deadline, event);

        storage.save(saved);
        List<Task> loaded = storage.load().tasks();

        assertEquals(saved.size(), loaded.size());
        for (int i = 0; i < saved.size(); i++) {
            assertEquals(saved.get(i).toString(), loaded.get(i).toString());
        }
    }

    /** Checks that a description survives being saved and read back unchanged. */
    private void assertSurvivesRoundTrip(String description) throws BobException {
        Storage storage = storageAt("duke.txt");

        storage.save(List.of(new Todo(description)));
        Storage.LoadResult result = storage.load();

        assertEquals(List.of(), result.messages());
        assertEquals(1, result.tasks().size());
        assertEquals("[T][ ] " + description, result.tasks().get(0).toString());
    }

    /** Returns storage backed by a file at {@code names} inside this test's folder. */
    private Storage storageAt(String... names) {
        Path path = tempDirectory;
        for (String name : names) {
            path = path.resolve(name);
        }
        return new Storage(path);
    }

    /** Returns storage backed by a save file already holding {@code lines}. */
    private Storage storageWithLines(String... lines) throws IOException {
        Path path = tempDirectory.resolve("duke.txt");
        Files.write(path, List.of(lines), StandardCharsets.UTF_8);
        return new Storage(path);
    }
}
