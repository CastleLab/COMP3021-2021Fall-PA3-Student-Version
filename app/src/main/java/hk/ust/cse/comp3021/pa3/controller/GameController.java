package hk.ust.cse.comp3021.pa3.controller;

import hk.ust.cse.comp3021.pa3.model.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Controller for {@link hk.ust.cse.comp3021.pa3.InertiaFxGame}.
 *
 * <p>
 * All game state mutations should be performed by this class.
 * </p>
 */
public class GameController {

    /**
     * The mapping from {@link Player#getId()} of the player to his game state.
     */
    @NotNull
    private final Map<Integer, GameState> gameStates;

    /**
     * Gets the current {@link GameState} controlled by the controller if the game is single player mode.
     *
     * @return The game state.
     * @throws IllegalArgumentException if there are more than one player.
     */
    public @NotNull GameState getGameState() {
        if (gameStates.size() > 1) {
            throw new IllegalArgumentException();
        }
        var iter = gameStates.values().iterator();
        return iter.next();
    }

    /**
     * Get the game state associated to the player with {@code playerID}.
     *
     * @param playerID ID of the player.
     * @return the game state instance.
     */
    public @NotNull GameState getGameState(int playerID) {
        var s = gameStates.get(playerID);
        if (s == null) {
            throw new IllegalArgumentException();
        }
        return s;
    }

    /**
     * @return all game state instances of all players as an array.
     */
    public GameState[] getGameStates() {
        return gameStates.values().toArray(new GameState[0]);
    }

    /**
     * Get the game board instance.
     *
     * @return the game board instance.
     */
    public GameBoard getGameBoard() {
        // Although there are multiple game state instance, there is only one game board instance that is shared by all game states.
        assert getGameStates().length > 0;
        return getGameStates()[0].getGameBoard();
    }

    public Player[] getPlayers() {
        return gameStates.values().stream().map(GameState::getPlayer).toArray(Player[]::new);
    }

    /**
     * Creates an instance.
     * Multiple instances of {@link GameState} can be provided in case there are multiple players.
     * Each player corresponds to a {@link GameState} instance.
     *
     * @param gameStates An array of instances of {@link GameState} to control.
     * @throws IllegalArgumentException if the number of {@link GameState} is less than 1.
     */
    public GameController(@NotNull final GameState... gameStates) {
        if (gameStates.length <= 0) {
            throw new IllegalArgumentException();
        }
        this.gameStates = new HashMap<>();
        for (var s :
                gameStates) {
            this.gameStates.put(s.getPlayer().getId(), s);
        }
    }

    /**
     * Processes a Move action performed by the player.
     *
     * @param direction The direction the player wants to move to.
     * @return An instance of {@link MoveResult} indicating the result of the action.
     */
    public MoveResult processMove(@NotNull final Direction direction) {
        return processMove(direction, getGameState().getPlayer().getId());
    }

    /**
     * Processes a Move action performed by the player.
     * TODO modify this method if you need to do thread synchronization.
     *
     * @param direction The direction the player wants to move to.
     * @param playerID  ID of the player to move.
     * @return An instance of {@link MoveResult} indicating the result of the action.
     */
    public MoveResult processMove(@NotNull final Direction direction, int playerID) {
        Objects.requireNonNull(direction);

        var lock = getGameBoard().getLock();
        lock.lock();
        var result = this.getGameState(playerID).getGameBoardController().makeMove(direction, playerID);
        if (result == null) {
            lock.unlock();
            return null;
        }

        var gameState = this.getGameState(playerID);
        if (result instanceof MoveResult.Valid v) {
            gameState.incrementNumMoves();

            if (v instanceof MoveResult.Valid.Alive va) {
                gameState.increaseNumLives(va.collectedExtraLives.size());
                gameState.increaseNumGotGems(va.collectedGems.size());
                gameState.getMoveStack().push(va);
            } else if (v instanceof MoveResult.Valid.Dead) {
                gameState.incrementNumDeaths();
                var livesLeft = gameState.decrementNumLives();
                if (livesLeft == 0) {
                    this.getGameState(playerID).getGameBoardController().kickOut(playerID);
                    result = new MoveResult.Valid.KickedOut(v.origPosition);
                }
            }
        }

        lock.unlock();
        return result;
    }

    /**
     * Processes an Undo action performed by the player.
     * Undo is only allowed in single player mode.
     *
     * @return {@code false} if there are no steps to undo.
     * @throws IllegalCallerException when the there are more than one player.
     */
    public boolean processUndo() {
        if (gameStates.size() > 1) {
            throw new IllegalCallerException();
        }

        if (this.getGameState().getMoveStack().isEmpty()) {
            return false;
        }

        final var prevState = this.getGameState().getMoveStack().pop();
        // This condition is impossible under this implementation, but just do it anyways.
        if (!(prevState instanceof final MoveResult.Valid.Alive aliveState)) {
            return false;
        }

        this.getGameState().decreaseNumLives(aliveState.collectedExtraLives.size());

        this.getGameState().getGameBoardController().undoMove(aliveState);
        return true;
    }

    /**
     * TODO Get winners of the game.
     * You can find the winning conditions from README.
     *
     * @return null if the game has not finished yet; otherwise emtpy array if there is no winners, or non-empty array if there are winners.
     */
    @Nullable
    public Player[] getWinners() {
        if (getGameBoard().getNumGems() == 0) {
            // not gems left
            // game ends with multiple winners
            var winners = new ArrayList<Player>();
            var maxScore = Integer.MIN_VALUE;
            for (var state :
                    getGameStates()) {
                if (state.hasLost()) {
                    continue;
                }
                if (state.getScore() > maxScore) {
                    maxScore = state.getScore();
                    winners.clear();
                    winners.add(state.getPlayer());
                } else if (state.getScore() == maxScore) {
                    winners.add(state.getPlayer());
                }
            }
            return winners.toArray(new Player[0]);
        } else {
            var hasPlayer = false;
            for (var state :
                    getGameStates()) {
                if (!state.hasLost()) {
                    hasPlayer = true;
                }
            }
            if (!hasPlayer) {
                // no one wins
                return new Player[0];
            }
        }
        return null;
    }
}
