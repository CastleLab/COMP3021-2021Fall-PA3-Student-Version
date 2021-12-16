package hk.ust.cse.comp3021.pa3.controller;

import hk.ust.cse.comp3021.pa3.model.*;
import hk.ust.cse.comp3021.pa3.util.GameStateSerializer;
import hk.ust.cse.comp3021.pa3.util.TestUtils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.*;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class PositionMatcher extends TypeSafeMatcher<Entity> {
    final int row;
    final int col;

    PositionMatcher(int row, int col) {
        this.row = row;
        this.col = col;
    }

    static PositionMatcher notOnBoard() {
        return new PositionMatcher(-1, -1);
    }

    public static boolean atPosition(Entity entity, int row, int column) {
        return entity.getOwner() != null &&
                entity.getOwner().getPosition().row() == row &&
                entity.getOwner().getPosition().col() == column;
    }

    @Override
    protected boolean matchesSafely(Entity item) {
        return row >= 0 ? atPosition(item, row, col) : item.getOwner() == null;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(String.format("[%d, %d]", row, col));
    }
}

class GameConsistent {
    final GameController controller;

    int totalGems;

    GameConsistent(GameController controller) {
        this.controller = controller;
    }

    private int countTotalGems() {
        var total = 0;
        for (var gameState :
                controller.getGameStates()) {
            total += gameState.getNumGotGems();
        }
        total += controller.getGameStates()[0].getNumGems();
        return total;
    }

    public void beforeMutation() {
        totalGems = countTotalGems();
    }

    public void afterMutation() {
        // total number of gems stays the same
        assertEquals(totalGems, countTotalGems());
    }
}

public class GameConcurrencyTest {
    static class ThreadPlayer extends Thread {
        static ReentrantLock lock = new ReentrantLock();
        static CyclicBarrier startBarrier;
        static Set<ThreadPlayer> pool = new HashSet<>();

        static void reset() {
            pool.clear();
        }

        static void runAll() throws InterruptedException {
            startBarrier = new CyclicBarrier(pool.size());
            pool.parallelStream().forEach(Thread::start);
            for (var tp :
                    pool) {
                tp.join();
            }
        }

        private record DelayedMove(Duration takeTime, Direction direction) {
        }

        Player player;
        GameController controller;
        Duration runTime;
        Throwable lastException;

        List<DelayedMove> moves = new ArrayList<>();
        boolean randomMove = false;
        Duration timeout = Duration.ZERO;

        ThreadPlayer(GameController controller, Player player) {
            super();
            this.player = player;
            this.controller = controller;
            pool.add(this);
            this.setUncaughtExceptionHandler((t, e) -> lastException = e);
        }

        void addMove(Duration takeTime, Direction direction) {
            moves.add(new DelayedMove(takeTime, direction));
        }

        void addMove(Direction direction) {
            addMove(Duration.ZERO, direction);
        }

        int numGems() {
            var state = controller.getGameState(player.getId());
            return state.getNumGotGems();
        }

        void setRandomMove(boolean randomMove, Duration timeout) {
            this.randomMove = randomMove;
            this.timeout = timeout;
        }

        GameState gameState() {
            return controller.getGameState(player.getId());
        }

