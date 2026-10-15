package com.linklink.myspatial.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LianliankanGameTest {

    @Test
    fun `adjacent same tiles connect with zero turns`() {
        val board = empty3DBoard(
            Cell(0, 0, 0) to 0,
            Cell(0, 1, 0) to 0,
        )
        val path = LianliankanSolver.findPath(board, Cell(0, 0, 0), Cell(0, 1, 0))

        assertNotNull(path)
        assertEquals(listOf(Cell(0, 0, 0), Cell(0, 1, 0)), path)
        assertEquals(0, LianliankanSolver.countTurns(path!!))
    }

    @Test
    fun `same depth layer connects straight through empty cells`() {
        val board = empty3DBoard(
            Cell(1, 0, 2) to 1,
            Cell(1, 3, 2) to 1,
        )
        val path = LianliankanSolver.findPath(board, Cell(1, 0, 2), Cell(1, 3, 2))

        assertNotNull(path)
        assertTrue(path!!.all { it.r == 1 && it.l == 2 })
        assertEquals(0, LianliankanSolver.countTurns(path))
    }

    @Test
    fun `row and column separated pair connects within two turns`() {
        val board = empty3DBoard(
            Cell(0, 0, 0) to 2,
            Cell(0, 2, 0) to 3,
            Cell(2, 2, 0) to 2,
        )
        val path = LianliankanSolver.findPath(board, Cell(0, 0, 0), Cell(2, 2, 0))

        assertNotNull(path)
        assertTrue(LianliankanSolver.countTurns(path!!) <= 2)
    }

    @Test
    fun `three axis diagonal connects with exactly two turns`() {
        val board = empty3DBoard(
            Cell(0, 0, 0) to 4,
            Cell(3, 3, 3) to 4,
        )
        val path = LianliankanSolver.findPath(board, Cell(0, 0, 0), Cell(3, 3, 3))

        assertNotNull(path)
        assertEquals(2, LianliankanSolver.countTurns(path!!))
    }

    @Test
    fun `blocked row can walk around the three dimensional outer shell`() {
        val board = empty3DBoard(
            Cell(0, 0, 0) to 5,
            Cell(0, 1, 0) to 6,
            Cell(0, 2, 0) to 5,
        )
        val path = LianliankanSolver.findPath(board, Cell(0, 0, 0), Cell(0, 2, 0))

        assertNotNull(path)
        assertTrue(path!!.any { it.r == -1 || it.r == 1 || it.l == -1 || it.l == 1 })
    }

    @Test
    fun `three turn path is accepted by relaxed three dimensional rule but not two turn rule`() {
        val assignments = buildMap {
            listOf(
                Cell(0, 1, 1),
                Cell(1, 0, 1),
                Cell(1, 2, 1),
                Cell(1, 1, 0),
                Cell(1, 1, 2),
                Cell(1, 2, 2),
                Cell(2, 1, 2),
                Cell(2, 3, 2),
                Cell(2, 2, 1),
                Cell(2, 2, 3),
            ).forEach { put(it, 7) }
            put(Cell(1, 1, 1), 8)
            put(Cell(2, 2, 2), 8)
        }
        val board = empty3DBoard(*assignments.toList().toTypedArray())

        assertEquals(3, LianliankanSolver.DEFAULT_MAX_TURNS)
        val relaxedPath = LianliankanSolver.findPath(
            board,
            Cell(1, 1, 1),
            Cell(2, 2, 2),
        )

        assertNotNull(relaxedPath)
        assertTrue(LianliankanSolver.countTurns(relaxedPath!!) <= 3)
        assertNull(
            LianliankanSolver.findPath(
                board,
                Cell(1, 1, 1),
                Cell(2, 2, 2),
                maxTurns = 2,
            ),
        )
        assertEquals(
            3,
            LianliankanSolver.countTurns(
                listOf(
                    Cell(1, 1, 1),
                    Cell(2, 1, 1),
                    Cell(3, 1, 1),
                    Cell(3, 2, 1),
                    Cell(3, 2, 2),
                    Cell(2, 2, 2),
                ),
            ),
        )
    }

    @Test
    fun `different kinds never connect`() {
        val board = empty3DBoard(
            Cell(0, 0, 0) to 0,
            Cell(0, 1, 0) to 1,
        )

        assertNull(LianliankanSolver.findPath(board, Cell(0, 0, 0), Cell(0, 1, 0)))
    }

    @Test
    fun `same cell is rejected`() {
        val board = empty3DBoard(Cell(0, 0, 0) to 0)

        assertNull(LianliankanSolver.findPath(board, Cell(0, 0, 0), Cell(0, 0, 0)))
    }

    @Test
    fun `tap selects then removes a legal pair`() {
        val game = LianliankanGame(seed = 11L)
        val pair = findAnyAvailablePair(game) ?: error("generated board has no pair")

        assertEquals(MoveResult.Selected(pair.first), game.tap(pair.first))
        val result = game.tap(pair.second)

        assertTrue(result is MoveResult.Matched)
        assertEquals(LianliankanGame.EMPTY, game.board[pair.first.r][pair.first.c][pair.first.l])
        assertEquals(
            LianliankanGame.EMPTY,
            game.board[pair.second.r][pair.second.c][pair.second.l],
        )
        assertEquals(31, game.remainingPairs)
        assertFalse(game.isCleared)
    }

    @Test
    fun `generated board has sixty four tiles in thirty two unique pairs`() {
        repeat(10) { seed ->
            val game = LianliankanGame(seed = seed.toLong())
            val tiles = game.board.flatMap { rows ->
                rows.flatMap { row -> row.map { kind -> kind } }
            }

            assertEquals(64, tiles.size)
            assertEquals(32, tiles.filter { it != LianliankanGame.EMPTY }.distinct().size)
            assertTrue(tiles.groupBy { it }.all { (_, values) -> values.size == 2 })
            assertTrue(game.hasAvailableMove())
        }
    }

    @Test
    fun `odd boards reserve one empty cell and generate complete pairs`() {
        val game = LianliankanGame(rows = 3, cols = 3, layers = 3, seed = 31L)
        val tiles = game.board.flatten().flatten()

        assertEquals(27, tiles.size)
        assertEquals(1, tiles.count { it == LianliankanGame.EMPTY })
        assertEquals(LianliankanGame.EMPTY, game.board[1][1][1])
        assertTrue(
            (0 until 3).flatMap { r ->
                (0 until 3).flatMap { c ->
                    (0 until 3).map { l -> Cell(r, c, l) }
                }
            }.all { cell ->
                cell == Cell(1, 1, 1) ||
                    game.board[cell.r][cell.c][cell.l] != LianliankanGame.EMPTY
            },
        )
        assertEquals(13, game.remainingPairs)
        assertEquals(13, tiles.filter { it != LianliankanGame.EMPTY }.distinct().size)
        assertTrue(
            tiles.filter { it != LianliankanGame.EMPTY }
                .groupBy { it }
                .all { (_, values) -> values.size == 2 },
        )
        assertTrue(game.hasAvailableMove())
    }

    @Test
    fun `non cubic custom boards generate the configured pair count`() {
        val game = LianliankanGame(rows = 2, cols = 3, layers = 4, seed = 32L)
        val tiles = game.board.flatten().flatten()

        assertEquals(24, tiles.size)
        assertEquals(0, tiles.count { it == LianliankanGame.EMPTY })
        assertEquals(12, game.remainingPairs)
        assertTrue(game.hasAvailableMove())
    }

    @Test
    fun `six by six by six board exceeds visual identities but remains fully paired`() {
        val game = LianliankanGame(rows = 6, cols = 6, layers = 6, seed = 61L)
        val tiles = game.board.flatten().flatten()

        assertEquals(216, tiles.size)
        assertEquals(108, game.remainingPairs)
        assertEquals(0, tiles.count { it == LianliankanGame.EMPTY })
        assertTrue(tiles.all { it in 0 until LianliankanGame.MAX_CARD_STYLES })
        assertTrue(
            tiles.groupBy { it }.values.all { kinds -> kinds.size % 2 == 0 },
        )
        assertTrue(game.hasAvailableMove())
    }

    @Test
    fun `seven by seven by seven next level keeps its hidden empty cell`() {
        val game = LianliankanGame(rows = 7, cols = 7, layers = 7, seed = 71L)
        val hiddenCell = Cell(3, 3, 3)

        assertEquals(171, game.remainingPairs)
        assertEquals(
            LianliankanGame.EMPTY,
            game.board[hiddenCell.r][hiddenCell.c][hiddenCell.l],
        )
        assertTrue(game.hasAvailableMove())
    }

    @Test
    fun `timed challenge durations scale by pair count and round to half minutes`() {
        assertEquals(5 * 60_000L, LianliankanGame.timedChallengeDurationMillis(13))
        assertEquals(10 * 60_000L, LianliankanGame.timedChallengeDurationMillis(32))
        assertEquals(18 * 60_000L, LianliankanGame.timedChallengeDurationMillis(62))
        assertEquals(30 * 60_000L + 30_000L, LianliankanGame.timedChallengeDurationMillis(108))
        assertEquals(3 * 60_000L, LianliankanGame.timedChallengeDurationMillis(1))
    }

    @Test
    fun `odd board hidden empty cell stays interior after restart and shuffle`() {
        val game = LianliankanGame(rows = 3, cols = 3, layers = 3, seed = 33L)
        val hiddenCell = Cell(1, 1, 1)

        repeat(3) {
            game.restart()
            assertEquals(LianliankanGame.EMPTY, game.board[hiddenCell.r][hiddenCell.c][hiddenCell.l])
            assertEquals(13, game.remainingPairs)
        }

        val pair = game.requestHint() ?: error("odd board should have a hint")
        game.tap(pair.first)
        assertTrue(game.tap(pair.second) is MoveResult.Matched)
        val moves = game.shuffleRemaining()

        assertTrue(moves.isNotEmpty())
        assertEquals(LianliankanGame.EMPTY, game.board[hiddenCell.r][hiddenCell.c][hiddenCell.l])
        assertTrue(game.hasAvailableMove())
    }

    @Test
    fun `restart produces a fresh playable three dimensional board`() {
        val game = LianliankanGame(seed = 12L)
        game.restart()

        assertEquals(32, game.remainingPairs)
        assertTrue(game.hasAvailableMove())
        assertEquals(null, game.selected)
    }

    @Test
    fun `hint returns a legal pair three times and restart restores quota`() {
        val game = LianliankanGame(seed = 21L)

        repeat(3) {
            val pair = game.requestHint() ?: error("hint $it should find a pair")
            assertEquals(2 - it, game.hintsRemaining)
            assertEquals(
                game.board[pair.first.r][pair.first.c][pair.first.l],
                game.board[pair.second.r][pair.second.c][pair.second.l],
            )
            assertNotNull(
                LianliankanSolver.findPath(
                    game.board,
                    pair.first,
                    pair.second,
                ),
            )
        }

        assertNull(game.requestHint())
        game.restart()
        assertEquals(3, game.hintsRemaining)
        assertNotNull(game.requestHint())
    }

    @Test
    fun `shuffle preserves remaining kinds and empties and stays playable`() {
        val game = LianliankanGame(seed = 22L)
        val firstPair = game.requestHint() ?: error("initial board should have a hint")
        game.tap(firstPair.first)
        assertTrue(game.tap(firstPair.second) is MoveResult.Matched)

        val beforeKinds = game.board.flatten().flatten()
            .filter { it != LianliankanGame.EMPTY }
            .groupBy { it }
        val moves = game.shuffleRemaining()

        assertTrue(moves.isNotEmpty())
        assertTrue(moves.all { (source, target) -> source != target })
        assertTrue(game.hasAvailableMove())
        assertEquals(null, game.selected)
        assertEquals(2, game.hintsRemaining)

        val afterKinds = game.board.flatten().flatten()
            .filter { it != LianliankanGame.EMPTY }
            .groupBy { it }
        assertEquals(beforeKinds.keys, afterKinds.keys)
        assertTrue(afterKinds.all { (_, values) -> values.size == 2 })
        assertEquals(31, game.remainingPairs)
    }

    @Test
    fun `explicit match judges two chosen cards without tap selection state`() {
        val game = LianliankanGame(seed = 23L)
        val pair = game.requestHint() ?: error("seeded board should have a pair")

        val result = game.match(pair.first, pair.second)

        assertTrue(result is MoveResult.Matched)
        assertEquals(null, game.selected)
        assertEquals(31, game.remainingPairs)
        assertEquals(
            LianliankanGame.EMPTY,
            game.board[pair.first.r][pair.first.c][pair.first.l],
        )
        assertEquals(
            LianliankanGame.EMPTY,
            game.board[pair.second.r][pair.second.c][pair.second.l],
        )
    }

    @Test
    fun `explicit match rejects same cell without changing board`() {
        val game = LianliankanGame(seed = 24L)
        val cell = Cell(0, 0, 0)
        val before = game.board

        assertTrue(game.match(cell, cell) is MoveResult.NotMatched)
        assertEquals(before, game.board)
        assertEquals(null, game.selected)
    }

    private fun empty3DBoard(vararg assignments: Pair<Cell, Int>): List<List<List<Int>>> {
        val values = assignments.toMap()
        return List(4) { r ->
            List(4) { c ->
                List(4) { l ->
                    values.getOrDefault(Cell(r, c, l), LianliankanGame.EMPTY)
                }
            }
        }
    }

    private fun findAnyAvailablePair(game: LianliankanGame): Pair<Cell, Cell>? {
        val cells = buildList {
            for (r in 0 until 4) {
                for (c in 0 until 4) {
                    for (l in 0 until 4) {
                        if (game.board[r][c][l] != LianliankanGame.EMPTY) {
                            add(Cell(r, c, l))
                        }
                    }
                }
            }
        }

        for (i in cells.indices) {
            for (j in i + 1 until cells.size) {
                val a = cells[i]
                val b = cells[j]
                if (game.board[a.r][a.c][a.l] == game.board[b.r][b.c][b.l] &&
                    LianliankanSolver.findPath(game.board, a, b) != null
                ) {
                    return a to b
                }
            }
        }
        return null
    }
}
