package bob.ui;

import bob.Bob;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * The chatbot's window: the conversation so far, and the box the user types into.
 *
 * <p>This class is to the window what {@link bob.Bob Bob}'s command loop is to the console. It
 * does the same three things — take a line from the user, have the chatbot answer
 * it, show the answer — but driven by the user pressing a key or a button rather
 * than by a loop, because that is how a window works: nothing happens until
 * somebody does something, and then a method runs in response.
 *
 * <p>What the window looks like is not here. It is in
 * {@code /view/MainWindow.fxml}, which names this class as its controller and each
 * of the four controls below with the same name the field has. That split is worth
 * having because the two are changed for different reasons and by different means:
 * the arrangement, sizes and colors are adjusted by eye, and can be adjusted in
 * the FXML without recompiling anything, while what the buttons <em>do</em> is
 * decided here.
 *
 * <p>It knows nothing about tasks, commands or save files. It hands the line the
 * user typed to {@link Bob#getResponse} and is given back the text to show, which
 * is why adding a command to the chatbot needs no change here at all.
 */
public class MainWindow extends AnchorPane {

    /** How long the farewell stays on screen before the window closes itself. */
    private static final Duration FAREWELL_PAUSE = Duration.seconds(1.5);

    /** The scrolling view of the conversation. */
    @FXML
    private ScrollPane scrollPane;

    /** The conversation itself, one {@link DialogBox} per thing said. */
    @FXML
    private VBox dialogContainer;

    /** Where the user types. */
    @FXML
    private TextField userInput;

    /** The other way to send what has been typed, for users who would rather click. */
    @FXML
    private Button sendButton;

    /** The chatbot this window is talking to, handed over by {@link Main}. */
    private Bob bob;

    /** The picture shown beside everything the user says. */
    private final Image userImage = new Image(getClass().getResourceAsStream("/images/DaUser.png"));

    /** The picture shown beside everything the chatbot says. */
    private final Image bobImage = new Image(getClass().getResourceAsStream("/images/DaBob.png"));

    /**
     * Prepares the window once JavaFX has built it from the FXML.
     *
     * <p>Named {@code initialize} because {@link javafx.fxml.FXMLLoader FXMLLoader} looks for that
     * name and calls it. It cannot be a constructor: the controls are filled in
     * after the controller is made, so a constructor would run while every field
     * below was still {@code null}.
     *
     * <p>Tying the scroll position to the height of the conversation is what keeps
     * the newest bubble in view. The conversation only ever grows downwards, so
     * every time it gets taller the view scrolls to the bottom, and the user is
     * never left looking at an answer given several turns ago.
     */
    @FXML
    public void initialize() {
        scrollPane.vvalueProperty().bind(dialogContainer.heightProperty());
    }

    /**
     * Gives this window the chatbot it is to talk to, and shows its greeting.
     *
     * <p>The chatbot is handed in rather than made here, so that this class is
     * responsible for showing a conversation and not for deciding whose it is or
     * where its tasks are kept. That decision is {@link Main}'s.
     *
     * @param bob the chatbot to talk to, which must be one made by
     *            {@link Bob#forGui}.
     */
    public void setBob(Bob bob) {
        this.bob = bob;
        dialogContainer.getChildren().add(DialogBox.getBobDialog(bob.getGreeting(), bobImage));
    }

    /**
     * Answers the line the user has just typed: shows it, shows the chatbot's
     * reply below it, and empties the box ready for the next one.
     *
     * <p>Run when the user presses Enter or clicks Send, both of which the FXML
     * points at this method, so the two are the same action and not two that have
     * to be kept in step.
     *
     * <p>An empty box is ignored. At a console an empty line is worth complaining
     * about, since the user may not have meant to send one; here the box is in
     * front of them and plainly empty, so a complaint would say only what they can
     * already see. The box is emptied before that is decided, so that a box holding
     * nothing but spaces is left properly empty — showing its prompt again — rather
     * than looking blank while quietly refusing to send.
     *
     * <p>Saying goodbye closes the window, but not at once. The farewell is shown
     * first and the window closes a moment later, so the user sees the chatbot
     * answer rather than the window vanishing as they press the key. Typing is
     * disabled in the meantime, so nothing can be sent to a chatbot that has
     * already said goodbye.
     */
    @FXML
    private void handleUserInput() {
        String input = userInput.getText().trim();
        userInput.clear();
        if (input.isEmpty()) {
            return;
        }

        String response = bob.getResponse(input);
        dialogContainer.getChildren().addAll(
                DialogBox.getUserDialog(input, userImage),
                DialogBox.getBobDialog(response, bobImage));

        if (bob.isExit()) {
            userInput.setDisable(true);
            sendButton.setDisable(true);
            PauseTransition farewellPause = new PauseTransition(FAREWELL_PAUSE);
            farewellPause.setOnFinished(event -> Platform.exit());
            farewellPause.play();
        }
    }
}
