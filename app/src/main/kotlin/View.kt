import javax.swing.*
import javax.swing.border.LineBorder
import javax.swing.table.DefaultTableModel
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

    fun updateProfit(frame: JFrame, profit: Float) {
        erase(frame)
        this.profit += profit
        label.text = "Profit: ${this.profit}"
    }

    override fun draw_this(frame: JFrame, offX: Int, offY: Int) {
        label.setBounds(offX, offY, 100, 20)
        frame.add(label)
    }

    override fun erase(frame: JFrame) {
        frame.remove(label)
    }
}

class ProbabilityGlyph(x: Int, y: Int) : Glyph(x, y) {
    val frame = JFrame("Strategies")
    val dealerHandHeaders = listOf("P") + all_denoms.map { " $it " }
    val tableModel = DefaultTableModel()
    val table = JTable(tableModel)

    init {
        dealerHandHeaders.forEach { tableModel.addColumn(it) }
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.add(JScrollPane(table))
        frame.setSize(300, 670)
        frame.isVisible = true
    }

    fun updateProbability(frame: JFrame, rows: List<List<String>>) {
        erase(frame)
        tableModel.setRowCount(0)
        rows.forEach { row -> tableModel.addRow(row.toTypedArray()) }
    }
}

class View(val frame: JFrame) {
    val screen = Glyph(0, 0)
    private lateinit var dispatcher: (Button) -> Unit
    val distribution: DistributionGlyph
    val profit: ProfitGlyph
    val dealer: HandGlyph
    val humans: List<HandGlyph>
    val status: StatusGlyph
    val strategy: ProbabilityGlyph
    val hitButton: ButtonGlyph
    val standButton: ButtonGlyph
    val dealButton: ButtonGlyph
    val doubleButton: ButtonGlyph
    val splitButton: ButtonGlyph
    val newShoe: ButtonGlyph

    var playerHandIndex = 1

    init {
        distribution = screen.addChild(DistributionGlyph(0, 0))

        profit = screen.addChild(ProfitGlyph(300, 10))

        val playArea = screen.addChild(Glyph(0, 60))
        dealer = playArea.addChild(HandGlyph(15, 15))
        // Only support 5 of these
        humans = listOf(
            playArea.addChild(HandGlyph(15, 60)),
            playArea.addChild(HandGlyph(15, 90)),
            playArea.addChild(HandGlyph(15, 120)),
            playArea.addChild(HandGlyph(15, 150)),
            playArea.addChild(HandGlyph(15, 180)),
        )
    
        status = screen.addChild(StatusGlyph(300, 60))
        status.appendText("Starting game\n")

        // This creates a second window for some reason
        strategy = screen.addChild(ProbabilityGlyph(0, 0))

        val buttons = playArea.addChild(Glyph(0, 240))
        hitButton = buttons.addChild(ButtonGlyph(0, 0, "Hit"))
        hitButton.addListener { dispatch(Button.Hit) }
        standButton = buttons.addChild(ButtonGlyph(100, 0, "Stand"))
        standButton.addListener { dispatch(Button.Stand) }
        dealButton = buttons.addChild(ButtonGlyph(200, 0, "Deal"))
        dealButton.addListener { dispatch(Button.Deal) }
        doubleButton = buttons.addChild(ButtonGlyph(0, 30, "Double"))
        doubleButton.addListener { dispatch(Button.Double) }
        splitButton = buttons.addChild(ButtonGlyph(100, 30, "Split")) 
        splitButton.addListener { dispatch(Button.Split) }
        newShoe = buttons.addChild(ButtonGlyph(200, 30, "New Shoe"))
        newShoe.addListener { dispatch(Button.NewShoe) }
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

    fun addCard(player: Player, card: Card) {
        if (player.index == DEALER_INDEX) {
            dealer.addCard(card)
            status.appendText("Dealer draws ${card.denom}\n")
        } else {
            humans[player.index-1].addCard(card)
            status.appendText("Player hand ${player.index} draws ${card.denom}\n")
        }
    }

    fun updateDistribution(distribution: Map<Card, Int>) {
        this.distribution.updateDistribution(frame, distribution)
    }

    fun updateStrategy(shoe: Shoe) {
        val rows = rowsFromShoe(shoe)
        strategy.updateProbability(frame, rows)
    }

    fun updateButtons(buttons: List<Button>) {
        if (buttons.contains(Button.Deal)) {
            dealButton.enable()
        } else {
            dealButton.disable()
        }
        if (buttons.contains(Button.Hit)) {
            hitButton.enable()
        } else {
            hitButton.disable()
        }
        if (buttons.contains(Button.Stand)) {
            standButton.enable()
        } else {
            standButton.disable()
        }
        if (buttons.contains(Button.Double)) {
            doubleButton.enable()
        } else {
            doubleButton.disable()
        }
        if (buttons.contains(Button.Split)) {
            splitButton.enable()
        } else {
            splitButton.disable()
        }
    }

    fun updateResult(result: Result, hand: Int) {
        status.appendText(when (result) {
            Result.Human -> "Player wins hand $hand\n"
            Result.Dealer -> "Dealer wins hand $hand\n"
            Result.Tie -> "Push on hand $hand\n"
            Result.HumanBust -> "Player busts hand $hand\n"
            Result.DealerBust -> "Dealer busts hand $hand\n"
            Result.DealerBlackjack -> "Dealer blackjack\n"
            Result.HumanBlackjack -> "Player blackjack hand $hand\n"
            Result.HumanBlackjackTwoToOne -> "Player blackjack 2:1 hand $hand\n"
            Result.DoubleWin -> "Double win on hand $hand\n"
            Result.DoubleLoss -> "Double loss on hand $hand\n"
            Result.DoubleDealerBust -> "Double - dealer bust on hand $hand\n" 
            Result.DoubleHumanBust -> "Double - player bust on hand $hand\n"
        })
    }

    fun updateProfit(profit: Float) {
        this.profit.updateProfit(frame, profit)
    }

    fun updateEndOfHand() {}

    fun advanceToNextHand() {
        // TODO: Add a hand indicator
        status.appendText("Next hand\n\n")
        playerHandIndex += 1
    }

    fun updateHand(player: Player, card: Card) {
        assert (player.index > DEALER_INDEX)
        humans[player.index - 1].clear(frame)
        humans[player.index - 1].addCard(card)
    }

    fun updateStartOfHand() {
        playerHandIndex = 1
        dealer.clear(frame)
        for (human in humans) {
            human.clear(frame)
        }
        status.appendText("\n Dealing cards...\n")
    }

    fun updateStatus(dealerShowing: HandValue, humanShowing: HandValue, playerBoth: PlayerBoth = PlayerBoth.Both) {
        if (playerBoth == PlayerBoth.Both || playerBoth == PlayerBoth.Dealer) {
            status.appendText("Dealer showing ${handValueStr(dealerShowing)}\n")
        }
        if (playerBoth == PlayerBoth.Both || playerBoth == PlayerBoth.Human) {
            status.appendText("Player showing ${handValueStr(humanShowing)}\n")
        }
    }

    fun writeText(text: String) {
        status.appendText(text)
    }
}
