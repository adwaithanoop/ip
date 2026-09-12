package bob.parser;

import bob.BobException;
import bob.command.AddCommand;
import bob.command.AfterCommand;
import bob.command.BeforeCommand;
import bob.command.Command;
import bob.command.CommandWord;
import bob.command.DeleteCommand;
import bob.command.ExitCommand;
import bob.command.FindCommand;
import bob.command.ListCommand;
import bob.command.MarkCommand;
import bob.command.NextCommand;
import bob.command.OnCommand;

/**
 * Parses the lines the user types into the commands the chatbot acts on.
 *
 * <p>Every rule about how a command is <em>written</em> lives in this package, so
 * changing what the chatbot accepts is a change here and nowhere else. Carrying a
 * command out belongs to the {@link Command} classes, each handed what it needs,
 * such as a finished {@link bob.task.Task Task}, a number or a day.
 *
 * <p>This class recognizes the command a line begins with and builds it. Reading
 * what follows the command word is left to three helpers, one for each way the
 * rest of a command can be written:
 *
 * <ul>
 *   <li>{@link TaskParser}, for the description and dates of a new task;</li>
 *   <li>{@link EditParser}, for the changes an edit makes, in any order;</li>
 *   <li>{@link ArgumentParser}, for a single task number, day, count or keyword.</li>
 * </ul>
 *
 * <p>Every method is {@code static}. There is nothing for a parser to remember
 * between one line and the next — each line is understood on its own — so an
 * instance of this class would carry no state.
 */
public class Parser {

    /**
     * Returns the command one line of the user's asks for, ready to be run.
     *
     * <p>Which word is which command is {@link CommandWord}'s own business; what
     * this method adds is the two ways a line can fail to name one at all, each
     * with its own explanation rather than a shared "bad command".
     *
     * <p>The {@code switch} below is the one place that names every command in a
     * list. Something has to turn a word into an object, and doing it here means
     * it happens once.
     *
     * @param line one whole line as the user typed it, with surrounding spaces removed.
     * @return the command that line asks for.
     * @throws BobException if the line is empty, does not begin with a command the
     *                      chatbot knows, or is one it knows but cannot carry out
     *                      as written.
     */
    public static Command parse(String line) throws BobException {
        if (line.isEmpty()) {
            throw new BobException("You didn't type anything."
                    + "\nTell me about a task, or type " + CommandWord.LIST.getKeyword()
                    + " to see the ones I already have.");
        }
        // orElseThrow unwraps the Optional when a command was recognized, and
        // throws the "I don't know what that means" error when none was.
        CommandWord word = CommandWord.parse(line).orElseThrow(() -> createUnknownCommandError(line));
        String arguments = word.getArgumentsIn(line);
        return switch (word) {
            case TODO -> new AddCommand(TaskParser.parseTodo(arguments));
            case DEADLINE -> new AddCommand(TaskParser.parseDeadline(arguments));
            case EVENT -> new AddCommand(TaskParser.parseEvent(arguments));
            case LIST -> new ListCommand();
            case ON -> new OnCommand(ArgumentParser.parseDay(arguments, word));
            case BEFORE -> new BeforeCommand(ArgumentParser.parseDay(arguments, word));
            case AFTER -> new AfterCommand(ArgumentParser.parseDay(arguments, word));
            case NEXT -> new NextCommand(ArgumentParser.parseCount(arguments));
            case FIND -> new FindCommand(ArgumentParser.parseKeyword(arguments));
            case MARK -> new MarkCommand(ArgumentParser.parseTaskNumber(arguments, word), true);
            case UNMARK -> new MarkCommand(ArgumentParser.parseTaskNumber(arguments, word), false);
            case DELETE -> new DeleteCommand(ArgumentParser.parseTaskNumber(arguments, word));
            case EDIT -> EditParser.parseEdit(arguments);
            case BYE -> new ExitCommand();
        };
    }

    /**
     * Returns the error to throw for a line that is not one of the commands the
     * chatbot knows, listing the ones it does know so the user can pick one.
     */
    private static BobException createUnknownCommandError(String line) {
        return new BobException("Sorry, I don't know what \"" + line + "\" means."
                + "\nTry one of: " + CommandWord.getAllKeywords());
    }
}
