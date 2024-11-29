import javax.swing.*
import javax.swing.border.LineBorder
import java.awt.Color
import java.awt.Font
import kotlin.random.Random

data class Card(val denom: Char);
val all_denoms = listOf('A', '2', '3', '4', '5', '6', '7', '8', '9', 'T')
val all_cards = all_denoms.map { Card(it) }

open class Glyph(val x: Int, val y: Int) {
    val children: MutableList<Glyph> = mutableListOf()

    fun addChild(child: Glyph) {
        children.add(child)
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
}

class CardGlyph(x: Int, y: Int, val card: Card) : Glyph(x, y) {
    override fun draw_this(frame: JFrame, offX: Int, offY: Int) {
        val label = JLabel(card.denom.toString())
        label.setBounds(offX, offY, 20, 20)
        label.border = LineBorder(Color.BLACK, 2)
        label.horizontalAlignment = JLabel.CENTER
        label.verticalAlignment = JLabel.CENTER
        label.font = Font("Arial", Font.PLAIN, 16)
        label.isVisible = true
        frame.add(label)
    }
}

class HandGlyph(x: Int, y: Int) : Glyph(x, y) {
    fun addCard(card: Card) = children.add(CardGlyph(25*children.size, 0, card))
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

fun main() {
    // Create the main frame
    val frame = JFrame("Box with Centered Text in Kotlin")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.setSize(400, 300)
    frame.layout = null // Disable layout manager for absolute positioning

    val shoe = Shoe()
    var dealer = HandGlyph(15, 15)
    dealer.addCard(shoe.drawCard())
    dealer.addCard(Card(' '))
    dealer.draw(frame)

    var player = HandGlyph(15, 60)
    player.addCard(shoe.drawCard())
    player.addCard(shoe.drawCard())
    player.draw(frame)

    // Make the frame visible
    frame.isVisible = true
}
