package ir.ac.kashanu.jbattle;

import ir.ac.kashanu.jbattle.auth.AuthService;
import ir.ac.kashanu.jbattle.persistence.PlayRepository;
import ir.ac.kashanu.jbattle.persistence.UserRepository;
import ir.ac.kashanu.jbattle.ui.GameController;
import ir.ac.kashanu.jbattle.ui.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

/**
 * JavaFX entry point. Owns the single {@link Stage} and swaps its root between the
 * login screen and the game screen, each loaded from FXML. Persistence lives in a
 * {@code data/} directory next to the working directory (the file-based "db").
 */
public class JBattleApplication extends Application {

    private static final String LOGIN_FXML = "/ir/ac/kashanu/jbattle/fxml/login.fxml";
    private static final String GAME_FXML = "/ir/ac/kashanu/jbattle/fxml/game.fxml";

    private Stage stage;
    private AuthService authService;
    private PlayRepository playRepository;

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        Path dataDir = Path.of("data");
        UserRepository users = new UserRepository(dataDir.resolve("users.db"));
        this.authService = new AuthService(users);
        this.playRepository = new PlayRepository(dataDir.resolve("plays.db"));

        stage.setTitle("JBattle: Goblin Siege");
        stage.setWidth(1100);
        stage.setHeight(700);
        stage.setMinWidth(800);
        stage.setMinHeight(560);
        showLogin();
        stage.show();
    }

    private void showLogin() {
        FXMLLoader loader = load(LOGIN_FXML);
        LoginController controller = loader.getController();
        controller.init(authService, this::showGame);
        setRoot(loader.getRoot());
    }

    private void showGame(String username) {
        FXMLLoader loader = load(GAME_FXML);
        GameController controller = loader.getController();
        controller.init(username, playRepository, this::showLogin);
        setRoot(loader.getRoot());
    }

    /** Loads an FXML document, surfacing the (unrecoverable) failure as unchecked. */
    private FXMLLoader load(String resource) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(resource));
        try {
            loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load " + resource, e);
        }
        return loader;
    }

    private void setRoot(Parent root) {
        Scene scene = stage.getScene();
        if (scene == null) {
            stage.setScene(new Scene(root));
        } else {
            scene.setRoot(root);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
