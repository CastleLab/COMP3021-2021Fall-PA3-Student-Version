package hk.ust.cse.comp3021.pa3.controller;

import hk.ust.cse.comp3021.pa3.util.GameStateSerializer;
import hk.ust.cse.comp3021.pa3.util.Robot;
import hk.ust.cse.comp3021.pa3.util.TestUtils;
import hk.ust.cse.comp3021.pa3.util.TimeIntervalGenerator;
import org.junit.jupiter.api.*;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public class StrategyTest {
    private GameController loadGame(String resourcePath) throws FileNotFoundException {
        var resource = getClass().getResource(resourcePath);
        assert resource != null;
        var path = Path.of(resource.getPath());
        var states = GameStateSerializer.loadFrom(path);
        return new GameController(states);
    }

    @BeforeEach
    void setup() {
        Robot.timeIntervalGenerator = TimeIntervalGenerator.veryFast();
    }

    @Test
    @Tag("robot")
    @Timeout(60)
    @DisplayName("Smart strategy of robot should win against random in most times")
    public void testSmartRobotWins() {
        var i = new AtomicInteger(0);
        TestUtils.assertAlways(() -> {
            var repeat = 11;
            TestUtils.assertMostly(() -> {
                int index = i.getAndIncrement();
                System.out.println("start: " + index);
                try {
                    var controller = loadGame("/maps/05.multiplayer.game");
                    var state0 = controller.getGameStates()[0];
                    var state1 = controller.getGameStates()[1];
                    var r0 = new Robot(state0, Robot.Strategy.Random);
                    var r1 = new Robot(state1, Robot.Strategy.Smart);
                    r0.startDelegation(direction -> controller.processMove(direction, state0.getPlayer().getId()));
                    r1.startDelegation(direction -> controller.processMove(direction, state1.getPlayer().getId()));
                    while (true) {
                        // busy waiting until the game finishes
                        Thread.sleep(50);
                        var winners = controller.getWinners();
                        if (winners != null) {
                            r0.stopDelegation();
                            r1.stopDelegation();
                            var bool = winners.length == 1 && winners[0].getId() == state1.getPlayer().getId();
                            System.out.println("stop: " + index + bool);
                            return bool;
                        }
                    }
                } catch (FileNotFoundException | InterruptedException ignored) {
                    System.out.println("stop: " + index + false);
                    return false;
                }
            }, repeat, 10000, false);
        }, 5, 12000, false);
    }
}

