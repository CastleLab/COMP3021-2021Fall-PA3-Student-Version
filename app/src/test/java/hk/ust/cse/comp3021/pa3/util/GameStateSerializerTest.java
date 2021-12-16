package hk.ust.cse.comp3021.pa3.util;

import hk.ust.cse.comp3021.pa3.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class GameStateSerializerTest {

    private BufferedReader reader = null;
    private BufferedWriter writer = null;

    @Test
    @Tag("sanity")
    @DisplayName("Sanity Test - Public Constructors")
    void testConstructors() {
        final var clazz = GameStateSerializer.class;
        final var ctors = ReflectionUtils.getPublicConstructors(clazz);

        assertEquals(0, ctors.length);
    }

    @Test
    @Tag("sanity")
    @DisplayName("Sanity Test - Public Methods")
    void testPublicMethods() {
        final var clazz = GameStateSerializer.class;
        final var instanceMethods = ReflectionUtils.getPublicInstanceMethods(clazz);

        assertEquals(0, instanceMethods.length);

        final var staticMethods = ReflectionUtils.getPublicStaticMethods(clazz);
        assertEquals(2, staticMethods.length);

        assertDoesNotThrow(() -> clazz.getMethod("loadFrom", Path.class));
        assertDoesNotThrow(() -> clazz.getMethod("writeTo", GameState.class, Path.class));
    }

    @Test
    @Tag("sanity")
    @DisplayName("Sanity Test - Public Fields")
    void testPublicFields() {
        final var clazz = GameStateSerializer.class;

        assertEquals(0, ReflectionUtils.getPublicInstanceFields(clazz).length);
        assertEquals(0, ReflectionUtils.getPublicStaticFields(clazz).length);
    }

    // P..
    // XWL
    // .*#
    @Test
    @Tag("sanity")
    @DisplayName("Serialization Test - Unlimited Lives")
    void testSerializeUnlimitedLives() {
        final var cells = GameBoardUtils.createEmptyCellArray(3, 3, (pos) -> {
            if (pos.equals(new Position(0, 0))) {
                return new EntityCell(pos, new Player());
            } else if (pos.equals(new Position(1, 0))) {
                return new EntityCell(pos, new Mine());
            } else if (pos.equals(new Position(1, 1))) {
                return new Wall(pos);
            } else if (pos.equals(new Position(1, 2))) {
                return new EntityCell(pos, new ExtraLife());
            } else if (pos.equals(new Position(2, 1))) {
                return new EntityCell(pos, new Gem());
            } else if (pos.equals(new Position(2, 2))) {
                return new StopCell(pos);
            } else {
                return new EntityCell(pos);
            }
        });

        final var gameBoard = new GameBoard(3, 3, cells);
        final var gameState = new GameState(gameBoard);

        final var strWriter = new StringWriter();
        try (var writer = new BufferedWriter(strWriter)) {
            GameStateSerializer.writeTo(gameState, writer);
        } catch (final IOException e) {
            fail(e);
        }

        final var expected = String.join(System.lineSeparator(), "3", "3", "", "P..", "MWL", ".GS")
                + System.lineSeparator();

        assertEquals(expected, strWriter.toString());
    }

    @Test
    @Tag("sanity")
    @DisplayName("Serialization Test - Limited Lives")
    void testSerializeLimitedLives() {
        final var cells = GameBoardUtils.createEmptyCellArray(3, 3, (pos) -> {
            if (pos.equals(new Position(0, 0))) {
                return new EntityCell(pos, new Player());
            } else if (pos.equals(new Position(1, 0))) {
                return new EntityCell(pos, new Mine());
            } else if (pos.equals(new Position(1, 1))) {
                return new Wall(pos);
            } else if (pos.equals(new Position(1, 2))) {
                return new EntityCell(pos, new ExtraLife());
            } else if (pos.equals(new Position(2, 1))) {
                return new EntityCell(pos, new Gem());
            } else if (pos.equals(new Position(2, 2))) {
                return new StopCell(pos);
            } else {
                return new EntityCell(pos);
            }
        });

        final var gameBoard = new GameBoard(3, 3, cells);
        final var gameState = new GameState(gameBoard, 10);

        final var strWriter = new StringWriter();
        try (var writer = new BufferedWriter(strWriter)) {
            GameStateSerializer.writeTo(gameState, writer);
        } catch (final IOException e) {
            fail(e);
        }

        final var expected = String.join(System.lineSeparator(), "3", "3", "10", "P..", "MWL", ".GS")
                + System.lineSeparator();

        assertEquals(expected, strWriter.toString());
    }

    @Test
    @Tag("sanity")
    @DisplayName("Deserialization Test - Unlimited Lives")
    void testDeserializeUnlimitedLives() {
        final var source = String.join(System.lineSeparator(), "3", "3", "", "P..", "MWL", ".GS");

        final GameState[] gameStates;
        try (var reader = new BufferedReader(new StringReader(source))) {
            gameStates = GameStateSerializer.loadFrom(reader);
        } catch (IOException e) {
            fail(e);
            throw new AssertionError();
        }

        assertEquals(1, gameStates.length);
        var gameState = gameStates[0];
        assertTrue(gameState.hasUnlimitedLives());

        final var gameBoard = gameState.getGameBoard();
        assertEquals(3, gameBoard.getNumRows());
        assertEquals(3, gameBoard.getNumCols());

        assertTrue(gameBoard.getCell(0, 0) instanceof StopCell);
        assertTrue(((StopCell) gameBoard.getCell(0, 0)).getEntity() instanceof Player);

        assertTrue(gameBoard.getCell(0, 1) instanceof EntityCell);
        assertNull(((EntityCell) gameBoard.getCell(0, 1)).getEntity());

        assertTrue(gameBoard.getCell(0, 2) instanceof EntityCell);
        assertNull(((EntityCell) gameBoard.getCell(0, 2)).getEntity());

        assertTrue(gameBoard.getCell(1, 0) instanceof EntityCell);
        assertTrue(((EntityCell) gameBoard.getCell(1, 0)).getEntity() instanceof Mine);

        assertTrue(gameBoard.getCell(1, 1) instanceof Wall);

        assertTrue(gameBoard.getCell(1, 2) instanceof EntityCell);
        assertTrue(((EntityCell) gameBoard.getCell(1, 2)).getEntity() instanceof ExtraLife);

        assertTrue(gameBoard.getCell(2, 0) instanceof EntityCell);
        assertNull(((EntityCell) gameBoard.getCell(2, 0)).getEntity());

        assertTrue(gameBoard.getCell(2, 1) instanceof EntityCell);
        assertTrue(((EntityCell) gameBoard.getCell(2, 1)).getEntity() instanceof Gem);

        assertTrue(gameBoard.getCell(2, 2) instanceof StopCell);
        assertNull(((StopCell) gameBoard.getCell(2, 2)).getEntity());
    }

    @Test
    @Tag("sanity")
    @DisplayName("Deserialization Test - Limited Lives")
    void testDeserializeLimitedLives() {
        final var source = String.join(System.lineSeparator(), "3", "3", "10", "P..", "MWL", ".GS");

        final GameState[] gameStates;
        try (var reader = new BufferedReader(new StringReader(source))) {
            gameStates = GameStateSerializer.loadFrom(reader);
        } catch (IOException e) {
            fail(e);
            throw new AssertionError();
        }

        assertEquals(1, gameStates.length);
        var gameState = gameStates[0];
        assertEquals(10, gameState.getNumLives());

        final var gameBoard = gameState.getGameBoard();
        assertEquals(3, gameBoard.getNumRows());
        assertEquals(3, gameBoard.getNumCols());

        assertTrue(gameBoard.getCell(0, 0) instanceof StopCell);
        assertTrue(((StopCell) gameBoard.getCell(0, 0)).getEntity() instanceof Player);

        assertTrue(gameBoard.getCell(0, 1) instanceof EntityCell);
        assertNull(((EntityCell) gameBoard.getCell(0, 1)).getEntity());

        assertTrue(gameBoard.getCell(0, 2) instanceof EntityCell);
        assertNull(((EntityCell) gameBoard.getCell(0, 2)).getEntity());

        assertTrue(gameBoard.getCell(1, 0) instanceof EntityCell);
        assertTrue(((EntityCell) gameBoard.getCell(1, 0)).getEntity() instanceof Mine);

        assertTrue(gameBoard.getCell(1, 1) instanceof Wall);

        assertTrue(gameBoard.getCell(1, 2) instanceof EntityCell);
        assertTrue(((EntityCell) gameBoard.getCell(1, 2)).getEntity() instanceof ExtraLife);

        assertTrue(gameBoard.getCell(2, 0) instanceof EntityCell);
        assertNull(((EntityCell) gameBoard.getCell(2, 0)).getEntity());

        assertTrue(gameBoard.getCell(2, 1) instanceof EntityCell);
        assertTrue(((EntityCell) gameBoard.getCell(2, 1)).getEntity() instanceof Gem);

        assertTrue(gameBoard.getCell(2, 2) instanceof StopCell);
        assertNull(((StopCell) gameBoard.getCell(2, 2)).getEntity());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (reader != null) {
            reader.close();
            reader = null;
        }
        if (writer != null) {
            writer.close();
            writer = null;
        }
    }
}