        @Override
        public void run() {
            try {
                startBarrier.await();

                var startT = Instant.now();
                if (!randomMove) {
                    for (var move :
                            this.moves) {
                        controller.processMove(move.direction(), player.getId());
                    }
                } else {
                    var shouldExit = new AtomicBoolean(false);
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            shouldExit.set(true);
                        }
                    }, timeout.toMillis());
                    while (!shouldExit.get()) {
                        var directions = new ArrayList<>(Arrays.asList(Direction.values()));
                        Collections.shuffle(directions);
                        Direction aliveDirection = null;
                        Direction deadDirection = null;
                        if (player.getOwner() == null) {
                            break;
                        }
                        var position = player.getOwner().getPosition();
                        for (var direction :
                                directions) {
                            lock.lock();
                            var result = gameState().getGameBoardController().tryMove(position, direction, player.getId());
                            if (result instanceof MoveResult.Valid.Alive) {
                                aliveDirection = direction;
                            } else if (result instanceof MoveResult.Valid.Dead) {
                                deadDirection = direction;
                            }
                            lock.unlock();
                        }
                        if (aliveDirection != null) {
                            controller.processMove(aliveDirection, player.getId());
                        } else if (deadDirection != null) {
                            controller.processMove(deadDirection, player.getId());
                        }
                        if (controller.getGameState(player.getId()).hasLost()) {
                            break;
                        }
                        lock.lock();
                        if (gameState().getNumGems() == 0) {
                            lock.unlock();
                            break;
                        }
                        lock.unlock();
                    }
                }
                var endT = Instant.now();
                this.runTime = Duration.between(startT, endT);
            } catch (InterruptedException | BrokenBarrierException ignored) {
            }
        }
    }

    static class Delay {
        static Duration oneSecond = Duration.ofSeconds(1);

        static Duration uniform(Duration min, Duration max) {
            var rnd = new Random();
            var ms = rnd.nextLong(min.toMillis(), max.toMillis());
            return Duration.ofMillis(ms);
        }

        static Duration gaussian(Duration mean, double std) {
            var rnd = new Random();
            var ms = (long) rnd.nextGaussian(mean.toMillis(), std);
            if (ms < 0) {
                ms = 0;
            }
            return Duration.ofMillis(ms);
        }

        static Duration gaussian(Duration mean) {
            return gaussian(mean, 1);
        }

        static Duration ofSeconds(double sec) {
            return Duration.ofMillis((long) (sec * 1000));
        }
    }

    private GameController loadGame(String resourcePath) throws FileNotFoundException {
        var resource = getClass().getResource(resourcePath);
        assert resource != null;
        var path = Path.of(resource.getPath());
        var states = GameStateSerializer.loadFrom(path);
        return new GameController(states);
    }

    private void assertPlayerPosition(ThreadPlayer player, int row, int column) {
        assert player != null;
        assertNotNull(player.player.getOwner());
        assertEquals(row, player.player.getOwner().getPosition().row());
        assertEquals(column, player.player.getOwner().getPosition().col());
    }

    @Tag("multithreading")
    @Test
    @DisplayName("Concurrency - players competing a gem should keep consistency")
    @Timeout(10)
    void testPlayersComputeConcurrently() {
        TestUtils.assertAlways(() -> {
            ThreadPlayer.reset();
            GameController controller = TestUtils.assumeDoesNotThrow(() -> loadGame("/maps/compete.multiplayer.game"));
            var players = controller.getPlayers();
            assert players.length == 4;
            var tP0 = new ThreadPlayer(controller, players[0]);
            assertPlayerPosition(tP0, 0, 1);
            tP0.addMove(Direction.DOWN);
            var tP1 = new ThreadPlayer(controller, players[1]);
            assertPlayerPosition(tP1, 1, 0);
            tP1.addMove(Direction.RIGHT);
            var tP2 = new ThreadPlayer(controller, players[2]);
            assertPlayerPosition(tP2, 1, 2);
            tP2.addMove(Direction.LEFT);
            var tP3 = new ThreadPlayer(controller, players[3]);
            assertPlayerPosition(tP3, 2, 1);
            tP3.addMove(Direction.UP);

            ThreadPlayer.runAll();

            assertNull(tP0.lastException);
            assertNull(tP1.lastException);

            var totalGems = tP0.numGems() + tP1.numGems() + tP2.numGems() + tP3.numGems();
            assertEquals(1, totalGems);
            assertNotNull(tP0.player.getOwner());
            assertNotNull(tP1.player.getOwner());
            assertNotNull(tP2.player.getOwner());
            assertNotNull(tP3.player.getOwner());
            var tps = new ThreadPlayer[]{tP0, tP1, tP2, tP3};
            for (int i = 0; i < 4; i++) {
                var tp = tps[i];
                if (tp.numGems() == 0)
                    switch (i) {
                        case 0 -> assertPlayerPosition(tp, 0, 1);
                        case 1 -> assertPlayerPosition(tp, 1, 0);
                        case 2 -> assertPlayerPosition(tp, 1, 2);
                        case 3 -> assertPlayerPosition(tp, 2, 1);
                        default -> assertTrue(true);
                    }
                else
                    assertPlayerPosition(tp, 1, 1);
            }
        }, 50, 1000);


    }

    @Tag("multithreading")
    @Test
    @DisplayName("Concurrency - one player move to block the other.")
    @Timeout(2)
    void testPlayersInterfereConcurrently() throws FileNotFoundException, InterruptedException {
        TestUtils.assertAlways(() -> {
            ThreadPlayer.reset();
            var controller = TestUtils.assumeDoesNotThrow(() -> loadGame("/maps/interfere.multiplayer.game"));
            var players = controller.getPlayers();
            assert players.length == 2;
            var tP0 = new ThreadPlayer(controller, players[0]);
            assertPlayerPosition(tP0, 0, 0);
            tP0.addMove(Direction.RIGHT);
            var tP1 = new ThreadPlayer(controller, players[1]);
            assertPlayerPosition(tP1, 1, 2);
            tP1.addMove(Direction.UP);

            var checker = new GameConsistent(controller);
            checker.beforeMutation();
            ThreadPlayer.runAll();
            checker.afterMutation();

            assertNull(tP0.lastException);
            assertNull(tP1.lastException);

            assertThat(tP0.player,
                    either(new PositionMatcher(0, 1))
                            .or(new PositionMatcher(0, 3))
            );
            assertThat(tP1.player,
                    new PositionMatcher(0, 2));
        }, 50, 1000);
    }

    @Tag("multithreading")
    @Test
    @DisplayName("Concurrency - two players collide with each other")
    @Timeout(2)
    void testPlayersCollideConcurrently() throws InterruptedException, FileNotFoundException {
        TestUtils.assertAlways(() -> {
            ThreadPlayer.reset();
            var controller = TestUtils.assumeDoesNotThrow(() -> loadGame("/maps/collide.multiplayer.game"));
            var players = controller.getPlayers();
            assert players.length == 2;

            var tP0 = new ThreadPlayer(controller, players[0]);
            assertThat(tP0.player, new PositionMatcher(0, 0));
            tP0.addMove(Direction.RIGHT);
            var tP1 = new ThreadPlayer(controller, players[1]);
            assertThat(tP1.player, new PositionMatcher(0, 4));
            tP1.addMove(Direction.LEFT);

            var checker = new GameConsistent(controller);
            checker.beforeMutation();
            ThreadPlayer.runAll();
            checker.afterMutation();

            assertNull(tP0.lastException);
            assertNull(tP1.lastException);

            assertThat(tP0.player, either(new PositionMatcher(0, 0))
                    .or(new PositionMatcher(0, 3)));
            assertThat(tP1.player, either(new PositionMatcher(0, 1))
                    .or(new PositionMatcher(0, 4)));
            if (PositionMatcher.atPosition(tP0.player, 0, 0)) {
                assertThat(tP1.player, new PositionMatcher(0, 1));
            } else {
                assertThat(tP1.player, new PositionMatcher(0, 4));
            }
        }, 50, 1000);
    }

    @Tag("multithreading")
    @Test
    @DisplayName("Concurrency - two players die together")
    @Timeout(2)
    void testPlayersDieTogether() throws FileNotFoundException, InterruptedException {
        TestUtils.assertAlways(() -> {
            ThreadPlayer.reset();
            var controller = TestUtils.assumeDoesNotThrow(() -> loadGame("/maps/die_together.multiplayer.game"));
            var players = controller.getPlayers();
            assert players.length == 2;

            var tP0 = new ThreadPlayer(controller, players[0]);
            tP0.addMove(Delay.oneSecond, Direction.RIGHT);
            var tP1 = new ThreadPlayer(controller, players[1]);
            tP1.addMove(Delay.oneSecond, Direction.UP);

            assertThat(tP0.player, new PositionMatcher(0, 0));
            assertThat(tP0.gameState().getNumDeaths(), equalTo(0));
            assertThat(tP0.gameState().getNumLives(), equalTo(2));
            assertThat(tP1.player, new PositionMatcher(2, 2));
            assertThat(tP1.gameState().getNumDeaths(), equalTo(0));
            assertThat(tP1.gameState().getNumLives(), equalTo(2));

            var checker = new GameConsistent(controller);
            checker.beforeMutation();
            ThreadPlayer.runAll();
            checker.afterMutation();

            assertNull(tP0.lastException);
            assertNull(tP1.lastException);

            assertThat(tP0.player, new PositionMatcher(0, 0));
            assertThat(tP0.gameState().getNumDeaths(), equalTo(1));
            assertThat(tP0.gameState().getNumLives(), equalTo(1));
            assertThat(tP1.player, new PositionMatcher(2, 2));
            assertThat(tP1.gameState().getNumDeaths(), equalTo(1));
            assertThat(tP1.gameState().getNumLives(), equalTo(1));
        }, 50, 1000);
    }

    @Tag("multithreading")
    @Test
    @DisplayName("Concurrency - two players die concurrently")
    @Timeout(2)
    void testPlayersDieConcurrently() {
        TestUtils.assertAlways(() -> {
            ThreadPlayer.reset();
            var controller = TestUtils.assumeDoesNotThrow(() -> loadGame("/maps/die_concurrently.multiplayer.game"));
            var players = controller.getPlayers();
            assert players.length == 2;

            var tP0 = new ThreadPlayer(controller, players[0]);
            tP0.addMove(Delay.oneSecond, Direction.RIGHT);
            var tP1 = new ThreadPlayer(controller, players[1]);
            tP1.addMove(Delay.oneSecond, Direction.UP);

            assertThat(tP0.player, new PositionMatcher(0, 0));
            assertThat(tP0.gameState().getNumDeaths(), equalTo(0));
            assertThat(tP0.gameState().getNumLives(), equalTo(1));
            assertThat(tP1.player, new PositionMatcher(2, 2));
            assertThat(tP1.gameState().getNumDeaths(), equalTo(0));
            assertThat(tP1.gameState().getNumLives(), equalTo(1));

            var checker = new GameConsistent(controller);
            checker.beforeMutation();
            ThreadPlayer.runAll();
            checker.afterMutation();

            assertNull(tP0.lastException);
            assertNull(tP1.lastException);

            assertThat(tP0.player, PositionMatcher.notOnBoard());
            assertThat(tP0.gameState().getNumDeaths(), equalTo(1));
            assertThat(tP0.gameState().getNumLives(), equalTo(0));
            assertThat(tP1.player, PositionMatcher.notOnBoard());
            assertThat(tP1.gameState().getNumDeaths(), equalTo(1));
            assertThat(tP1.gameState().getNumLives(), equalTo(0));
        }, 50, 1000);
    }

    @Tag("multithreading")
    @Test
    @DisplayName("Concurrency - two players move cross")
    @Timeout(2)
    void testPlayersMoveCross() throws FileNotFoundException, InterruptedException {
        TestUtils.assertAlways(() -> {
            ThreadPlayer.reset();
            var controller = TestUtils.assumeDoesNotThrow(() -> loadGame("/maps/cross.multiplayer.game"));
            var players = controller.getPlayers();
            assert players.length == 2;

            var tP0 = new ThreadPlayer(controller, players[0]);
            tP0.addMove(Direction.RIGHT);
            var tP1 = new ThreadPlayer(controller, players[1]);
            tP1.addMove(Direction.UP);

            var checker = new GameConsistent(controller);
            checker.beforeMutation();
            ThreadPlayer.runAll();
            checker.afterMutation();

            assertNull(tP0.lastException);
            assertNull(tP1.lastException);
        }, 50, 1000);
    }

    @Test
    @Tag("multithreading")
    @Timeout(6)
    void testRandomPlay() {
        TestUtils.assertAlways(() -> {
            ThreadPlayer.reset();
            var controller = TestUtils.assumeDoesNotThrow(() -> loadGame("/maps/05.multiplayer.game"));
            var players = controller.getPlayers();
            assert players.length == 2;

            var tP0 = new ThreadPlayer(controller, players[0]);
            tP0.setRandomMove(true, Duration.ofSeconds(1));
            var tP1 = new ThreadPlayer(controller, players[1]);
            tP1.setRandomMove(true, Duration.ofSeconds(1));

            var checker = new GameConsistent(controller);
            checker.beforeMutation();
            ThreadPlayer.runAll();
            checker.afterMutation();

            assertNull(tP0.lastException);
            assertNull(tP1.lastException);
        }, 50, 5000);
    }
}
