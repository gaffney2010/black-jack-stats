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
    }
}