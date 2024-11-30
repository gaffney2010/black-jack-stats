import javax.swing.*
import javax.swing.border.LineBorder
import java.awt.Color
import java.awt.event.ActionListener
import java.awt.Font
import kotlin.random.Random

data class Card(val denom: Char);
val all_denoms = listOf('A', '2', '3', '4', '5', '6', '7', '8', '9', 'T')
val all_cards = all_denoms.map { Card(it) }

enum class Player {
    Dealer, Human
}

enum class PlayerBoth {
    Dealer, Human, Both
}

enum class Result {
    Human, Dealer, Tie, HumanBust, DealerBust
}

enum class Button {
    Hit, Stand, Deal
}

fun cardValue(card: Card) : Int {
    return when (card.denom) {
        'A' -> 1
        'T' -> 10
        else -> card.denom.toString().toInt()
    }
}

open class Glyph(val x: Int, val y: Int) {
    val children: MutableList<Glyph> = mutableListOf()

    fun <T : Glyph> addChild(child: T): T {
        children.add(child)
        return child
    }

    // Method to draw the glyph and its children
    fun draw(frame: JFrame, offX: Int = 0, offY: Int = 0) {
        draw_this(frame, offX + x, offY + y)
        for (child in children) {
            child.draw(frame, offX + x, offY + y)
        }
    }

    // Draw method for children with adjusted positions
    open fun draw_this(frame: JFrame, offX: Int, offY: Int) {}

    open fun erase(frame: JFrame) {}
}

class CardGlyph(x: Int, y: Int, val card: Card) : Glyph(x, y) {
    val label = JLabel(card.denom.toString())

    init {
        label.border = LineBorder(Color.BLACK, 2)
        label.horizontalAlignment = JLabel.CENTER
        label.verticalAlignment = JLabel.CENTER
        label.font = Font("Arial", Font.PLAIN, 16)
        label.isVisible = true
    }

    override fun draw_this(frame: JFrame, offX: Int, offY: Int) {
        label.setBounds(offX, offY, 20, 20)
        frame.add(label)
    }

    override fun erase(frame: JFrame) {
        frame.remove(label)
    }
}

class HandGlyph(x: Int, y: Int) : Glyph(x, y) {
    fun addCard(card: Card) : Glyph {
        return addChild(CardGlyph(25*children.size, 0, card))
    }

    fun clear(frame: JFrame) {
        for (child in children) {
            child.erase(frame)
        }
        children.clear()
    }
}

class StatusGlyph(x: Int, y: Int) : Glyph(x, y) {
    val textArea = JTextArea()
    val scrollPane = JScrollPane(textArea)

    init {
        textArea.isEditable = false // Make the text area read-only
    }

    fun appendText(text: String) {
        textArea.append(text)
        textArea.caretPosition = textArea.document.length
    }

    override fun draw_this(frame: JFrame, offX: Int, offY: Int) {
        scrollPane.setBounds(offX, offY, 200, 100)
        frame.add(scrollPane)
    }
}

class ButtonGlyph(x: Int, y: Int, buttonText: String) : Glyph(x, y) {
    val button = JButton(buttonText)

    fun addListener(listener: ActionListener) {
        button.addActionListener(listener)
    }

    override fun draw_this(frame: JFrame, offX: Int, offY: Int) {
        button.setBounds(offX, offY, 100, 30)
        frame.add(button)
    }

    fun enable() {
        button.isEnabled = true
    }

    fun disable() {
        button.isEnabled = false
    }
}

class Shoe() {
    var distribution = all_cards.map { Pair(it, if (it.denom == 'T') 4 else 1) }.toMap()

    fun drawCard() : Card {
        val totalSum = distribution.values.sum()
        val normalizedDistribution = distribution.mapValues { (_, value) ->
            value.toDouble() / totalSum
        }

        val randomValue = Random.nextDouble() // Random value between 0.0 and 1.0
        var cumulativeProbability = 0.0
    
        for ((key, probability) in normalizedDistribution) {
            cumulativeProbability += probability
            if (randomValue <= cumulativeProbability) {
                return key
            }
        }

        throw IllegalArgumentException("Invalid distribution.  This should never happen.")
    }
}

class View(val frame: JFrame) {
    val screen = Glyph(0, 0)
    private lateinit var dispatcher: (Button) -> Unit
    val dealer: HandGlyph
    val human: HandGlyph
    val status: StatusGlyph
    val hitButton: ButtonGlyph
    val standButton: ButtonGlyph
    val dealButton: ButtonGlyph

    init {
        val playArea = screen.addChild(Glyph(0, 0))
        dealer = playArea.addChild(HandGlyph(15, 15))
        human = playArea.addChild(HandGlyph(15, 60))
    
        status = playArea.addChild(StatusGlyph(200, 0))
        status.appendText("Starting game\n")

        val buttons = playArea.addChild(Glyph(0, 100))
        hitButton = buttons.addChild(ButtonGlyph(0, 0, "Hit"))
        hitButton.addListener { dispatch(Button.Hit) }
        standButton = buttons.addChild(ButtonGlyph(100, 0, "Stand"))
        standButton.addListener { dispatch(Button.Stand) }
        dealButton = buttons.addChild(ButtonGlyph(200, 0, "Deal"))
        dealButton.addListener { dispatch(Button.Deal) }
    }

