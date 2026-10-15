package com.linklink.myspatial.game

import kotlin.math.absoluteValue
import kotlin.random.Random

/**
 * A three-dimensional board cell.
 *
 * `r` is the vertical row, `c` is the horizontal column, and `l` is the depth
 * layer. A 4x4x4 board contains 64 cells.
 */
data class Cell(val r: Int, val c: Int, val l: Int)

/** Result of tapping a tile. */
sealed interface MoveResult {
    /** The tapped tile became the currently selected tile. */
    data class Selected(val cell: Cell) : MoveResult

    /** The tapped tile was the selected tile and has been deselected. */
    data class Deselected(val cell: Cell) : MoveResult

    /** The two tiles have the same kind and a legal path; they are removed. */
    data class Matched(val a: Cell, val b: Cell, val path: List<Cell>) : MoveResult

    /** Kinds differ or no legal path exists; the selection was cleared. */
    data object NotMatched : MoveResult
}

/**
 * Three-dimensional Lianliankan rules.
 *
 * A pair can be removed when both cells carry the same kind and can be joined
 * by an axis-aligned path with at most three turns. The path may walk through
 * empty cells and through the one-cell outer shell surrounding the 3D board.
 */
class LianliankanGame(
    val rows: Int = DEFAULT_ROWS,
    val cols: Int = DEFAULT_COLS,
    val layers: Int = DEFAULT_LAYERS,
    private val kindCount: Int = rows * cols * layers / 2,
    seed: Long? = null,
) {
    private val random: Random = if (seed != null) Random(seed) else Random.Default

    /** Current board: kind index per cell, or [EMPTY] after a match. */
    var board: List<List<List<Int>>> = newPlayableBoard()
        private set

    var selected: Cell? = null
        private set

    /** Hints remaining for the current game. A restart restores the quota. */
    var hintsRemaining: Int = DEFAULT_HINT_COUNT
        private set

    val remainingPairs: Int
        get() = board.flatten().flatten().count { it != EMPTY } / 2

    val pairCount: Int
        get() = rows * cols * layers / 2

    val isCleared: Boolean
        get() = board.flatten().flatten().all { it == EMPTY }

    fun tap(cell: Cell): MoveResult {
        if (!cell.inBounds(rows, cols, layers)) {
            return MoveResult.NotMatched
        }
        if (board[cell.r][cell.c][cell.l] == EMPTY) {
            return MoveResult.NotMatched
        }

        val first = selected
        return when {
            first == null -> {
                selected = cell
                MoveResult.Selected(cell)
            }

            first == cell -> {
                selected = null
                MoveResult.Deselected(cell)
            }

            else -> {
                val path = LianliankanSolver.findPath(board, first, cell)
                if (path == null) {
                    selected = null
                    MoveResult.NotMatched
                } else {
                    board = board.copyOfBoardWith(first, cell, EMPTY)
                    selected = null
                    MoveResult.Matched(first, cell, path)
                }
            }
        }
    }

    /**
     * Judges two explicitly chosen cells without relying on the single-selection
     * state machine. Feel mode uses this for its left-hand/right-hand ritual.
     */
    fun match(first: Cell, second: Cell): MoveResult {
        val firstInBounds = first.inBounds(rows, cols, layers)
        val secondInBounds = second.inBounds(rows, cols, layers)
        if (!firstInBounds || !secondInBounds || first == second) {
            return MoveResult.NotMatched
        }

        val firstKind = board[first.r][first.c][first.l]
        val secondKind = board[second.r][second.c][second.l]
        if (firstKind == EMPTY || firstKind != secondKind) {
            return MoveResult.NotMatched
        }

        val path = LianliankanSolver.findPath(board, first, second)
            ?: return MoveResult.NotMatched

        board = board.copyOfBoardWith(first, second, EMPTY)
        selected = null
        return MoveResult.Matched(first, second, path)
    }

    /** Starts a fresh, guaranteed-playable board and clears the selection. */
    fun restart() {
        board = newPlayableBoard()
        selected = null
        hintsRemaining = DEFAULT_HINT_COUNT
    }

    /** True when at least one remaining pair can currently be connected. */
    fun hasAvailableMove(): Boolean = findAvailablePair() != null

    /**
     * Consumes one hint and returns a currently connectable pair.
     *
     * The quota is not consumed when the board has no legal move; that case
     * should direct the player to shuffle instead.
     */
    fun requestHint(): Pair<Cell, Cell>? {
        if (hintsRemaining <= 0 || isCleared) return null
        val pair = findAvailablePair() ?: return null
        hintsRemaining--
        return pair
    }

    /** Returns one same-kind pair that can be connected by the current board. */
    fun findAvailablePair(): Pair<Cell, Cell>? {
        val cells = remainingCells()
        for (i in cells.indices) {
            for (j in i + 1 until cells.size) {
                val a = cells[i]
                val b = cells[j]
                if (board[a.r][a.c][a.l] == board[b.r][b.c][b.l] &&
                    LianliankanSolver.findPath(board, a, b) != null
                ) {
                    return a to b
                }
            }
        }
        return null
    }

    /** Returns the current legal path for a connectable pair, for hint visuals. */
    fun pathFor(first: Cell, second: Cell): List<Cell>? =
        LianliankanSolver.findPath(board, first, second)

    /**
     * Shuffles only the remaining tile kinds and returns the visual movement
     * mapping `{ source cell -> destination cell }`.
     *
     * Every active source is mapped to a different active destination, and the
     * resulting board is guaranteed to contain at least one available move.
     * Empty cells and already-matched tiles stay empty.
     */
    fun shuffleRemaining(): Map<Cell, Cell> {
        val cells = remainingCells()
        if (cells.size < 2) return emptyMap()

        val kinds = cells.associateWith { cell -> board[cell.r][cell.c][cell.l] }
        repeat(SHUFFLE_ATTEMPTS) {
            val targets = cells.shuffled(random)
            val hasFixedPoint = cells.indices.any { cells[it] == targets[it] }
            if (hasFixedPoint) return@repeat

            val candidate = board.copyOfBoardWithMoves(cells, targets, kinds)
            if (hasAvailableMoveOn(candidate)) {
                board = candidate
                selected = null
                return cells.indices.associate { index ->
                    cells[index] to targets[index]
                }
            }
        }
        return emptyMap()
    }

    private fun remainingCells(): List<Cell> = buildList {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                for (l in 0 until layers) {
                    if (board[r][c][l] != EMPTY) add(Cell(r, c, l))
                }
            }
        }
    }

    private fun newPlayableBoard(): List<List<List<Int>>> {
        repeat(MAX_SHUFFLE_ATTEMPTS) {
            val candidate = shuffledBoard()
            if (hasAvailableMoveOn(candidate)) return candidate
        }
        return fallbackBoard()
    }

    private fun shuffledBoard(): List<List<List<Int>>> {
        // Boards beyond 5×5×5 need more logical pairs than the 64 available
        // animal/color identities. Reuse identities in complete pairs; this
        // keeps every generated tile matchable while retaining all unique
        // textures for 3×3×3 through 5×5×5.
        val pairedTiles = List(kindCount) { it % MAX_CARD_STYLES }
            .flatMap { listOf(it, it) }
            .shuffled(random)
        val hiddenEmptyCell = hiddenEmptyCell()
        var index = 0
        return List(rows) { r ->
            List(cols) { c ->
                List(layers) { l ->
                    if (Cell(r, c, l) == hiddenEmptyCell) {
                        LianliankanGame.EMPTY
                    } else {
                        pairedTiles[index++]
                    }
                }
            }
        }
    }

    /**
     * Deterministic board with adjacent pairs; always playable. Like the main
     * shuffled board, an odd-grid empty slot is kept at the innermost cell.
     */
    private fun fallbackBoard(): List<List<List<Int>>> {
        val hiddenEmptyCell = hiddenEmptyCell()
        val pairedTiles = List(kindCount) { it % MAX_CARD_STYLES }
            .flatMap { listOf(it, it) }
        var index = 0
        return List(rows) { r ->
            List(cols) { c ->
                List(layers) { l ->
                    val cell = Cell(r, c, l)
                    if (cell == hiddenEmptyCell) {
                        LianliankanGame.EMPTY
                    } else {
                        pairedTiles[index++]
                    }
                }
            }
        }
    }

    /**
     * Odd grids reserve their innermost cell for the unavoidable empty slot.
     * Keeping it at the median on every axis means the exterior remains full
     * and the board still looks complete from every side.
     */
    private fun hiddenEmptyCell(): Cell? {
        if (rows * cols * layers == kindCount * 2) return null
        return Cell(
            r = rows / 2,
            c = cols / 2,
            l = layers / 2,
        )
    }

    private fun hasAvailableMoveOn(candidate: List<List<List<Int>>>): Boolean {
        val saved = board
        board = candidate
        val result = hasAvailableMove()
        board = saved
        return result
    }

    companion object {
        const val DEFAULT_ROWS = 4
        const val DEFAULT_COLS = 4
        const val DEFAULT_LAYERS = 4
        const val EMPTY = -1
        const val DEFAULT_HINT_COUNT = 3
        const val MAX_CARD_STYLES = 64

        /**
         * Timed Challenge duration: a 90-second base plus 16 seconds per pair,
         * rounded to a player-friendly 30-second boundary. Small custom boards
         * receive a three-minute floor.
         */
        fun timedChallengeDurationMillis(pairCount: Int): Long {
            val rawSeconds = 90 + 16 * pairCount
            val roundedSeconds = ((rawSeconds + 15) / 30) * 30
            val seconds = maxOf(roundedSeconds, 180)
            return seconds * 1_000L
        }
        private const val MAX_SHUFFLE_ATTEMPTS = 100
        private const val SHUFFLE_ATTEMPTS = 100
    }
}

