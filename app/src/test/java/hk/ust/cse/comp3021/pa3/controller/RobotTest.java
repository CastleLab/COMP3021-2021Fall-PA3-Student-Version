package hk.ust.cse.comp3021.pa3.controller;

import hk.ust.cse.comp3021.pa3.model.*;
import hk.ust.cse.comp3021.pa3.util.GameBoardUtils;
import hk.ust.cse.comp3021.pa3.util.Robot;
import hk.ust.cse.comp3021.pa3.util.TimeIntervalGenerator;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;

public class RobotTest extends ApplicationTest {
    Robot robot0;
    Robot robot1;
    GameController gameController;

    // Map:
    // PW.G
    // ....
    // S..W
    // ...P
    @BeforeEach
    void setup() {
        var p0 = new Player();
        var p1 = new Player();
        var gameBoard = GameBoardUtils.createGameBoard(4, 4, (pos) -> {
            if (pos.equals(new Position(0, 0))) {
                return new StopCell(pos, p0);
            } else if (pos.equals(new Position(0, 1))) {
                return new Wall(pos);
            } else if (pos.equals(new Position(0, 3))) {
                return new EntityCell(pos, new Gem());
            } else if (pos.equals(new Position(2, 0))) {
                return new StopCell(pos);
            } else if (pos.equals(new Position(2, 3))) {
                return new Wall(pos);
            } else if (pos.equals(new Position(3, 3))) {
                return new StopCell(pos, p1);
            } else {
                return new EntityCell(pos);
            }
        });
        gameController = new GameController(
                new GameState(gameBoard, p0, 1),
                new GameState(gameBoard, p1, 1)
        );
        robot0 = new Robot(gameController.getGameStates()[0], Robot.Strategy.Random);
        robot1 = new Robot(gameController.getGameStates()[1], Robot.Strategy.Smart);
        Robot.timeIntervalGenerator = TimeIntervalGenerator.veryFast();
    }

    @Test
    @Tag("robot")
    @Timeout(2)
    @DisplayName("Robot should start a new thread")
    void testNewThread() throws InterruptedException {
        var moveCount = new AtomicInteger(0);
        Robot.timeIntervalGenerator = TimeIntervalGenerator.expectedMilliseconds(200);
        robot0.startDelegation(direction -> {
            moveCount.incrementAndGet();
        });
        // robot's new thread does not have time to move 2 times.
        assertThat(moveCount.get(), Matchers.lessThan(2));
        Thread.sleep(500);
        // robot's new thread should have moved 2 times but no more than 4 times.
        assertThat(moveCount.get(), Matchers.allOf(
                Matchers.greaterThan(1),
                Matchers.lessThanOrEqualTo(4)
        ));
    }

    @Test
    @Tag("robot")
    @Timeout(2)
    @DisplayName("Thread should already stopped when the stopDelegation method returns")
    void testStopDelegation() throws InterruptedException {
        var moveCount = new AtomicInteger(0);
        robot0.startDelegation(direction -> {
            moveCount.incrementAndGet();
        });
        robot0.stopDelegation();
        var stopCount = moveCount.get();
        Thread.sleep(1000);
        assertThat(stopCount, Matchers.equalTo(moveCount.get()));
    }

    @Test
    @Tag("robot")
    @Timeout(2)
    @DisplayName("Robot should move with time interval")
    void testTimeInterval() throws InterruptedException {
        Robot.timeIntervalGenerator = TimeIntervalGenerator.expectedMilliseconds(200);
        var moveCount = new AtomicInteger(0);
        robot0.startDelegation(direction -> moveCount.incrementAndGet());
        Thread.sleep(500);
        robot0.stopDelegation();
        assertThat(moveCount.get(), Matchers.allOf(
                Matchers.greaterThanOrEqualTo(2),
                Matchers.lessThanOrEqualTo(4)
        ));
    }
}
