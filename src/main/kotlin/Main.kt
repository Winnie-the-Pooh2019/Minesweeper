package minesweeper

import kotlin.random.Random

object State {
    enum class Persistence {
        IDLE,
        PLAY,
        OVER,
        WON
    }

    var current = Persistence.IDLE
}

class Field(val width: Int, val height: Int) {
    enum class Character(val character: Char) {
        UNKNOWN('.'),
        FREE('/'),
        MINE('X'),
        MARKED_MINE('*')
    }

    val pseudoField: ArrayList<ArrayList<Char>> = ArrayList()
    val actualField: ArrayList<ArrayList<Char>> = ArrayList()
    val minesCoords: ArrayList<Pair<Int, Int>> = ArrayList()
    val minesDisability: HashMap<Pair<Int, Int>, Boolean> = HashMap()

    init {
        for (i in 0 until (width)) {
            pseudoField.add(ArrayList())
            actualField.add(ArrayList())

            for (j in 0 until height) {
                pseudoField[i].add(Character.UNKNOWN.character)
                actualField[i].add(Character.FREE.character)
            }
        }
    }

    fun show(): Unit {
        println(" |${(1..width).joinToString("")}|")
        println("-|---------|")
        for (i in 0 until height) {
            val list = ArrayList<Char>()
            for (j in 0 until width) {
                list.add(pseudoField[i][j])
            }

            println("${i + 1}|${list.joinToString("")}|")
        }
        println("-|---------|")
    }

    fun minesGen(minesAmount: Int, tapped: Pair<Int, Int>): Unit {
        var counter: Int = 0

        while (counter < minesAmount) {
            val randomCoords = Pair(Random.nextInt(0, width), Random.nextInt(0, height))

            if (randomCoords in minesCoords || randomCoords == tapped)
                continue

            minesCoords.add(randomCoords)
            actualField[randomCoords.first][randomCoords.second] = 'X'
            minesDisability[randomCoords] = false

            counter++
        }

        for (i in 0 until actualField.size) {
            for (j in 0 until actualField[i].size) {
                if (actualField[i][j] == 'X')
                    continue

                val neighbors = actualField.findNeighbors(Pair(i, j))

                val count = neighbors.count { it in minesCoords }
                if (count > 0) actualField[i][j] = count.toString()[0]
            }
        }

        State.current = State.Persistence.PLAY
    }

    private fun displayMines(): Unit {
        for (mine in minesCoords) {
            pseudoField[mine.first][mine.second] = 'X'
        }
    }

    private fun ArrayList<ArrayList<Char>>.findNeighbors(point: Pair<Int, Int>): ArrayList<Pair<Int, Int>> {
        val result = ArrayList<Pair<Int, Int>>()

        val columnLeftLimit = if (point.first != 0) point.first - 1 else point.first
        val columnRightLimit = if (point.first != this.size - 1) point.first + 1 else point.first

        for (i in (columnLeftLimit)..(columnRightLimit)) {
            val rowLeftLimit = if (point.second != 0) point.second - 1 else point.second
            val rowRightLimit = if (point.second != this[i].size - 1) point.second + 1 else point.second

            for (j in (rowLeftLimit)..(rowRightLimit)) {
                if (Pair(i, j) == point)
                    continue

                result.add(Pair(i, j))
            }
        }

        return result
    }

    // clears ONLY UNKNOWN CELLS
    private fun clear(point: Pair<Int, Int>): Unit {
        val code = actualField[point.first][point.second].toString().toIntOrNull()
        if (code != null) {
            pseudoField[point.first][point.second] = actualField[point.first][point.second]

            return
        }

        recClear(point)

        for (i in 0 until pseudoField.size) {
            for (j in 0 until pseudoField[i].size) {
                if (actualField[i][j].toString().toIntOrNull() != null) {
                    val neighbors = actualField.findNeighbors(Pair(i, j))

                    val count = neighbors.count { pseudoField[it.first][it.second] == '/' }

                    if (count > 0) {
                        pseudoField[i][j] = actualField[i][j]
                    }
                }
            }
        }
    }

    private fun recClear(point: Pair<Int, Int>): Unit {
        if ((pseudoField[point.first][point.second] == '.' || pseudoField[point.first][point.second] == '*') && actualField[point.first][point.second] == '/') {
            pseudoField[point.first][point.second] = '/'

            val rightNeighbors = actualField.findNeighbors(point).filter { it != point && actualField[it.first][it.second] == '/' }

            for (rn in rightNeighbors) {
                recClear(rn)
            }
        }
    }

    private fun mark(point: Pair<Int, Int>): Unit {
        if (pseudoField[point.first][point.second].toString()
                .toIntOrNull() == null && pseudoField[point.first][point.second] != '/'
        ) {
            pseudoField[point.first][point.second] = if (pseudoField[point.first][point.second] == '.') '*' else '.'

            if (point in minesCoords)
                minesDisability[point] = !minesDisability[point]!!
        }
    }

    private fun check(point: Pair<Int, Int>): Boolean {
        return point !in minesCoords
    }

    fun onAction(point: Pair<Int, Int>, action: Game.Action) {
        when (action) {
            Game.Action.FREE -> {
                val checking = check(point)

                if (!checking) {
                    displayMines()
                    State.current = State.Persistence.OVER
                    return
                }
                clear(point)
            }

            Game.Action.MARK -> {
                mark(point)
            }
        }

        var freed = 0
        var free = 0
        for (i in 0 until actualField.size) {
            for (j in 0 until actualField[i].size) {
                if (actualField[i][j] == '/') free++
                if (pseudoField[i][j] == '/') freed++
            }
        }

        var nummed = 0
        var num = 0
        for (i in 0 until actualField.size) {
            for (j in 0 until actualField[i].size) {
                if (actualField[i][j].toString().toIntOrNull() != null) num++
                if (pseudoField[i][j].toString().toIntOrNull() != null) nummed++
            }
        }

        val enable = minesDisability.values.count { !it }

        if (enable == 0 || free == freed && nummed == num)
            State.current = State.Persistence.WON
    }
}

class Game(private val minesAmount: Int) {
    enum class Action {
        FREE,
        MARK
    }

    private val field = Field(9, 9)

    fun run(): Unit {
        field.show()
        while (true) {

            when (State.current) {
                State.Persistence.IDLE -> {
                    val answer = askForInput()
                    field.minesGen(minesAmount, answer.first)
                    field.onAction(answer.first, answer.second)
                }

                State.Persistence.PLAY -> {
                    val answer = askForInput()

                    field.onAction(answer.first, answer.second)
                }

                State.Persistence.OVER -> {
                    println("You stepped on a mine and failed!")

                    break
                }

                State.Persistence.WON -> {
                    println("Congratulations! You found all the mines!")

                    break
                }
            }

            field.show()
        }
    }

    private fun askForInput(): Pair<Pair<Int, Int>, Action> {
        print("Set/unset mines marks or claim a cell as free: > ")

        val inp = readLine()!!.split(" ")

        val act = if (inp.size > 2) if (inp[2] == Action.FREE.name.lowercase()) Action.FREE else Action.MARK else Action.FREE

        return Pair(Pair(inp[1].toInt() - 1, inp[0].toInt() - 1), act)
    }
}

fun main() {
    print("How many mines do you want on the field? > ")

    val mines = readLine()!!.toInt()

    val game = Game(mines)

    game.run()
}