package ir.ac.kashanu.jbattle;

import ir.ac.kashanu.jbattle.auth.AuthService;
import ir.ac.kashanu.jbattle.persistence.PlayRepository;
import ir.ac.kashanu.jbattle.persistence.UserRepository;
import ir.ac.kashanu.jbattle.ui.GameScreen;
import ir.ac.kashanu.jbattle.ui.LoginScreen;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Path;

/**
 * JavaFX entry point. Owns the single {@link Stage} and swaps its root between the
 * login screen and the game screen. Persistence lives in a {@code data/} directory
 * next to the working directory (the file-based "db").
 */
public class JBattleApplication extends Application {

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
        setRoot(LoginScreen.create(authService, this::showGame));
    }

    private void showGame(String username) {
        GameScreen screen = new GameScreen(username, playRepository, this::showLogin);
        setRoot(screen.getRoot());
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
