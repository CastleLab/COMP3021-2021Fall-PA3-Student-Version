package hk.ust.cse.comp3021.pa3.view.panes;

import hk.ust.cse.comp3021.pa3.InertiaFxGame;
import hk.ust.cse.comp3021.pa3.controller.GameController;
import hk.ust.cse.comp3021.pa3.model.GameBoard;
import hk.ust.cse.comp3021.pa3.model.GameState;
import hk.ust.cse.comp3021.pa3.model.MoveResult;
import hk.ust.cse.comp3021.pa3.view.GameUIComponent;
import hk.ust.cse.comp3021.pa3.view.UIServices;
import hk.ust.cse.comp3021.pa3.view.events.MoveEvent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link javafx.scene.layout.Pane} representing the game play interface of the game.
 */
public class MainGamePane extends VBox implements GameUIComponent {
    private boolean gameEnded = false;

    private final Label gameTitle = new Label("Inertia Game");

    private final GameBoardPane gameBoardPane = new GameBoardPane();

    /**
     * The list of {@link PlayerPane} instances, each of which corresponds to a player.
     * The length of this list should be equal to the total number of players.
     */
    private final List<PlayerPane> playerPanes = new ArrayList<>();

    private final GameController gameController;

    private final InertiaFxGame game;

    /**
     * Creates a new instance of {@link MainGamePane}.
     * This constructor is only meant to maintain backward compatibility with PA2.
     * When there is only one single player, the game state of that player is provided in this constructor.
     *
     * @param gameState The {@link GameState} to start playing.
     */
    public MainGamePane(GameState gameState, InertiaFxGame game) {
        this(new GameState[]{gameState}, game);
    }

    /**
     * Creates a new instance of {@link MainGamePane}.
     *
     * @param gameStates The list of {@link GameState} instances corresponding to that of each player.
     *                   The length of this list should be equal to the total number of players.
     */
    public MainGamePane(GameState[] gameStates, InertiaFxGame game) {
        this.gameController = new GameController(gameStates);
        this.game = game;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeComponents() {
        this.gameTitle.getStyleClass().add("game-title");

        // Initialize and add the game board pane
        this.gameBoardPane.initializeComponents();
        this.gameBoardPane.showGameState(gameController.getGameStates());

        var operationArea = new HBox();
        operationArea.getChildren().add(gameBoardPane);
        for (var s :
                gameController.getGameStates()) {
            var playerPane = new PlayerPane(gameController, s, gameController.getGameStates().length == 1);
            playerPane.initializeComponents();
            playerPane.setOnMove(this::gameMoveHandler);
            playerPanes.add(playerPane);
            operationArea.getChildren().add(playerPane);
        }
        this.getChildren().addAll(
                gameTitle,
                operationArea
        );
        VBox.setVgrow(operationArea, Priority.ALWAYS);
        HBox.setHgrow(gameBoardPane, Priority.ALWAYS);
    }

    /**
     * Return the {@link GameBoard} instance.
     * Note that although there can be multiple {@link hk.ust.cse.comp3021.pa3.model.Player} instances and
     * multiple {@link GameState} instances,
     * there is only one instance of {@link GameBoard}, which is shared among all {@link GameState} instances.
     *
     * @return the game board instance that is shared by all {@link GameState} instances.
     */
    private GameBoard getGameBoard() {
        assert gameController.getGameStates().length > 0;
        return gameController.getGameStates()[0].getGameBoard();
    }

    /**
     * {@link javafx.event.Event} handler for a game move operation triggered by {@link GameControlPane}.
     * TODO you may or may not need to do thread synchronization in this method.
     *
     * @param e The corresponding {@link MoveEvent}.
     */
    private synchronized void gameMoveHandler(MoveEvent e) {
        if (gameEnded) {
            return;
        }
        var lock = gameController.getGameBoard().getLock();
        lock.lock();

        // update the gameBoardPane with the latest game states.
        this.gameBoardPane.showGameState(gameController.getGameStates());

        // show lose dialog if the move event indicates a player loses and get kicked out of the game board.
        if (e.getMoveResult() instanceof MoveResult.Valid.KickedOut) {
            getPlayerPane(e.getPlayerID()).kickOut();
            this.getPlayerPane(e.getPlayerID()).stopRobot();
            UIServices.showLoseDialog(gameController.getGameBoard().getPlayer(e.getPlayerID()));
        }

        // try to get winners from the game controller
        var winners = gameController.getWinners();

        lock.unlock();

        // winners == null means the game is still on going.
        if (winners != null) {
            gameEnded = true;
            // stop all enabled robots if exist
            for (var playerPane :
                    playerPanes) {
                playerPane.stopRobot();
            }

            // show win dialog for every winner.
            for (var winner :
                    winners) {
                UIServices.showWinDialog(winner);
            }

            // return to main menu
            if (game != null) game.showMainMenu();
        }
    }

    /**
     * @return the {@link GameController} instance.
     */
    public GameController getGameController() {
        return gameController;
    }

    /**
     * Get the {@link PlayerPane} instance of the player.
     * t*
     *
     * @param playerID The id of the player.
     * @return The PlayerPane instance.
     */
    public PlayerPane getPlayerPane(int playerID) {
        return this.playerPanes.stream()
                .filter(p -> p.getPlayer().getId() == playerID)
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
