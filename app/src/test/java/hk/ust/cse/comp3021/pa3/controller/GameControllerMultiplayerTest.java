package hk.ust.cse.comp3021.pa3.controller;

import hk.ust.cse.comp3021.pa3.model.*;
import hk.ust.cse.comp3021.pa3.util.GameBoardUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class GameControllerMultiplayerTest {
    @Test
    @Tag("multiplayer")
    @DisplayName("Get Winners - the game is not finished yet")
    void testGetWinnersNull() {
        var p0 = new Player();
        var p1 = new Player();
        var gameBoard = GameBoardUtils.createGameBoard(2, 4, (pos) -> {
            if (pos.equals(new Position(0, 3))) {
                return new EntityCell(pos, new Gem());
            } else if (pos.equals(new Position(0, 0))) {
                return new StopCell(pos, p0);
            } else if (pos.equals(new Position(1, 3))) {
                return new StopCell(pos, p1);
            } else {
                return new EntityCell(pos);
            }
        });
        var controller = new GameController(
                new GameState(gameBoard, p0, 1),
                new GameState(gameBoard, p1, 1)
        );
        assertNull(controller.getWinners());
    }

    @Test
    @Tag("multiplayer")
    @DisplayName("Get Winners - the game finishes and there are no winners")
    void testGetWinnersEmpty() {
        var p0 = new Player();
        var p1 = new Player();
        var gameBoard = GameBoardUtils.createGameBoard(2, 4, (pos) -> {
            if (pos.equals(new Position(0, 3))) {
                return new EntityCell(pos, new Gem());
            } else if (pos.equals(new Position(0, 0))) {
                return new StopCell(pos, p0);
            } else if (pos.equals(new Position(1, 3))) {
                return new StopCell(pos, p1);
            } else if (pos.equals(new Position(1, 0))) {
                return new EntityCell(pos, new Mine());
            } else {
                return new EntityCell(pos);
            }
        });
        var controller = new GameController(
                new GameState(gameBoard, p0, 1),
                new GameState(gameBoard, p1, 1)
        );
        controller.processMove(Direction.DOWN, p0.getId());
        controller.processMove(Direction.LEFT, p1.getId());

        assertNotNull(controller.getWinners());
        assertEquals(0, controller.getWinners().length);
    }

    @Test
    @Tag("multiplayer")
    @DisplayName("Get Winners - the game is not finished and there are only one player left")
    void testGetWinnersNullWithSomeLost() {
        var p0 = new Player();
        var p1 = new Player();
        var gameBoard = GameBoardUtils.createGameBoard(2, 4, (pos) -> {
            if (pos.equals(new Position(0, 3))) {
                return new EntityCell(pos, new Gem());
            } else if (pos.equals(new Position(0, 0))) {
                return new StopCell(pos, p0);
            } else if (pos.equals(new Position(1, 3))) {
                return new StopCell(pos, p1);
            } else if (pos.equals(new Position(1, 0))) {
                return new EntityCell(pos, new Mine());
            } else {
                return new EntityCell(pos);
            }
        });
        var controller = new GameController(
                new GameState(gameBoard, p0, 1),
                new GameState(gameBoard, p1, 1)
        );
        controller.processMove(Direction.DOWN, p0.getId());

        assertNull(controller.getWinners());
    }

    @Test
    @Tag("multiplayer")
    @DisplayName("Get Winners - the game finishes and there are only one winner")
    void testGetWinnersSingleAlive() {
        var p0 = new Player();
        var p1 = new Player();
        var gameBoard = GameBoardUtils.createGameBoard(2, 4, (pos) -> {
            if (pos.equals(new Position(0, 3))) {
                return new EntityCell(pos, new Gem());
            } else if (pos.equals(new Position(0, 0))) {
                return new StopCell(pos, p0);
            } else if (pos.equals(new Position(1, 3))) {
                return new StopCell(pos, p1);
            } else if (pos.equals(new Position(1, 0))) {
                return new EntityCell(pos, new Mine());
            } else {
                return new EntityCell(pos);
            }
        });
        var controller = new GameController(
                new GameState(gameBoard, p0, 1),
                new GameState(gameBoard, p1, 1)
        );
        controller.processMove(Direction.DOWN, p0.getId());
        controller.processMove(Direction.UP, p1.getId());

        assertNotNull(controller.getWinners());
        assertThat(controller.getWinners(), Matchers.allOf(
                Matchers.arrayWithSize(1),
                Matchers.arrayContaining(p1)
        ));
    }

    @Test
    @Tag("multiplayer")
    @DisplayName("Get Winners - all players are alive and there are multiple winners")
    void testGetWinnersMultiple() {
        var p0 = new Player();
        var p1 = new Player();
        var gameBoard = GameBoardUtils.createGameBoard(2, 4, (pos) -> {
            if (pos.equals(new Position(0, 3))) {
                return new EntityCell(pos, new Gem());
            } else if (pos.equals(new Position(0, 0))) {
                return new StopCell(pos, p0);
            } else if (pos.equals(new Position(1, 3))) {
                return new StopCell(pos, p1);
            } else if (pos.equals(new Position(1, 0))) {
                return new EntityCell(pos, new Gem());
            } else {
                return new EntityCell(pos);
            }
        });
        var controller = new GameController(
                new GameState(gameBoard, p0, 1),
                new GameState(gameBoard, p1, 1)
        );
        controller.processMove(Direction.RIGHT, p0.getId());
        controller.processMove(Direction.LEFT, p1.getId());

        assertNotNull(controller.getWinners());
        assertThat(controller.getWinners(), Matchers.allOf(
                Matchers.arrayWithSize(2),
                Matchers.arrayContaining(p0, p1)
        ));
    }

    @Test
    @Tag("multiplayer")
    @DisplayName("Get Winners - all players are alive and there are only one winner")
    void testGetWinnersMultipleAlive() {
        var p0 = new Player();
        var p1 = new Player();
        var gameBoard = GameBoardUtils.createGameBoard(2, 4, (pos) -> {
            if (pos.equals(new Position(0, 3))) {
                return new EntityCell(pos, new Gem());
            } else if (pos.equals(new Position(0, 0))) {
                return new StopCell(pos, p0);
            } else if (pos.equals(new Position(1, 3))) {
                return new StopCell(pos, p1);
            } else if (pos.equals(new Position(1, 0))) {
                return new EntityCell(pos, new Gem());
            } else {
                return new EntityCell(pos);
            }
        });
        var controller = new GameController(
                new GameState(gameBoard, p0, 1),
                new GameState(gameBoard, p1, 1)
        );
        controller.processMove(Direction.RIGHT, p0.getId());
        controller.processMove(Direction.UP, p1.getId());
        controller.processMove(Direction.LEFT, p1.getId());
        controller.processMove(Direction.DOWN, p1.getId());

        assertNotNull(controller.getWinners());
        assertThat(controller.getWinners(), Matchers.allOf(
                Matchers.arrayWithSize(2),
                Matchers.arrayContaining(p0, p1)
        ));
    }

    // Map:
    // P.GG
    // G..M
    // ...P
    @Test
    @Tag("multiplayer")
    @DisplayName("Get Winners - all players are alive and there are only one winner")
    void testGetWinnersDeadButHigher() {
        var p0 = new Player();
        var p1 = new Player();
        var gameBoard = GameBoardUtils.createGameBoard(3, 4, (pos) -> {
            if (pos.equals(new Position(0, 3))) {
                return new EntityCell(pos, new Gem());
            } else if(pos.equals(new Position(0,2))){
                return new EntityCell(pos, new Gem());
            } else if (pos.equals(new Position(0, 0))) {
                return new StopCell(pos, p0);
            } else if (pos.equals(new Position(1, 0))) {
                return new EntityCell(pos, new Gem());
            } else if (pos.equals(new Position(1, 3))) {
                return new EntityCell(pos, new Mine());
            } else if (pos.equals(new Position(2, 3))) {
                return new StopCell(pos, p1);
            } else {
                return new EntityCell(pos);
            }
        });
        var controller = new GameController(
                new GameState(gameBoard, p0, 1),
                new GameState(gameBoard, p1, 1)
        );
        controller.processMove(Direction.RIGHT, p0.getId());
        controller.processMove(Direction.DOWN, p0.getId());
        controller.processMove(Direction.LEFT, p1.getId());
        controller.processMove(Direction.RIGHT, p1.getId());
        controller.processMove(Direction.LEFT, p1.getId());
        controller.processMove(Direction.UP, p1.getId());

        assertNotNull(controller.getWinners());
        assertThat(controller.getGameState(p0.getId()).getScore(), Matchers.greaterThan(
                controller.getGameState(p1.getId()).getScore()
        ));
        assertThat(controller.getWinners(), Matchers.allOf(
                Matchers.arrayWithSize(1),
                Matchers.arrayContaining(p1)
        ));
    }
}
