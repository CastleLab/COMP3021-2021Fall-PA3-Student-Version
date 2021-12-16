package hk.ust.cse.comp3021.pa3.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The main game board of the game.
 *
 * <p>
 * The top-left hand corner of the game board is the "origin" of the board (0, 0).
 * </p>
 */
public final class GameBoard {

    /**
     * Number of rows in the game board.
     */
    private final int numRows;
    /**
     * Number of columns in the game board.
     */
    private final int numCols;

    /**
     * 2D array representing each cell in the game board.
     */
    @NotNull
    private final Cell[][] board;

    /**
     * The instance of {@link Player} on this game board.
     */
    @NotNull
    private final List<Player> players;

    private final Lock lock = new ReentrantLock();

    /**
     * Creates an instance using the provided creation parameters.
     *
     * @param numRows The number of rows in the game board.
     * @param numCols The number of columns in the game board.
     * @param cells   The initial values of cells.
     * @throws IllegalArgumentException if any of the following are true:
     *                                  <ul>
     *                                      <li>{@code numRows} is not equal to {@code cells.length}</li>
     *                                      <li>{@code numCols} is not equal to {@code cells[0].length}</li>
     *                                      <li>There is no player or more than one player in {@code cells}</li>
     *                                      <li>There are no gems in {@code cells}</li>
     *                                      <li>There are some gems which cannot be reached by the player</li>
     *                                  </ul>
     */
    public GameBoard(final int numRows, final int numCols, @NotNull final Cell[][] cells) {
        this.numRows = numRows;
        this.numCols = numCols;
        this.board = Objects.requireNonNull(cells);

        if (cells.length != numRows) {
            throw new IllegalArgumentException();
        }
        if (cells[0].length != numCols) {
            throw new IllegalArgumentException();
        }

        this.players = getPlayers();

        if (getNumGems() == 0) {
            throw new IllegalArgumentException();
        }
        if (!isAllGemsReachable()) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Checks that a single player exists on the game board, and returns the instance of the player.
     *
     * @return The single instance of player on the game board.
     * @throws IllegalArgumentException if the game board has zero, or more than one player entities.
     */
    @NotNull
    public List<Player> getPlayers() {
        var players = new ArrayList<Player>();
        for (final var row : board) {
            for (final var cell : row) {
                if (cell instanceof EntityCell ec && ec.getEntity() instanceof Player p) {
                    players.add(p);
                }
            }
        }

        if (players.size() == 0) {
            throw new IllegalArgumentException();
        }

        return players;
    }

    /**
     * Retrieves the instance of {@link EntityCell} by a position with an offset.
     *
     * @param pos    Position.
     * @param offset Positional offset to apply.
     * @return An instance of {@link EntityCell}, or {@code null} if the resulting position does not contain an entity
     * cell.
     */
    @Nullable
    private Position getEntityCellByOffset(@NotNull final Position pos, @NotNull final PositionOffset offset) {
        final var newPos = pos.offsetByOrNull(offset, getNumRows(), getNumCols());
        if (newPos == null) {
            return null;
        }
        if (getCell(newPos) instanceof Wall) {
            return null;
        }

        return newPos;
    }

    /**
     * Gets all {@link Position} of cells which the player can stop on from the cell of the {@code initialPosition}.
     *
     * <p>
     * A cell is "stoppable" iff the player can stop on the cell with any combination of valid moves.
     * </p>
     *
     * @param initialPosition The starting position to get all stoppable cells.
     * @return {@link List} of all positions stoppable from {@code initialPosition}.
     */
    @NotNull
    private List<Position> getAllStoppablePositions(@NotNull final Position initialPosition) {
        Objects.requireNonNull(initialPosition);

        final List<Position> allStoppablePos = new ArrayList<>();
        final List<Position> posToTraverse = new ArrayList<>();

        posToTraverse.add(initialPosition);

        while (!posToTraverse.isEmpty()) {
            final var nextPos = posToTraverse.remove(posToTraverse.size() - 1);

            if (!(getCell(nextPos) instanceof EntityCell)) {
                continue;
            }
            if (allStoppablePos.contains(nextPos)) {
                continue;
            }
            allStoppablePos.add(nextPos);

            for (@NotNull final var dir : Direction.values()) {
                for (int i = 0; i < Math.max(getNumRows(), getNumCols()); ++i) {
                    final var posOffset = new PositionOffset(dir.getRowOffset() * i, dir.getColOffset() * i);
                    final var posToAdd = getEntityCellByOffset(nextPos, posOffset);

                    if (posToAdd == null) {
                        final var maxDist = i - 1;
                        if (maxDist > 0) {
                            final var posOffsetBeforeThis = new PositionOffset(
                                    dir.getRowOffset() * maxDist,
                                    dir.getColOffset() * maxDist
                            );
                            final var posBeforeThis = getEntityCellByOffset(nextPos, posOffsetBeforeThis);

                            if (posBeforeThis != null && !posToTraverse.contains(posBeforeThis)) {
                                posToTraverse.add(posBeforeThis);
                            }
                        }

                        break;
                    }

                    if (getCell(posToAdd) instanceof StopCell || isBorderCell(posToAdd, dir)) {
                        posToTraverse.add(posToAdd);
                    }
                }
            }
        }

        return Collections.unmodifiableList(allStoppablePos);
    }

    /**
     * Gets all {@link Position} of cells which are reachable from the cell of the {@code initialPosition}.
     *
     * <p>
     * A cell is reachable iff the player can move over the cell with any combination of valid moves.
     * </p>
     *
     * @param initialPosition The starting position to get all reachable cells.
     * @return {@link List} of all positions reachable from {@code initialPosition}.
     */
    @NotNull
    private List<Position> getAllReachablePositions(@NotNull final Position initialPosition) {
        Objects.requireNonNull(initialPosition);

        final List<Position> allReachablePos = new ArrayList<>();
        final List<Position> allStoppablePos = getAllStoppablePositions(initialPosition);

        for (@NotNull final var reachablePos : allStoppablePos) {
            for (@NotNull final var dir : Direction.values()) {
                for (int i = 0; i < Math.max(getNumRows(), getNumCols()); ++i) {
                    final var posOffset = new PositionOffset(dir.getRowOffset() * i, dir.getColOffset() * i);
                    final var posToAdd = getEntityCellByOffset(reachablePos, posOffset);
                    if (posToAdd == null) {
                        break;
                    }

                    if (!allReachablePos.contains(posToAdd)) {
                        allReachablePos.add(posToAdd);
                    }
                }
            }
        }

        return Collections.unmodifiableList(allReachablePos);
    }

    /**
     * Checks whether the given cell is a border cell with reference to the direction.
     *
     * @param cellPos The position of the cell.
     * @param dir     The direction is moving at.
     * @return {@code true} if the cell at the specified position is a border cell, i.e. the last cell in the game board
     * before going out of bounds in the given direction.
     */
    private boolean isBorderCell(@NotNull final Position cellPos, @NotNull final Direction dir) {
        return switch (dir) {
            case UP -> cellPos.row() == 0;
            case DOWN -> cellPos.row() == getNumRows() - 1;
            case LEFT -> cellPos.col() == 0;
            case RIGHT -> cellPos.col() == getNumCols() - 1;
        };
    }

    /**
     * Checks whether all gems are reachable from the player's initial position.
     *
     * @return {@code true} if all gems are reachable.
     */
    private boolean isAllGemsReachable() {
        for (var player : players) {
            if (!isAllGemsReachableFor(player)) {
                return false;
            }
        }
        return true;
    }

    private boolean isAllGemsReachableFor(Player player) {
        final var expectedNumOfGems = getNumGems();
        final var initialPosition = Objects.requireNonNull(player.getOwner()).getPosition();
        final var playerReachableCells = getAllReachablePositions(initialPosition);

        int actualNumOfGems = 0;
        for (final var pos : playerReachableCells) {
            final var cell = getCell(pos);
            if (cell instanceof EntityCell ec && ec.getEntity() instanceof Gem) {
                ++actualNumOfGems;
            }
        }

        return expectedNumOfGems == actualNumOfGems;
    }

    /**
     * Returns the {@link Cell}s of a single row of the game board.
     *
     * @param r Row index.
     * @return 1D array representing the row. The first element in the array corresponds to the leftmost element of the
     * row.
     */
    @NotNull
    public Cell[] getRow(final int r) {
        return board[r];
    }

    /**
     * Returns the {@link Cell}s of a single column of the game board.
     *
     * @param c Column index.
     * @return 1D array representing the column. The first element in the array corresponds to the topmost element of
     * the row.
     */
    @NotNull
    public Cell[] getCol(final int c) {
        final var col = new Cell[numRows];

        for (int r = 0; r < getNumRows(); ++r) {
            col[r] = getCell(r, c);
        }

        return col;
    }

    /**
     * Returns a single cell of the game board.
     *
     * @param r Row index.
     * @param c Column index.
     * @return The {@link Cell} instance at the specified location.
     */
    @NotNull
    public Cell getCell(final int r, final int c) {
        return board[r][c];
    }

    /**
     * Returns a single cell of the game board.
     *
     * @param position The position object representing the location of the cell.
     * @return The {@link Cell} instance at the specified location.
     */
    @NotNull
    public Cell getCell(@NotNull final Position position) {
        Objects.requireNonNull(position);

        return getCell(position.row(), position.col());
    }

    /**
     * Returns an {@link EntityCell} on the game board.
     *
     * <p>
     * This method is a convenience method for getting a cell which is unconditionally known to be an entity cell.
     * </p>
     *
     * @param r Row index.
     * @param c Column index.
     * @return The {@link EntityCell} instance at the specified location.
     * @throws IllegalArgumentException if the cell at the specified position is not an instance of {@link EntityCell}.
     */
    @NotNull
    public EntityCell getEntityCell(final int r, final int c) {
        final var cell = getCell(r, c);
        if (!(cell instanceof final EntityCell entityCell)) {
            throw new IllegalArgumentException();
        }

        return entityCell;
    }

    /**
     * Returns an {@link EntityCell} on the game board.
     *
     * <p>
     * This method is a convenience method for getting a cell which is unconditionally known to be an entity cell.
     * </p>
     *
     * @param position The position object representing the location of the cell.
     * @return The {@link EntityCell} instance at the specified location.
     * @throws IllegalArgumentException if the cell at the specified position is not an instance of {@link EntityCell}.
     */
    @NotNull
    public EntityCell getEntityCell(@NotNull final Position position) {
        return getEntityCell(position.row(), position.col());
    }

    /**
     * @return The number of rows of this game board.
     */
    public int getNumRows() {
        return numRows;
    }

    /**
     * @return The number of columns of this game board.
     */
    public int getNumCols() {
        return numCols;
    }

    /**
     * @return The player instance when the game is in single player mode..
     * @throws IllegalCallerException if the game board is not in the single player mode.
     */
    @NotNull
    public Player getPlayer() {
        if (isMultiplayer()) {
            throw new IllegalCallerException();
        }

        var iterator = players.iterator();
        return iterator.next();
    }

    /**
     * @param id The id of the player.
     * @return The player instance with the given id.
     * @throws IllegalArgumentException if the player with the id does not exist.
     */
    @NotNull
    public Player getPlayer(int id) {
        Player player = null;
        for (var p :
                players) {
            if (p.getId() == id) {
                player = p;
            }
        }
        if (player == null) {
            throw new IllegalArgumentException();
        }

        return player;
    }

    /**
     * Whether a player exists in the game.
     *
     * @param player The player to check existence.
     * @return true if exists.
     */
    public boolean hasPlayer(Player player) {
        return this.players.contains(player);
    }


    public boolean isMultiplayer() {
        return players.size() != 1;
    }

    /**
     * @return The number of gems still present in the game board.
     */
    public int getNumGems() {
        var count = 0;

        for (final var row : board) {
            for (final var cell : row) {
                if (cell instanceof EntityCell ec && ec.getEntity() instanceof Gem) {
                    count++;
                }
            }
        }

        return count;
    }

    public Lock getLock() {
        return lock;
    }
}
