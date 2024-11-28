import javax.swing.*
import javax.swing.border.LineBorder
import java.awt.Color
import java.awt.Font

open class Glyph(val x: Int, val y: Int) {
    val children: MutableList<Glyph> = mutableListOf()

    fun addChild(child: Glyph) {
        children.add(child)
    }

    // Method to draw the glyph and its children
    fun draw(frame: JFrame, off_x: Int = 0, off_y: Int = 0) {
        draw_this(frame, off_x + x, off_y + y)
        for (child in children) {
            child.draw(frame, off_x + x, off_y + y)
        }
    }

    // Draw method for children with adjusted positions
    open fun draw_this(frame: JFrame, off_x: Int, off_y: Int) {}
}

class Card(x: Int, y: Int, val char: String) : Glyph(x, y) {
    override fun draw_this(frame: JFrame, off_x: Int, off_y: Int) {
        val label = JLabel(char)
        label.setBounds(off_x, off_y, 20, 20)
        label.border = LineBorder(Color.BLACK, 2)
        label.horizontalAlignment = JLabel.CENTER
        label.verticalAlignment = JLabel.CENTER
        label.font = Font("Arial", Font.PLAIN, 16)
        label.isVisible = true
        frame.add(label)
    }
}

class Hand(x: Int, y: Int) : Glyph(x, y) {
    fun addCard(char: String) {
        children.add(Card(25*children.size, 0, char))
    }
}


fun main() {
    // Create the main frame
    val frame = JFrame("Box with Centered Text in Kotlin")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.setSize(400, 300)
    frame.layout = null // Disable layout manager for absolute positioning

    var hand = Hand(15, 15)
    hand.addCard("A")
    hand.addCard("9")
    hand.draw(frame)

    // Make the frame visible
    frame.isVisible = true
}
