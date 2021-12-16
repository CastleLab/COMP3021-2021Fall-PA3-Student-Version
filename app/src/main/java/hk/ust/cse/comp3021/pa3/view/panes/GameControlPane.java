package hk.ust.cse.comp3021.pa3.view.panes;

import hk.ust.cse.comp3021.pa3.controller.GameController;
import hk.ust.cse.comp3021.pa3.model.Direction;
import hk.ust.cse.comp3021.pa3.model.GameState;
import hk.ust.cse.comp3021.pa3.model.Player;
import hk.ust.cse.comp3021.pa3.util.MoveDelegate;
import hk.ust.cse.comp3021.pa3.view.GameUIComponent;
import hk.ust.cse.comp3021.pa3.view.events.MoveEvent;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A {@link javafx.scene.layout.Pane} representing the control area of a player.
 * That is to say, each player has a {@link GameControlPane} in the GUI.
 */
public class GameControlPane extends GridPane implements GameUIComponent {
    /**
     * The player that this instance corresponds to.
     */
    private final Player player;

    /**
     * Whether this player is allowed to undo movements.
     * Undo should only be allowed when there is only one player.
     */
    private final boolean allowUndo;

    private final Button upButton = new Button("\u2191");
    private final Button downButton = new Button("\u2193");
    private final Button leftButton = new Button("\u2190");
    private final Button rightButton = new Button("\u2192");

    private final Button undoButton = new Button("UNDO");

    private GameController gameController;
    private MoveDelegate moveDelegate;

    private final ObjectProperty<EventHandler<MoveEvent>> moveEvent = new ObjectPropertyBase<>() {
        @Override
        public Object getBean() {
            return GameControlPane.this;
        }

        @Override
        public String getName() {
            return "onMove";
        }
    };

    /**
     * Create an instance.
     *
     * @param gameController The game controller, which is shared among all {@link GameControlPane} instances.
     * @param player         The player that this instance should correspond to.
     * @param allowUndo      Whether undo is allowed for this player.
     */
    GameControlPane(GameController gameController, Player player, boolean allowUndo) {
        this.gameController = gameController;
        this.player = player;
        this.allowUndo = allowUndo;
    }

    /**
     * Performs a move action towards the specified {@link Direction}.
     * TODO you may or may not need to do thread synchronization in this method.
     *
     * @param direction The {@link Direction} to move.
     */
    private void move(@NotNull Direction direction) {
        var result = this.gameController.processMove(direction, player.getId());
        if (result != null) {
            this.moveEvent.get().handle(new MoveEvent(result, player.getId()));
        }
    }

    /**
     * Sets the {@link EventHandler} for the move event.
     *
     * @param handler The handler.
     */
    public void setOnMove(EventHandler<MoveEvent> handler) {
        this.moveEvent.set(handler);
    }

    /**
     * TODO Delegate the control of movement from the GUI to an automated delegate.
     * Call the {@link MoveDelegate#startDelegation(MoveDelegate.MoveProcessor)} method of the given delegate.
     * <p>
     * After delegation, the {@link GameControlPane#upButton}, {@link GameControlPane#downButton},
     * {@link GameControlPane#leftButton}, and {@link GameControlPane#rightButton}
     * should be disabled to disallow the control from GUI, i.e., call {@link GameControlPane#disable()}.
     *
     * @param delegate The automated delegate to control the movement.
     */
    public void delegateControl(MoveDelegate delegate) {
        this.revokeControl();
        moveDelegate = delegate;
        moveDelegate.startDelegation(direction -> {
            Platform.runLater(() -> {
                move(direction);
            });
        });
        this.disable();
    }

