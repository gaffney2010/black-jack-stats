import javax.swing.*


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
