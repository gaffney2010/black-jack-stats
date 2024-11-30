import javax.swing.*
import javax.swing.border.LineBorder
import java.awt.Color
import java.awt.event.ActionListener
import java.awt.Font


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

class CountGlyph(x: Int, y: Int, val count: Int) : Glyph(x, y) {
    val label = JLabel(count.toString())

    init {
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
        scrollPane.setBounds(offX, offY, 200, 300)
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

class DistributionEntryGlyph(x: Int, y: Int, val card: Card, val count: Int) : Glyph(x, y) {
    val cardGlyph = CardGlyph(0, 0, card)
    val countGlyph = CountGlyph(0, 30, count)

    init {
        children.add(cardGlyph)
        children.add(countGlyph)
    }

    override fun erase(frame: JFrame) {
        cardGlyph.erase(frame)
        countGlyph.erase(frame)
    }
}

class DistributionGlyph(x: Int, y: Int) : Glyph(x, y) {
    fun updateDistribution(frame: JFrame, distribution: Map<Card, Int>) {
        for (child in children) {
            child.erase(frame)
        }
        children.clear()
        for ((card, count) in distribution) {
            addChild(DistributionEntryGlyph(25*children.size, 0, card, count))
        }
    }
}


class ProfitGlyph(x: Int, y: Int) : Glyph(x, y) {
    var profit = 0.0f
    val label = JLabel("Profit: $profit")

    init {
        label.horizontalAlignment = JLabel.CENTER
        label.verticalAlignment = JLabel.CENTER
        label.font = Font("Arial", Font.PLAIN, 16)
        label.isVisible = true
    }

    fun updateProfit(frame: JFrame,profit: Float) {
        erase(frame)
        this.profit = profit
        label.text = "Profit: $profit"
    }

    override fun draw_this(frame: JFrame, offX: Int, offY: Int) {
        label.setBounds(offX, offY, 100, 20)
        frame.add(label)
    }

    override fun erase(frame: JFrame) {
        frame.remove(label)
    }
}

class View(val frame: JFrame) {
    val screen = Glyph(0, 0)
    private lateinit var dispatcher: (Button) -> Unit
    val distribution: DistributionGlyph
    val profit: ProfitGlyph
    val dealer: HandGlyph
    val human: HandGlyph
    val status: StatusGlyph
    val hitButton: ButtonGlyph
    val standButton: ButtonGlyph
    val dealButton: ButtonGlyph

    init {
        distribution = screen.addChild(DistributionGlyph(0, 0))

        profit = screen.addChild(ProfitGlyph(300, 10))

        val playArea = screen.addChild(Glyph(0, 60))
        dealer = playArea.addChild(HandGlyph(15, 15))
        human = playArea.addChild(HandGlyph(15, 60))
    
        status = screen.addChild(StatusGlyph(300, 60))
        status.appendText("Starting game\n")

        val buttons = playArea.addChild(Glyph(0, 240))
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

    fun updateDistribution(distribution: Map<Card, Int>) {
        this.distribution.updateDistribution(frame, distribution)
    }

    fun updateProfit(profit: Float) {
        this.profit.updateProfit(frame, profit)
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

    fun updateStatus(dealerShowing: HandValue, humanShowing: HandValue, playerBoth: PlayerBoth = PlayerBoth.Both) {
        if (playerBoth == PlayerBoth.Both || playerBoth == PlayerBoth.Dealer) {
            status.appendText("Dealer showing ${handValueStr(dealerShowing)}\n")
        }
        if (playerBoth == PlayerBoth.Both || playerBoth == PlayerBoth.Human) {
            status.appendText("Player showing ${handValueStr(humanShowing)}\n")
        }
    }

    fun updateEndOfHand(result: Result) {
        status.appendText(when (result) {
            Result.Human -> "Player wins\n"
            Result.Dealer -> "Dealer wins\n"
            Result.Tie -> "Tie\n"
            Result.HumanBust -> "Player busts\n"
            Result.DealerBust -> "Dealer busts\n"
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
        status.appendText("\n Dealing cards...\n")
    }
}
