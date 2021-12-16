package hk.ust.cse.comp3021.pa3.model;

import hk.ust.cse.comp3021.pa3.util.ReflectionUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

public class DirectionTest {

    @Test
    @Tag("sanity")
    @DisplayName("Sanity Test - Class Is Enum")
    void testClassIsEnum() {
        assertTrue(Direction.class.isEnum());
    }

    @Test
    @Tag("sanity")
    @DisplayName("Sanity Test - Enum contains Four Values")
    void testEnumContainsFourValues() {
        assertEquals(4, Direction.class.getEnumConstants().length);
    }

    @Test
    @Tag("sanity")
    @DisplayName("Sanity Test - Public Methods")
    void testPublicMethods() {
        final var clazz = Direction.class;
        final var publicMethods = ReflectionUtils.getPublicInstanceMethods(clazz);

        assertEquals(3, publicMethods.length);

        assertDoesNotThrow(() -> clazz.getDeclaredMethod("getOffset"));
        assertDoesNotThrow(() -> clazz.getDeclaredMethod("getRowOffset"));
        assertDoesNotThrow(() -> clazz.getDeclaredMethod("getColOffset"));
    }

    @Test
    @Tag("sanity")
    @DisplayName("Sanity Test - Public Fields")
    void testPublicFields() {
        final var clazz = Direction.class;
        final var publicFields = ReflectionUtils.getPublicInstanceFields(clazz);

        assertEquals(0, publicFields.length);
    }

    @ParameterizedTest
    @Tag("sanity")
    @EnumSource(Direction.class)
    @DisplayName("getOffset - Test correct offset values")
    void testDirectionOffset(final Direction direction) {
        final var actual = direction.getOffset();

        switch (direction) {
            case UP -> {
                assertEquals(-1, actual.dRow());
                assertEquals(0, actual.dCol());
            }
            case DOWN -> {
                assertEquals(1, actual.dRow());
                assertEquals(0, actual.dCol());
            }
            case LEFT -> {
                assertEquals(0, actual.dRow());
                assertEquals(-1, actual.dCol());
            }
            case RIGHT -> {
                assertEquals(0, actual.dRow());
                assertEquals(1, actual.dCol());
            }
            default -> assertTrue(true);
        }
    }

    @ParameterizedTest
    @Tag("sanity")
    @EnumSource(Direction.class)
    @DisplayName("Sanity Test - Test correct row offset values")
    void testDirectionRowOffset(final Direction direction) {
        final var actual = direction.getRowOffset();

        if (direction == Direction.UP) {
            assertEquals(-1, actual);
        } else if (direction == Direction.DOWN) {
            assertEquals(1, actual);
        } else {
            assertEquals(0, actual);
        }
    }

    @ParameterizedTest
    @Tag("sanity")
    @EnumSource(Direction.class)
    @DisplayName("Sanity Test - Test correct column offset values")
    void testDirectionColOffset(final Direction direction) {
        final var actual = direction.getColOffset();

        if (direction == Direction.LEFT) {
            assertEquals(-1, actual);
        } else if (direction == Direction.RIGHT) {
            assertEquals(1, actual);
        } else {
            assertEquals(0, actual);
        }
    }
}