    fun setDispatcher(dispatcher: (Button) -> Unit) {
        this.dispatcher = dispatcher
    }

    private fun dispatch(button: Button) {
        if (!::dispatcher.isInitialized) {
            throw IllegalStateException("dispatcher is not initialized")
        }
        dispatcher(button)
    }

    fun draw() {
        screen.draw(frame)
        frame.repaint()
    }

    fun updateHand(player: Player, card: Card) {
        if (player == Player.Dealer) {
            dealer.addCard(card)
            status.appendText("Dealer draws ${card.denom}\n")

        } else {
            human.addCard(card)
            status.appendText("Player draws ${card.denom}\n")
        }
    }

    fun updateStatus(dealerShowing: Int, humanShowing: Int, playerBoth: PlayerBoth = PlayerBoth.Both) {
        if (playerBoth == PlayerBoth.Both || playerBoth == PlayerBoth.Dealer) {
            status.appendText("Dealer showing $dealerShowing\n")
        }
        if (playerBoth == PlayerBoth.Both || playerBoth == PlayerBoth.Human) {
            status.appendText("Player showing $humanShowing\n")
        }
    }

    fun updateEndOfHand(result: Result) {
        status.appendText(when (result) {
            Result.Human -> "Player wins\n"
            Result.Dealer -> "Dealer wins\n"
            Result.Tie -> "Tie\n"
            Result.HumanBust -> "Player busts\n"
            Result.DealerBust -> "Dealer wins\n"
        })
        hitButton.disable()
        standButton.disable()
        dealButton.enable()
    }

    fun updateStartOfHand() {
        dealer.clear(frame)
        human.clear(frame)
        hitButton.enable()
        standButton.enable()
        dealButton.disable()
    }
}

class Model() {
    val shoe = Shoe()

    var dealerShowing = 0
    var humanShowing = 0
    var isEndOfHand = false

    private fun updateHand(player: Player, card: Card) {
        if (player == Player.Dealer) {
            dealerShowing += cardValue(card)
        } else {
            humanShowing += cardValue(card)
        }
    }

    fun drawCardUpdateHand(player: Player) : Card {
        val card = shoe.drawCard()
        updateHand(player, card)
        return card
    }

    fun showing(player: Player) : Int {
        return when (player) {
            Player.Dealer -> dealerShowing
            Player.Human -> humanShowing
        }
    }

    fun isBust(player: Player) : Boolean {
        return when (player) {
            Player.Dealer -> dealerShowing > 21
            Player.Human -> humanShowing > 21
        }
    }
    
    fun dealerShouldDraw() : Boolean {
        return dealerShowing < 17
    }

    fun result() : Result {
        return when {
            humanShowing > 21 -> Result.HumanBust
            dealerShowing > 21 -> Result.DealerBust
            humanShowing > dealerShowing -> Result.Human
            humanShowing < dealerShowing -> Result.Dealer
            else -> Result.Tie
        }
    }

    fun updateEndOfHand() {
        isEndOfHand = true
    }

    fun updateStartOfHand() {
        dealerShowing = 0
        humanShowing = 0
        isEndOfHand = false
    }
}

class Controller(val model: Model, val view: View) {
    fun hit() {
        val card = model.drawCardUpdateHand(Player.Human)
        view.updateHand(Player.Human, card)
        view.updateStatus(model.showing(Player.Dealer), model.showing(Player.Human), PlayerBoth.Human)
        if (model.isBust(Player.Human)) {
            view.updateEndOfHand(Result.HumanBust)
            val card = model.drawCardUpdateHand(Player.Dealer)
            view.updateHand(Player.Dealer, card)
            model.updateEndOfHand()
        }
        view.draw()
    }

    fun stand() {
        while (model.dealerShouldDraw()) {
            val card = model.drawCardUpdateHand(Player.Dealer)
            view.updateHand(Player.Dealer, card)
        }
        view.updateStatus(model.showing(Player.Dealer), model.showing(Player.Human), PlayerBoth.Both)
        model.updateEndOfHand()
        view.updateEndOfHand(model.result())
        view.draw()
    }

    fun deal() {
        view.updateStartOfHand()
        model.updateStartOfHand()
        var card = model.drawCardUpdateHand(Player.Dealer)
        view.updateHand(Player.Dealer, card)
        card = model.drawCardUpdateHand(Player.Human)
        view.updateHand(Player.Human, card)
        card = model.drawCardUpdateHand(Player.Human)
        view.updateHand(Player.Human, card)
        view.updateStatus(model.showing(Player.Dealer), model.showing(Player.Human))
        view.draw()
    }

    fun dispatch(button: Button) {
        when (button) {
            Button.Hit -> hit()
            Button.Stand -> stand()
            Button.Deal -> deal()
        }
    }
}

fun main() {
    // Create the main frame
    val frame = JFrame("Black Jack")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.setSize(400, 300)
    frame.layout = null // Disable layout manager for absolute positioning

    val view = View(frame)
    val model = Model()
    val controller = Controller(model, view)
    view.setDispatcher(controller::dispatch)

    // Make the frame visible
    view.draw()
    frame.isVisible = true
}
