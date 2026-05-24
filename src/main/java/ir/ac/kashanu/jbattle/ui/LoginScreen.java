package ir.ac.kashanu.jbattle.ui;

import ir.ac.kashanu.jbattle.auth.AuthResult;
import ir.ac.kashanu.jbattle.auth.AuthService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/** Builds the login / signup view. Stateless factory. */
public final class LoginScreen {

    private LoginScreen() {
    }

    /**
     * @param auth            authentication backend
     * @param onAuthenticated called with the username once login or signup succeeds
     */
    public static Parent create(AuthService auth, Consumer<String> onAuthenticated) {
        Label title = new Label("JBattle: Goblin Siege");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #ffd479;");
        Label subtitle = new Label("Log in, or sign up, to enter the battle.");
        subtitle.setStyle("-fx-text-fill: #cfcfe0;");

        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        Label message = new Label();
        message.setWrapText(true);

        Button login = new Button("Log In");
        login.setDefaultButton(true);
        Button signup = new Button("Sign Up");

        Consumer<AuthResult> handle = result -> {
            if (result.success()) {
                onAuthenticated.accept(username.getText().trim());
            } else {
                message.setText(result.message());
                message.setStyle("-fx-text-fill: #ff6b6b;");
            }
        };
        Runnable doLogin = () -> handle.accept(auth.login(username.getText(), password.getText()));
        login.setOnAction(e -> doLogin.run());
        password.setOnAction(e -> doLogin.run());
        signup.setOnAction(e -> handle.accept(auth.signup(username.getText(), password.getText())));

        HBox buttons = new HBox(10, login, signup);
        buttons.setAlignment(Pos.CENTER);

        VBox card = new VBox(14, title, subtitle, username, password, buttons, message);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40));
        card.setMaxWidth(380);

        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #2b2b3c, #1a1a26);");
        return root;
    }
}
