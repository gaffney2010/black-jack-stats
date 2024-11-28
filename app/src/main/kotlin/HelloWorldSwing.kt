import javax.swing.JFrame
import javax.swing.JLabel

fun main() {
    // Create the main frame
    val frame = JFrame("Hello Swing in Kotlin")

    // Add a label
    val label = JLabel("Hello, World!", JLabel.CENTER)
    frame.add(label)

    // Set frame properties
    frame.setSize(400, 200) // Width: 400, Height: 200
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.isVisible = true
}
