package game

import gameAI.AI
import gameAI.ArtificialIntelligence
import sample.Controller
import kotlin.concurrent.thread
import kotlin.random.Random

open class Game(player1: Player, player2: Player, private val controller: Controller) : GameCore(player1, player2), Runnable {

    private var ai: ArtificialIntelligence? = null
    private var startPlayer : Player? = null
    private var gameThread: Thread? = null

    init {
        if (player1.type == PlayerType.AI || player2.type == PlayerType.AI) {
            ai = AI(this)
        }
    }

    override fun start() {
        prepareBoard()
        prepareGame()
        startPlayer = if (Random.nextInt(1, 3) == 1) player1 else player2
        when (startPlayer) {
            player1 -> {
                ai?.filename = "p1start.bin"
                ai?.load()
            }
            player2 -> {
                ai?.filename = "p2start.bin"
                ai?.load()
            }
        }
        currentPlayer = startPlayer
        gameThread = thread {
            try {
                run()
            } catch (ignored: InterruptedException) {
            } /*catch (e: Throwable) {
                println("Game.kt:40 - " + e.localizedMessage)
            }*/
        }

    }

    override fun run() {
        var lastMoveWasCapture = false
        var lastCapturePawn = -1
        while (!gameOver) {
            checkCaptureObligation()
            controller.zmienGracza(currentPlayer!!.number)
            ruch@while (true) {
                controller.koloruj(matrix)
                val nextMove = when (currentPlayer!!.type) {
                    PlayerType.AI -> {
                        val startFields: Array<Int>
                        startFields = if (captureRequired) {
                            val temp = mutableListOf<Int>()
                            for (move in captureMoves) {
                                temp.add(move.first)
                            }
                            temp.toTypedArray()
                        } else {
                            arrayOf()
                        }
                        Thread.sleep(700)
                        ai!!.move(startFields)
                    }
                    PlayerType.Human -> {
                        controller.zacznijRuch()
                        var move = controller.pobierzRuch()
                        while (move == null) {
                            Thread.sleep(100)
                            move = controller.pobierzRuch()
                        }
                        move
                    }
                }
                try {
                    lastMoveWasCapture = captureRequired
                    if(lastMoveWasCapture)
                        lastCapturePawn = nextMove.second
                    move(nextMove.first, nextMove.second)
                    if (getOponent(currentPlayer)?.type == PlayerType.AI) {
                        ai?.setEnemyMove(generateBoardHash(), nextMove)
                    }
                    controller.wyswietlWiadomosc("")
                    if(currentPlayer?.type == PlayerType.AI){
                        ai?.update(generateBoardHash())
                    }
                } catch (e: InvalidMoveException) {
                    controller.wyswietlWiadomosc("Nie można wykonać ruchu")
                    continue@ruch
                } catch (e: CaptureRequiredException) {
                    controller.wyswietlWiadomosc("Należy wykonać bicie")
                    continue@ruch
                }
                catch (e: Throwable){
                    println("Game.kt:100 - " + e.localizedMessage)
                }
                checkCaptureObligation()
                if(!(lastMoveWasCapture && captureRequired && captureMoves.find { it.first == lastCapturePawn } is Pair<Int, Int>))
                    break@ruch
            }
            currentPlayer = getOponent(currentPlayer)
            Thread.sleep(100)
        }
        controller.koloruj(matrix)
        controller.wyswietlWiadomosc("Koniec gry! Wygrał gracz ${winner!!.number}")
        ai?.updateState(winner == startPlayer && startPlayer?.type == PlayerType.AI)
        ai?.save()
    }

    fun close() {
        gameThread?.interrupt()
        ai?.save()
    }
}