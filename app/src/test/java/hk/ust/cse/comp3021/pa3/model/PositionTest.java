package hk.ust.cse.comp3021.pa3.model;

import hk.ust.cse.comp3021.pa3.util.ReflectionUtils;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class PositionTest {

    private Position position = null;

    @Test
    @Tag("sanity")
    @Disabled
    @DisplayName("Sanity Test - Class Is Record")
    void testClassIsRecord() {
        assertFalse(Position.class.isRecord());
    }

    @Test
    @Tag("sanity")
    @DisplayName("Sanity Test - Public Constructors")
    void testConstructors() {
        final var clazz = Position.class;
        final var ctors = ReflectionUtils.getPublicConstructors(clazz);

        assertEquals(1, ctors.length);

        assertDoesNotThrow(() -> clazz.getConstructor(int.class, int.class));
    }

    @Test
    @Tag("sanity")
    @DisplayName("Sanity Test - Public Methods")
    void testPublicMethods() {
        final var clazz = Position.class;
        final var publicMethods = ReflectionUtils.getPublicInstanceMethods(clazz);

        // Records have numFields + 3 (equals, hashCode, toString) public non-static declared methods
        assertEquals(9, publicMethods.length);

        assertDoesNotThrow(() -> clazz.getMethod("offsetBy", int.class, int.class));
        assertDoesNotThrow(() -> clazz.getMethod("offsetBy", PositionOffset.class));
        assertDoesNotThrow(() -> clazz.getMethod("offsetByOrNull", int.class, int.class, int.class, int.class));
        assertDoesNotThrow(() -> clazz.getMethod("offsetByOrNull", PositionOffset.class, int.class, int.class));
    }

    @Test
    @Tag("sanity")
    @DisplayName("Sanity Test - Public Fields")
    void testPublicFields() {
        final var clazz = Position.class;
        final var publicFields = ReflectionUtils.getPublicInstanceFields(clazz);

        assertEquals(0, publicFields.length);
    }

    @Test
    @Tag("sanity")
    @DisplayName("Sanity Test - Creation with Valid Values")
    void testCreation() {
        assertDoesNotThrow(() -> position = new Position(0, 0));
    }

    @Test
    @Tag("sanity")
    @DisplayName("Sanity Test - Creation with Invalid Values")
    void testCreationFailure() {
        assertThrows(IllegalArgumentException.class, () -> position = new Position(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> position = new Position(0, -1));
        assertThrows(IllegalArgumentException.class, () -> position = new Position(-1, -1));
    }

    @Test
    @Tag("sanity")
    @DisplayName("Get Offset - int-overload")
    void testOffsetByInts() {
        final var origPosition = new Position(3, 5);
        position = origPosition.offsetBy(1, -3);

        assertEquals(4, position.row());
        assertEquals(2, position.col());
    }

    @Test
    @Tag("sanity")
    @DisplayName("Get Offset - PositionOffset-overload")
    void testOffsetByPosOffset() {
        final var origPosition = new Position(3, 5);
        final var offset = new PositionOffset(1, -3);
        position = origPosition.offsetBy(offset);

        assertEquals(4, position.row());
        assertEquals(2, position.col());
    }

    @AfterEach
    void tearDown() {
        position = null;
    }
}
