package hk.ust.cse.comp3021.pa3.util;

import hk.ust.cse.comp3021.pa3.model.Direction;
import hk.ust.cse.comp3021.pa3.model.GameState;
import hk.ust.cse.comp3021.pa3.model.MoveResult;
import hk.ust.cse.comp3021.pa3.model.Position;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * The Robot is an automated worker that can delegate the movement control of a player.
 * <p>
 * It implements the {@link MoveDelegate} interface and
 * is used by {@link hk.ust.cse.comp3021.pa3.view.panes.GameControlPane#delegateControl(MoveDelegate)}.
 */
public class Robot implements MoveDelegate {
    public enum Strategy {
        Random, Smart
    }

    /**
     * A generator to get the time interval before the robot makes the next move.
     */
    public static Generator<Long> timeIntervalGenerator = TimeIntervalGenerator.everySecond();

    /**
     * e.printStackTrace();
     * The game state of thee.printStackTrace(); player that the robot delegates.
     */
    private final GameState gameState;

    /**
     * The strategy of this instance of robot.
     */
    private final Strategy strategy;

    private final AtomicBoolean shouldStop;
    private final AtomicBoolean running;

    public Robot(GameState gameState) {
        this(gameState, Strategy.Smart);
    }

    public Robot(GameState gameState, Strategy strategy) {
        this.strategy = strategy;
        this.gameState = gameState;
        this.running = new AtomicBoolean(false);
        this.shouldStop = new AtomicBoolean(false);
    }

    /**
     * TODO Start the delegation in a new thread.
     * The delegation should run in a separate thread.
     * This method should return immediately when the thread is started.
     * <p>
     * In the delegation of the control of the player,
     * the time interval between moves should be obtained from {@link Robot#timeIntervalGenerator}.
     * That is to say, the new thread should:
     * <ol>
     *   <li>Stop all existing threads by calling {@link Robot#stopDelegation()}</li>
     *   <li>Wait for some time (obtained from {@link TimeIntervalGenerator#next()}</li>
     *   <li>Make a move, call {@link Robot#makeMoveRandomly(MoveProcessor)} or
     *   {@link Robot#makeMoveSmartly(MoveProcessor)} according to {@link Robot#strategy}</li>
     *   <li>goto 2</li>
     * </ol>
     * The thread should be able to exit when {@link Robot#stopDelegation()} is called.
     * <p>
     *
     * @param processor The processor to make movements.
     */
    @Override
    public void startDelegation(@NotNull MoveProcessor processor) {
        stopDelegation();
        running.set(true);
        shouldStop.set(false);
        var thread = new Thread(() -> {
            var timer = new Timer();
            while (!shouldStop.get()) {
                var interval = timeIntervalGenerator.next();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (strategy == Strategy.Random) {
                            makeMoveRandomly(processor);
                        } else if (strategy == Strategy.Smart) {
                            makeMoveSmartly(processor);
                        }
                        synchronized (timer) {
                            timer.notify();
                        }
                    }
                }, interval);
                synchronized (timer) {
                    try {
                        timer.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            running.set(false);
            synchronized (running) {
                timer.cancel();
                running.notify();
            }
        });
        thread.start();
    }

    /**
     * TODO Stop the delegations, i.e., stop the thread of this instance.
     * When this method returns, the thread must have exited already.
     */
    @Override
    public void stopDelegation() {
        shouldStop.set(true);
        synchronized (running) {
            while (running.get()) {
                try {
                    running.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private MoveResult tryMove(Direction direction) {
        var player = gameState.getPlayer();
        if (player.getOwner() == null) {
            return null;
        }
        return gameState.getGameBoardController().tryMove(player.getOwner().getPosition(), direction, player.getId());
    }

    private MoveResult tryMove(Position from, Direction direction) {
        var player = gameState.getPlayer();
        if (player.getOwner() == null) {
            return null;
        }
        return gameState.getGameBoardController().tryMove(from, direction, player.getId());
    }

    /**
     * The robot moves randomly but rationally,
     * which means the robot will not move to a direction that will make the player die if there are other choices,
     * but for other non-dying directions, the robot just randomly chooses one.
     * If there is no choice but only have one dying direction to move, the robot will still choose it.
     * If there is no valid direction, i.e. can neither die nor move, the robot do not perform a move.
     * <p>
     * TODO modify this method if you need to do thread synchronization.
     *
     * @param processor The processor to make movements.
     */
    private void makeMoveRandomly(MoveProcessor processor) {
        var directions = new ArrayList<>(Arrays.asList(Direction.values()));
        Collections.shuffle(directions);
        Direction aliveDirection = null;
        Direction deadDirection = null;
        var lock = gameState.getGameBoard().getLock();
        lock.lock();
        for (var direction :
                directions) {
            var result = tryMove(direction);
            if (result instanceof MoveResult.Valid.Alive) {
                aliveDirection = direction;
            } else if (result instanceof MoveResult.Valid.Dead) {
                deadDirection = direction;
            }
        }
        lock.unlock();
        if (aliveDirection != null) {
            processor.move(aliveDirection);
        } else if (deadDirection != null) {
            processor.move(deadDirection);
        }
    }

    /**
     * TODO implement this method
     * The robot moves with a smarter strategy compared to random.
     * This strategy is expected to beat random strategy in most of the time.
     * That is to say we will let random robot and smart robot compete with each other and repeat many (>10) times
     * (10 seconds timeout for each run).
     * You will get the grade if the robot with your implementation can win in more than half of the total runs
     * (e.g., at least 6 when total is 10).
     * <p>
     *
     * @param processor The processor to make movements.
     */
    private void makeMoveSmartly(MoveProcessor processor) {
        // smart strategy uses a simple greedy algorithm to find next local optimal move.
        var lock = gameState.getGameBoard().getLock();
        lock.lock();
        var optimalMove = Arrays.stream(Direction.values())
                .max(Comparator.comparing(this::fitness));
        lock.unlock();
        if (optimalMove.isPresent()) {
            processor.move(optimalMove.get());
        } else {
            this.makeMoveRandomly(processor);
        }
    }

    private double fitness(Direction direction) {
        var r = tryMove(direction);
        if (r instanceof MoveResult.Valid.Alive aliveR) {// get the paths to all gems after move
            var paths = pathToAllGems(aliveR.newPosition);
            // calculate the fitness of the move
            int fitness = paths.values().stream()
                    .map(List::size)
                    .reduce(0, Integer::sum);
            if (fitness == 0) {
                return aliveR.collectedGems.size();
            }
            return aliveR.collectedGems.size() + (double) paths.size() / fitness;
        } else if (r instanceof MoveResult.Invalid) {
            return 0;
        } else if (r instanceof MoveResult.Valid.Dead) {
            return -1;
        } else if (r instanceof MoveResult.Valid.KickedOut) {
            return -2;
        }
        return Integer.MIN_VALUE;
    }

    /**
     * Fitness is defined as the sum of the distances between current player
     * Distance of two position is the minimal number of steps to get from one position to another.
     *
     * @return the distance value
     */
    private Map<Position, List<Direction>> pathToAllGems(Position from) {
        var visited = new ArrayList<Position>();
        visited.add(from);

        var paths = new HashMap<Position, List<Direction>>();

        var expandQueue = new LinkedList<Map.Entry<Position, ArrayList<Direction>>>();
        BiConsumer<Position, ArrayList<Direction>> expand = (f, previousMoves) -> {
            visited.add(f);

            for (var direction :
                    Direction.values()) {
                var r = tryMove(f, direction);
                var path = (ArrayList<Direction>) previousMoves.clone();
                path.add(direction);

                if (r instanceof MoveResult.Valid.Alive aliveR) {
                    for (var gem :
                            aliveR.collectedGems) {
                        if (!paths.containsKey(gem)) {
                            paths.put(gem, path);
                        }
                    }

                    if (!visited.contains(aliveR.newPosition)) {
                        expandQueue.add(new AbstractMap.SimpleEntry<>(aliveR.newPosition, path));
                    }
                }
            }
        };

        var moves = new ArrayList<Direction>();
        expand.accept(from, (ArrayList<Direction>) moves.clone());
        while (!expandQueue.isEmpty() && paths.size() < gameState.getGameBoard().getNumGems() && paths.size() == 0) {
            var nextP = expandQueue.poll();
            expand.accept(nextP.getKey(), nextP.getValue());
        }
        return paths;
    }

    private Integer distance(Position from, Position to) {
        return Math.abs(from.col() - to.col()) + Math.abs(from.row() - to.row());
    }
}