private fun List<List<List<Int>>>.copyOfBoardWithMoves(
    sources: List<Cell>,
    targets: List<Cell>,
    kinds: Map<Cell, Int>,
): List<List<List<Int>>> {
    val destinationKinds = targets.indices.associate { index ->
        targets[index] to kinds.getValue(sources[index])
    }
    return mapIndexed { r, rows ->
        rows.mapIndexed { c, row ->
            row.mapIndexed { l, _ ->
                destinationKinds[Cell(r, c, l)] ?: LianliankanGame.EMPTY
            }
        }
    }
}

/** Path search for the 3D, at-most-three-turn Lianliankan rule. */
object LianliankanSolver {
    const val DEFAULT_MAX_TURNS = 3

    private val DR = intArrayOf(-1, 0, 1, 0, 0, 0)
    private val DC = intArrayOf(0, 1, 0, -1, 0, 0)
    private val DL = intArrayOf(0, 0, 0, 0, -1, 1)

    fun findPath(
        board: List<List<List<Int>>>,
        from: Cell,
        to: Cell,
        maxTurns: Int = DEFAULT_MAX_TURNS,
    ): List<Cell>? {
        val rows = board.size
        val cols = board[0].size
        val layers = board[0][0].size
        if (!from.inBounds(rows, cols, layers) || !to.inBounds(rows, cols, layers)) {
            return null
        }
        if (from == to) {
            return null
        }

        val kind = board[from.r][from.c][from.l]
        val toKind = board[to.r][to.c][to.l]
        if (kind == LianliankanGame.EMPTY) {
            return null
        }
        if (toKind != kind) {
            return null
        }

        fun passable(r: Int, c: Int, l: Int): Boolean {
            if (r == to.r && c == to.c && l == to.l) return true
            val outside = r !in 0 until rows ||
                c !in 0 until cols ||
                l !in 0 until layers
            if (outside) {
                return r in -1..rows && c in -1..cols && l in -1..layers
            }
            return board[r][c][l] == LianliankanGame.EMPTY
        }

        data class State(val r: Int, val c: Int, val l: Int, val dir: Int)

        val start = State(from.r, from.c, from.l, -1)
        val minTurns = Array(rows + 2) {
            Array(cols + 2) {
                Array(layers + 2) { IntArray(6) { Int.MAX_VALUE } }
            }
        }
        val parent = HashMap<State, State>()
        val queue = ArrayDeque<State>()

        fun offer(next: State, turns: Int, cameFrom: State) {
            val best = minTurns[next.r + 1][next.c + 1][next.l + 1][next.dir]
            if (turns < best) {
                minTurns[next.r + 1][next.c + 1][next.l + 1][next.dir] = turns
                parent[next] = cameFrom
                queue.add(next)
            }
        }

        for (dir in DR.indices) {
            val nr = from.r + DR[dir]
            val nc = from.c + DC[dir]
            val nl = from.l + DL[dir]
            val nextPassable = passable(nr, nc, nl)
            if (nextPassable) {
                offer(State(nr, nc, nl, dir), 0, start)
            }
        }

        while (queue.isNotEmpty()) {
            val state = queue.removeFirst()
            if (state.r == to.r && state.c == to.c && state.l == to.l) {
                val path = buildList {
                    var node: State? = state
                    while (node != null) {
                        add(Cell(node.r, node.c, node.l))
                        node = parent[node]
                    }
                }.asReversed()
                return path
            }

            val turns = minTurns[state.r + 1][state.c + 1][state.l + 1][state.dir]
            for (dir in DR.indices) {
                val nr = state.r + DR[dir]
                val nc = state.c + DC[dir]
                val nl = state.l + DL[dir]
                val nextTurns = if (dir == state.dir) turns else turns + 1
                val nextPassable = passable(nr, nc, nl)
                if (!nextPassable) continue
                if (nextTurns > maxTurns) {
                    continue
                }
                offer(State(nr, nc, nl, dir), nextTurns, state)
            }
        }
        return null
    }
    /** Number of direction changes along a returned path. */
    fun countTurns(path: List<Cell>): Int {
        if (path.size < 3) return 0

        fun direction(a: Cell, b: Cell): Triple<Int, Int, Int> =
            Triple(
                (b.r - a.r).absoluteValue,
                (b.c - a.c).absoluteValue,
                (b.l - a.l).absoluteValue,
            )

        var turns = 0
        for (i in 2 until path.size) {
            if (direction(path[i - 2], path[i - 1]) !=
                direction(path[i - 1], path[i])
            ) {
                turns++
            }
        }
        return turns
    }
}

private fun Cell.inBounds(rows: Int, cols: Int, layers: Int): Boolean =
    r in 0 until rows && c in 0 until cols && l in 0 until layers

private fun List<List<List<Int>>>.copyOfBoardWith(
    a: Cell,
    b: Cell,
    value: Int,
): List<List<List<Int>>> = mapIndexed { r, layerRows ->
    layerRows.mapIndexed { c, row ->
        row.mapIndexed { l, cell ->
            val isA = r == a.r && c == a.c && l == a.l
            val isB = r == b.r && c == b.c && l == b.l
            if (isA || isB) value else cell
        }
    }
}
