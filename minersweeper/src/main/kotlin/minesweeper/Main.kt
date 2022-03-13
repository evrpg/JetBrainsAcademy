package minesweeper
import java.util.Stack
import kotlin.random.Random

class Cell(var bomb: Boolean = false,
           var sacredCell: Boolean = false,
           var checked: Boolean = false,
           var explored: Boolean = false,
           var aroundBombs: Int = 0) {

    fun print(printBomp: Boolean) = when {
        printBomp && bomb -> CellSymbol.BOMB.symbol
        checked && !explored -> CellSymbol.CHECKED.symbol
        !explored -> CellSymbol.UNEXPLORED.symbol
        isFree() && explored -> CellSymbol.FREE.symbol
        explored && isNumber() -> aroundBombs.toString()
        else -> CellSymbol.ERROR.symbol
    }

    fun isNumber(): Boolean = aroundBombs > 0

    fun isFree(): Boolean = !bomb && !isNumber()

    fun toggleCheck() {
        checked = !checked
    }

    companion object {
        enum class CellSymbol(val symbol: String) {
            BOMB("x"),
            CHECKED("*"),
            UNEXPLORED("."),
            FREE("/"),
            ERROR("e")
        }
    }
}

class MinerSweeperGame (_x: Int = 9, _y: Int = 9) {
    var gameStatus = GameStatus.NO_STARTED
    var gameEndReason = GameEndReason.NONE
    var printField = true
    private val mineField: MinerSweeperField

    init {
        print("How many mines do you want on the field? ")
        val minesQty = readLine()!!.toInt()

        mineField = MinerSweeperField(minesQty, _x, _y)

    }

    fun printField(printBomp: Boolean) {
        if (printField) {
            mineField.printField(printBomp)
        }
    }

    fun receiveCommand() {
        print("Set/unset mine marks or claim a cell as free: ")
        val (x, y, command) = readLine()!!.split(" ")
        val cell = mineField.getCell(y.toInt() - 1 , x.toInt() - 1)

        when(command) {
            "mine" -> {
                //todo: if is number there is a number and is explored
                printField = if (cell.isNumber() and cell.explored) {
                    println("There is a number here!")
                    false
                } else {
                    cell.toggleCheck()
                    true
                }
            }
            "free" -> {
                if (gameStatus == GameStatus.NO_STARTED) {
                    mineField.initField(x.toInt() -1 , y.toInt() - 1)
                    mineField.autoExplore(y.toInt(), x.toInt())
                    gameStatus = GameStatus.STARTED
                } else {
                    if (cell.bomb) { // todo: if a bomb the game end, reason bomb
                        gameStatus = GameStatus.FINISHED
                        gameEndReason = GameEndReason.STEPPED_A_BOMB
                    } else {
                        mineField.autoExplore(y.toInt(), x.toInt())
                        printField = true
                    }
                }
            }
        }
    }

    fun evaluateGame() {
        if (mineField.checkGame()) {
            gameStatus = GameStatus.FINISHED
            gameEndReason = GameEndReason.WON
        }

        if (gameStatus == GameStatus.FINISHED) {
            when (gameEndReason) {
                GameEndReason.STEPPED_A_BOMB -> {
                    printField(true)
                    println(GameEndReason.STEPPED_A_BOMB.message)
                }

                GameEndReason.WON -> {
                    printField(true)
                    println(GameEndReason.WON.message)
                }

                GameEndReason.NONE -> println("The game enter in a error: finishing without reason")
            }
        }
    }

    companion object {
        enum class GameStatus {
            NO_STARTED,
            STARTED,
            FINISHED
        }

        enum class GameEndReason(val message: String) {
            STEPPED_A_BOMB("You stepped on a mine and failed!"),
            WON("Congratulations! You found all the mines!"),
            NONE("")
        }
    }
}

class  MinerSweeperField (private val minesQty: Int, private val x: Int, private val y: Int) {

    private val mineField: MutableList<MutableList<Cell>> = MutableList(y) { MutableList(x) { Cell() } }

    fun initField(sacredFieldX: Int, sacredFieldY: Int) {
        // Randomly insert mines
        var assignedMines = minesQty
        getCell(sacredFieldY , sacredFieldX ).sacredCell = true
        while(assignedMines > 0) {
            val rX = Random.nextInt(x)
            val rY = Random.nextInt(y)

            // avoid adding bombs the first time the user enters a free command
            if( !getCell(rY, rX).bomb  && !getCell(rY, rX).sacredCell) {
                getCell(rY, rX).bomb = true
                assignedMines--
            }
        }

        // Look around
        mineField.forEachIndexed { iY, row -> row.forEachIndexed { iX, _ ->

            getCell(iX, iY).aroundBombs = lookAround(mineField, iX, iY)

        }}
    }