    /**
     * TODO Revoke the control from the delegate if there is any.
     * After revoking delegation, the {@link GameControlPane#upButton}, {@link GameControlPane#downButton},
     * {@link GameControlPane#leftButton}, and {@link GameControlPane#rightButton}
     * should be enabled to allow control from GUI, i.e., call {@link GameControlPane#enable()}.
     */
    public void revokeControl() {
        if (moveDelegate != null) {
            moveDelegate.stopDelegation();
        }
        this.enable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeComponents() {
        setLayout();

        // Setup and add the move buttons.
        setMoveButtonsHandler();
        styleMoveButtons();
        addMoveButtons();
        addPlayerImage();

        // Setup and add the undo button.
        if (this.allowUndo) {
            setUndoButtonLayout();
            this.add(undoButton, 0, 3, 3, 1);
        }
    }

    private void setUndoButtonLayout() {
        this.undoButton.setMaxWidth(Double.MAX_VALUE);
        GridPane.setFillWidth(undoButton, true);

        this.undoButton.setOnAction(e -> this.performUndo());
    }

    /**
     * Set the event handlers for the move buttons
     */
    private void setMoveButtonsHandler() {
        this.upButton.setOnAction(e -> move(Direction.UP));
        this.downButton.setOnAction(e -> move(Direction.DOWN));
        this.leftButton.setOnAction(e -> move(Direction.LEFT));
        this.rightButton.setOnAction(e -> move(Direction.RIGHT));
    }

    /**
     * Sets the layout of the panel.
     */
    private void setLayout() {
        this.setPadding(new Insets(10));
        this.setVgap(5);
        this.setHgap(5);
    }

    /**
     * Adds the move buttons to the appropriate position in the grid.
     */
    private void addMoveButtons() {
        this.add(upButton, 1, 0);
        this.add(leftButton, 0, 1);
        this.add(rightButton, 2, 1);
        this.add(downButton, 1, 2);
    }

    /**
     * Adds the image of the player in the middle of the buttons.
     */
    private void addPlayerImage() {
        var resourceName = player.toImage();
        var resourceUrl = Objects.requireNonNull(getClass().getResource(resourceName));
        var image = new Image(resourceUrl.toExternalForm());
        var imageView = new ImageView(image);
        imageView.setFitHeight(50);
        imageView.setFitWidth(50);
        this.add(imageView, 1, 1);
    }

    /**
     * Sets the CSS class for the move buttons
     */
    private void styleMoveButtons() {
        this.upButton.getStyleClass().add("move-button");
        this.downButton.getStyleClass().add("move-button");
        this.leftButton.getStyleClass().add("move-button");
        this.rightButton.getStyleClass().add("move-button");
    }

    /**
     * Sets the {@link GameController} on which the move actions are performed.
     *
     * @param gameController The {@link GameController}.
     */
    public void setGameController(GameController gameController) {
        this.gameController = gameController;
    }

    /**
     * Performs an undo action on the game.
     */
    public void performUndo() {
        if (gameController.getGameState().getMoveStack().isEmpty()) {
            return;
        }
        var mostRecentMove = gameController.getGameState().getMoveStack().peek();
        this.gameController.processUndo();
        this.moveEvent.get().handle(new MoveEvent(mostRecentMove, player.getId()));
    }

    /**
     * Get the player that this instance belongs to.
     *
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Get the {@link GameState} instance that the player owning this instance is associated with.
     * Recall that each {@link Player} instance is associated with a {@link GameState} instance.
     *
     * @return the game state.
     */
    public GameState getGameState() {
        return gameController.getGameState(player.getId());
    }

    /**
     * Disable {@link GameControlPane#upButton}, {@link GameControlPane#downButton},
     * {@link GameControlPane#leftButton}, and {@link GameControlPane#rightButton}.
     */
    public void disable() {
        this.upButton.setDisable(true);
        this.downButton.setDisable(true);
        this.leftButton.setDisable(true);
        this.rightButton.setDisable(true);
        this.undoButton.setDisable(true);
    }

    /**
     * Enable {@link GameControlPane#upButton}, {@link GameControlPane#downButton},
     * {@link GameControlPane#leftButton}, and {@link GameControlPane#rightButton}.
     */
    public void enable() {
        this.upButton.setDisable(false);
        this.downButton.setDisable(false);
        this.leftButton.setDisable(false);
        this.rightButton.setDisable(false);
        this.undoButton.setDisable(false);
    }
}
