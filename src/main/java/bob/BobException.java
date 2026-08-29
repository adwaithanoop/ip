package bob;

/**
 * An error in what the user asked the chatbot to do, described in words that
 * can be shown to the user as-is.
 *
 * <p>Using an exception lets the code that works out what a command means stop
 * as soon as something is wrong, without also having to know how the chatbot
 * prints things. Every such error travels up to one place — the command loop in
 * {@link Bob} — which catches it and prints the message inside the usual block
 * of output. So the rule "an unusable command is reported, not obeyed" is
 * enforced in a single place instead of in every command.
 *
 * <p>It extends {@code Exception} rather than {@code RuntimeException} so the
 * compiler insists that it is either handled or declared. A mistyped command is
 * an expected part of talking to a chatbot, so it should be impossible to forget
 * to deal with it.
 *
 * <p>A single exception class is enough here because the chatbot's only response
 * to any of these errors is to print the message. If different errors ever
 * needed different handling, the usual next step would be subclasses (say
 * {@code UnknownCommandException}) so that {@code catch} could tell them apart.
 */
public class BobException extends Exception {

    /**
     * Creates an error carrying a message meant for the user's eyes.
     *
     * @param message what went wrong, and where helpful how to put it right;
     *                may contain {@code \n} to be printed as several lines.
     */
    public BobException(String message) {
        super(message);
    }
}