    fun printField(printBomp: Boolean = false) {
        println()
        print(" │")
        for ( i in 1..y) {
            print(i)
        }
        println("│")

        print("—│")
        for ( i in 1..y) {
            print("—")
        }
        println("│")

        mineField.forEachIndexed { index, it ->

            print("${index + 1}|")
            it.forEach { cell -> print(cell.print(printBomp)) }

            println("|")
        }

        print("—│")
        for ( i in 1..y) {
            print("—")
        }
        println("│")
    }

    fun autoExplore(x: Int, y: Int) {

        val pairXY = Pair(x -1 , y - 1)
        val cachedStack: Stack<Pair<Int,Int>> = Stack<Pair<Int,Int>>()
        cachedStack.push(pairXY)

        while (!cachedStack.isEmpty()) {
            val p = cachedStack.pop()
            mineField[p.first][p.second].explored = true
            val around = getAround(p)

            if (around.stream().allMatch { !mineField[it.first][it.second].bomb }){
                cachedStack.addAll(around)
            }
        }
    }

    private fun getAround(p: Pair<Int, Int>): Stack<Pair<Int, Int>> {

        val x = p.first
        val y = p.second

        val around: Stack<Pair<Int, Int>> = Stack()

        // check back
        val back = (y - 1 >= 0)

        // check down
        val down = (x + 1 <= mineField.lastIndex)

        // check front
        val front = (y + 1 <= mineField[x].lastIndex)

        // check up
        val up = (x - 1 >= 0)

        if(back) {
            if ( !getCell(x, y - 1).explored) around.push(Pair(x, y - 1))
        }

        if(back && down){
            if ( !getCell(x + 1, y - 1).explored) around.push(Pair(x + 1, y - 1))
        }

        if(down){
            if ( !getCell(x + 1, y).explored ) around.push(Pair(x + 1, y))
        }

        if(down && front) {
            if ( !getCell(x + 1, y + 1).explored ) around.push(Pair(x + 1, y + 1))
        }

        if( front ){
            if ( !getCell(x, y + 1).explored) around.push(Pair(x, y + 1))
        }

        if( front && up ) {
            if ( !getCell(x - 1, y + 1).explored ) around.push(Pair(x - 1, y + 1))
        }

        if( up ) {
            if( !getCell(x - 1, y).explored  ) around.push(Pair(x - 1, y))
        }

        if( up && back ) {
            if( !getCell(x - 1, y - 1).explored ) around.push(Pair(x - 1, y - 1))
        }

        return around
    }


    fun getCell(x: Int, y: Int): Cell =
        mineField[x][y]

    private fun lookAround(mineField: MutableList<MutableList<Cell>>, x: Int, y: Int): Int {
        var mines = 0

        // check back
        val back = (y - 1 >= 0)

        // check down
        val down = (x + 1 <= mineField.lastIndex)

        // check front
        val front = (y + 1 <= mineField[x].lastIndex)

        // check up
        val up = (x - 1 >= 0)

        if(back) {
            if ( mineField[x][y - 1].bomb ) mines++
        }

        if(back && down){
            if ( mineField[x + 1][y - 1].bomb ) mines++
        }

        if(down){
            if ( mineField[x + 1][y].bomb ) mines++
        }

        if(down && front){
            if ( mineField[x + 1][y + 1].bomb ) mines++
        }

        if( front ){
            if ( mineField[x][y + 1].bomb ) mines++
        }

        if( front && up ){
            if ( mineField[x - 1][y + 1].bomb ) mines++
        }

        if( up ){
            if( mineField[x - 1][y].bomb ) mines++
        }

        if( up && back ){
            if( mineField[x - 1][y - 1].bomb ) mines++
        }

        return mines
    }

    fun checkGame(): Boolean =
        mineField.all { it.filter { cell ->  cell.bomb }.all { cell -> cell.checked }} &&   // all bombs are market
                mineField.all { it.filter { cell ->  cell.checked }.all { cell -> cell.bomb }} // all market are bombs
                ||
                mineField.all { it.filter { cell ->  cell.bomb }.all { cell -> !cell.explored }} &&   // all bombs are unexplored
                mineField.all { it.filter { cell ->  !cell.explored }.all { cell -> cell.bomb }} // all unexplored are bombs

}


fun main() {

    val game = MinerSweeperGame()

    while (game.gameStatus != MinerSweeperGame.Companion.GameStatus.FINISHED) {

        // Print the field
        game.printField(false)

        // Set / delete mine mark
        game.receiveCommand()

        // Check Game
        game.evaluateGame()
    }
}