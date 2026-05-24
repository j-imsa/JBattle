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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The responsive battle screen: an arena where sprites move, a player health bar,
 * and a story board on the right. Implements {@link GameListener}; all engine
 * callbacks arrive off the FX thread and are bounced through {@link Platform#runLater}.
 */
public class GameScreen implements GameListener {

    private static final double SPRITE = 48;

    private final String username;
    private final PlayRepository plays;
    private final Runnable onLogout;

    private final BorderPane root = new BorderPane();
    private final Pane arena = new Pane();
    private final TextArea storyBoard = new TextArea();
    private final ProgressBar healthBar = new ProgressBar(1.0);
    private final Label healthLabel = new Label();
    private final Button newPlayButton = new Button("New Play");

    private final Map<GameCharacter, ImageView> sprites = new HashMap<>();
    private final Map<CharacterType, Image> imageCache = new EnumMap<>(CharacterType.class);
    private final Circle targetMarker = new Circle(7);

    private GameEngine engine;
    private final AnimationTimer renderLoop;

    public GameScreen(String username, PlayRepository plays, Runnable onLogout) {
        this.username = username;
        this.plays = plays;
        this.onLogout = onLogout;
        build();
        this.renderLoop = createRenderLoop();
        renderLoop.start();
    }

    public BorderPane getRoot() {
        return root;
    }

    private void build() {
        // --- top bar: identity, health, controls ---
        Label name = new Label("Hero: " + username);
        name.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
        Label heart = new Label("❤");
        heart.setStyle("-fx-text-fill: #f44336; -fx-font-size: 16px;");
        healthBar.setPrefWidth(220);
        healthLabel.setStyle("-fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        newPlayButton.setOnAction(e -> startNewPlay());
        Button logout = new Button("Logout");
        logout.setOnAction(e -> doLogout());

        HBox top = new HBox(12, name, heart, healthBar, healthLabel, spacer, newPlayButton, logout);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(10));
        top.setStyle("-fx-background-color: #20202c;");

        // --- arena (grows to fill the center) ---
        arena.setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 80%, #3c5a2e, #213018);");
        arena.setMinSize(400, 300);
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

        // --- story board on the right ---
        Label boardTitle = new Label("📜 Battle Chronicle");
        boardTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
        storyBoard.setEditable(false);
        storyBoard.setWrapText(true);
        storyBoard.setPrefWidth(340);
        storyBoard.setMinWidth(280);
        VBox.setVgrow(storyBoard, Priority.ALWAYS);
        VBox board = new VBox(6, boardTitle, storyBoard);
        board.setPadding(new Insets(10));
        board.setStyle("-fx-background-color: #1a1a26;");

        root.setTop(top);
        root.setCenter(arena);
        root.setRight(board);

        storyBoard.appendText("Welcome, " + username + "!\n");
        appendHistory();
        storyBoard.appendText("\nPress \"New Play\" to begin the siege.\n");
        updateHealthDisplay(1.0, CharacterType.PLAYER.getMaxHealth());
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

    private void doLogout() {
        if (engine != null) {
            engine.stop();
        }
        renderLoop.stop();
        onLogout.run();
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
