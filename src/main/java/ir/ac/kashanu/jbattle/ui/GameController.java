package ir.ac.kashanu.jbattle.ui;

import ir.ac.kashanu.jbattle.game.GameEngine;
import ir.ac.kashanu.jbattle.game.GameListener;
import ir.ac.kashanu.jbattle.game.GameResult;
import ir.ac.kashanu.jbattle.game.GameStats;
import ir.ac.kashanu.jbattle.model.CharacterType;
import ir.ac.kashanu.jbattle.model.GameCharacter;
import ir.ac.kashanu.jbattle.model.PlayRecord;
import ir.ac.kashanu.jbattle.persistence.PlayRepository;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for {@code fxml/game.fxml}: the responsive battle screen. The static
 * chrome (top bar, arena, story board) lives in the FXML; this class owns the
 * dynamic parts — the sprites added to the arena, click-to-move, the render loop
 * and the {@link GameListener} callbacks.
 *
 * <p>All engine callbacks arrive off the FX thread and are bounced through
 * {@link Platform#runLater}. Dependencies are injected via {@link #init} after the
 * loader builds the view (FXMLLoader uses the no-arg constructor).
 */
public class GameController implements GameListener {

    private static final double SPRITE = 48;

    @FXML private Pane arena;
    @FXML private TextArea storyBoard;
    @FXML private ProgressBar healthBar;
    @FXML private Label healthLabel;
    @FXML private Label heroLabel;
    @FXML private Button newPlayButton;

    private String username;
    private PlayRepository plays;
    private Runnable onLogout;

    private final Map<GameCharacter, ImageView> sprites = new HashMap<>();
    private final Map<CharacterType, Image> imageCache = new EnumMap<>(CharacterType.class);
    private final Circle targetMarker = new Circle(7);

    private GameEngine engine;
    private AnimationTimer renderLoop;

    /** Called by FXMLLoader once the @FXML fields are injected. */
    @FXML
    private void initialize() {
        // Keep sprites clipped to the arena as it resizes.
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(arena.widthProperty());
        clip.heightProperty().bind(arena.heightProperty());
        arena.setClip(clip);
        arena.widthProperty().addListener((o, ov, nv) -> syncBounds());
        arena.heightProperty().addListener((o, ov, nv) -> syncBounds());

        // Click-to-move: walk the hero toward where the player clicks.
        targetMarker.setFill(Color.web("#ffd479", 0.30));
        targetMarker.setStroke(Color.web("#ffd479", 0.9));
        targetMarker.setMouseTransparent(true);
        targetMarker.setVisible(false);
        arena.setOnMouseClicked(e -> {
            if (engine != null && engine.isRunning()) {
                engine.setPlayerTarget(e.getX(), e.getY());
                targetMarker.setCenterX(e.getX());
                targetMarker.setCenterY(e.getY());
                targetMarker.setVisible(true);
            }
        });
    }

    /**
     * Injects backend dependencies and starts the render loop. Must be called once,
     * after the FXML is loaded.
     */
    public void init(String username, PlayRepository plays, Runnable onLogout) {
        this.username = username;
        this.plays = plays;
        this.onLogout = onLogout;

        heroLabel.setText("Hero: " + username);
        storyBoard.appendText("Welcome, " + username + "!\n");
        appendHistory();
        storyBoard.appendText("\nPress \"New Play\" to begin the siege.\n");
        updateHealthDisplay(1.0, CharacterType.PLAYER.getMaxHealth());

        renderLoop = createRenderLoop();
        renderLoop.start();
    }

    @FXML
    private void onNewPlay() {
        startNewPlay();
    }

    @FXML
    private void onLogout() {
        if (engine != null) {
            engine.stop();
        }
        renderLoop.stop();
        onLogout.run();
    }

    private void startNewPlay() {
        if (engine != null) {
            engine.stop();
        }
        arena.getChildren().clear();
        sprites.clear();

        double width = arena.getWidth() > 0 ? arena.getWidth() : 800;
        double height = arena.getHeight() > 0 ? arena.getHeight() : 540;
        engine = new GameEngine(this, username, width, height);

        targetMarker.setVisible(false);
        arena.getChildren().add(targetMarker); // sits beneath the sprites

        for (GameCharacter c : engine.getCharacters()) {
            ImageView view = new ImageView(imageFor(c.getType()));
            view.setFitWidth(SPRITE);
            view.setFitHeight(SPRITE);
            view.setPreserveRatio(true);
            view.setLayoutX(c.getX());
            view.setLayoutY(c.getY());
            sprites.put(c, view);
            arena.getChildren().add(view);
        }

        storyBoard.clear();
        appendHistory();
        storyBoard.appendText("Click in the arena to move your hero toward the foes.\n");
        newPlayButton.setDisable(true);
        engine.start();
    }

    private AnimationTimer createRenderLoop() {
        return new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (engine == null) {
                    return;
                }
                for (GameCharacter c : engine.getCharacters()) {
                    ImageView view = sprites.get(c);
                    if (view == null) {
                        continue;
                    }
                    if (!c.isAlive()) {
                        view.setVisible(false);
                        continue;
                    }
                    view.setLayoutX(c.getX());
                    view.setLayoutY(c.getY());
                }
                GameCharacter player = engine.getPlayer();
                updateHealthDisplay(Math.max(0, player.healthRatio()), Math.max(0, player.getHealth()));
                if (!player.hasTarget()) {
                    targetMarker.setVisible(false); // hero reached the spot
                }
            }
        };
    }

    private void updateHealthDisplay(double ratio, int health) {
        healthBar.setProgress(ratio);
        healthLabel.setText(health + " / " + CharacterType.PLAYER.getMaxHealth());
        String color = ratio > 0.5 ? "#4caf50" : ratio > 0.25 ? "#ff9800" : "#f44336";
        healthBar.setStyle("-fx-accent: " + color + ";");
    }

    private void syncBounds() {
        if (engine != null) {
            engine.setBounds(arena.getWidth(), arena.getHeight());
        }
    }

    private void appendHistory() {
        List<PlayRecord> history = plays.findByUser(username);
        if (history.isEmpty()) {
            storyBoard.appendText("This is your first battle. Good luck!\n");
        } else {
            PlayRecord last = history.get(history.size() - 1);
            storyBoard.appendText("You have fought " + history.size() + " battle(s). "
                    + "Last: " + last.result() + " (" + last.monstersDefeated() + " slain).\n");
        }
    }

    private Image imageFor(CharacterType type) {
        return imageCache.computeIfAbsent(type, t -> {
            var stream = getClass().getResourceAsStream(t.getImageResource());
            if (stream == null) {
                throw new IllegalStateException("Missing sprite resource: " + t.getImageResource());
            }
            return new Image(stream);
        });
    }

    // --- GameListener (called off the FX thread) ------------------------------

    @Override
    public void onNarration(String message) {
        Platform.runLater(() -> storyBoard.appendText(message + "\n"));
    }

    @Override
    public void onGameOver(GameResult result, GameStats stats) {
        Platform.runLater(() -> {
            String banner = result == GameResult.VICTORY ? "🏆 VICTORY! 🏆" : "☠ DEFEAT ☠";
            storyBoard.appendText("\n===== " + banner + " =====\n");
            storyBoard.appendText("Monsters defeated: " + stats.monstersDefeated()
                    + "/" + stats.totalMonsters() + "\n");
            storyBoard.appendText("Health remaining: " + stats.remainingHealth() + "\n");
            storyBoard.appendText("Battle lasted: " + stats.durationSeconds() + "s\n");

            plays.add(new PlayRecord(username, System.currentTimeMillis(), result.name(),
                    stats.monstersDefeated(), stats.remainingHealth(), stats.durationSeconds()));
            newPlayButton.setDisable(false);
        });
    }
}
