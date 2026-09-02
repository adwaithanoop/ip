package bob.ui;

import java.io.IOException;

import bob.Bob;
import bob.storage.Storage;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * The chatbot as a windowed program.
 *
 * <p>This is the graphical counterpart of {@link Bob#main}: it makes a chatbot,
 * gives it something to talk through, and steps aside. The difference is that a
 * console program then runs a loop until the input ends, while this one hands its
 * window to JavaFX and returns — from there on the user's typing and clicking are
 * what make anything happen, and {@link MainWindow} answers them.
 *
 * <p>Extending {@link Application} is what makes this class a JavaFX program:
 * {@link Application#launch} sets up the toolkit, opens a window, and calls
 * {@link #start} on the thread that JavaFX draws on. That launch is done from
 * {@link bob.Launcher Launcher} rather than from a {@code main} here, for the reason given there.
 *
 * <p>The chatbot is made here, and only here, so that the window below is left
 * knowing how to show a conversation without also having to decide whose it is or
 * where its tasks are kept. It reads and writes the same save file as the console
 * chatbot, so a task added in one is there in the other.
 */
public class Main extends Application {

    /**
     * Narrowest the window may be dragged to: enough for the Send button and a
     * useful amount of the text field beside it.
     */
    private static final double MIN_WIDTH = 380.0;

    /** Shortest the window may be dragged to: the input row and a bubble or two. */
    private static final double MIN_HEIGHT = 300.0;

    /** The chatbot the window talks to, kept in the same save file the console one uses. */
    private final Bob bob = Bob.forGui(Storage.DEFAULT_FILE_PATH);

    /**
     * Builds the window from {@code /view/MainWindow.fxml}, points it at the
     * chatbot, and shows it.
     *
     * <p>The controller is fetched from the loader rather than made here, because
     * the loader has already made one — the FXML names the class — and it is that
     * one the window's controls were given to. Handing the chatbot to any other
     * would leave the window talking to nobody.
     */
    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/view/MainWindow.fxml"));
            AnchorPane root = fxmlLoader.load();
            fxmlLoader.<MainWindow>getController().setBob(bob);

            stage.setScene(new Scene(root));
            stage.setTitle("Bob");
            stage.setMinWidth(MIN_WIDTH);
            stage.setMinHeight(MIN_HEIGHT);
            stage.show();
        } catch (IOException e) {
            // The FXML is packaged with the program, so failing to read it means a
            // broken build rather than anything the user could put right.
            throw new IllegalStateException("Could not load the chatbot's window layout", e);
        }
    }
}
