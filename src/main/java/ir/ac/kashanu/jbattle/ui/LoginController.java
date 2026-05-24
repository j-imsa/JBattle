package ir.ac.kashanu.jbattle.ui;

import ir.ac.kashanu.jbattle.auth.AuthResult;
import ir.ac.kashanu.jbattle.auth.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.function.Consumer;

/**
 * Controller for {@code fxml/login.fxml}: the login / signup view. The layout is
 * declared in FXML; this class only wires the two buttons to {@link AuthService}.
 * Dependencies are injected via {@link #init} after the loader builds the view,
 * because FXMLLoader instantiates controllers through their no-arg constructor.
 */
public class LoginController {

    @FXML private TextField username;
    @FXML private PasswordField password;
    @FXML private Label message;

    private AuthService auth;
    private Consumer<String> onAuthenticated;

    /**
     * @param auth            authentication backend
     * @param onAuthenticated called with the username once login or signup succeeds
     */
    public void init(AuthService auth, Consumer<String> onAuthenticated) {
        this.auth = auth;
        this.onAuthenticated = onAuthenticated;
    }

    @FXML
    private void onLogin() {
        handle(auth.login(username.getText(), password.getText()));
    }

    @FXML
    private void onSignup() {
        handle(auth.signup(username.getText(), password.getText()));
    }

    private void handle(AuthResult result) {
        if (result.success()) {
            onAuthenticated.accept(username.getText().trim());
        } else {
            message.setText(result.message());
            if (!message.getStyleClass().contains("error")) {
                message.getStyleClass().add("error");
            }
        }
    }
}
