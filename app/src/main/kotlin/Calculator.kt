import javax.swing.*
import javax.swing.table.DefaultTableModel

sealed class Expression {
    abstract fun evaluate(variables: MutableMap<String, Double>): Double
    abstract fun print(): String

    operator fun plus(other: Expression): Expression {
        return BinaryExpression(this, other, Operation.ADD)
    }

    operator fun minus(other: Expression): Expression {
        return BinaryExpression(this, other, Operation.SUBTRACT)
    }

    operator fun times(other: Expression): Expression {
        return BinaryExpression(this, other, Operation.MULTIPLY)
    }

    operator fun div(other: Expression): Expression {
        return BinaryExpression(this, other, Operation.DIVIDE)
    }
}

class Product(val terms: List<Expression>) : Expression() {
    override fun evaluate(variables: MutableMap<String, Double>): Double {
        return terms.map { it.evaluate(variables) }.reduce { acc, value -> acc * value }
    }

    override fun print(): String {
        return terms.joinToString(" * ") { it.print() }
    }
}

class Sum(val terms: List<Expression>) : Expression() {
    override fun evaluate(variables: MutableMap<String, Double>): Double {
        return terms.map { it.evaluate(variables) }.reduce { acc, value -> acc + value }
    }

    override fun print(): String {
        return terms.joinToString(" + ") { it.print() }
    }
}

class Variable(val name: String) : Expression() {
    override fun evaluate(variables: MutableMap<String, Double>): Double {
        return variables[name] ?: throw IllegalArgumentException("Variable $name not found in provided values")
    }

    override fun print(): String {
        return name
    }
}

class Constant(private val value: Double) : Expression() {
    override fun evaluate(variables: MutableMap<String, Double>): Double {
        return value
    }

    override fun print(): String {
        return value.toString()
    }
}

enum class Operation(val symbol: String) {
    ADD("+"),
    SUBTRACT("-"),
    MULTIPLY("*"),
    DIVIDE("/")
}

class BinaryExpression(
    private val left: Expression,
    private val right: Expression,
    private val operation: Operation
) : Expression() {
    override fun evaluate(variables: MutableMap<String, Double>): Double {
        val leftValue = left.evaluate(variables)
        val rightValue = right.evaluate(variables)

        return when (operation) {
            Operation.ADD -> leftValue + rightValue
            Operation.SUBTRACT -> leftValue - rightValue
            Operation.MULTIPLY -> leftValue * rightValue
            Operation.DIVIDE -> leftValue / rightValue
        }
    }

    override fun print(): String {
        return "(${left.print()} ${operation.symbol} ${right.print()})"
    }
}

// Example usage
// fun main() {
//     val x = Variable("x")
//     val y = Variable("y")
//     val c1 = Constant(10.0)
//     val c2 = Constant(5.0)

//     val expr = (x + c1) - (y / c2) // Expression: (x + 10) - (y / 5)

//     println("Expression: ${expr.print()}") // Outputs: Expression: ((x + 10.0) - (y / 5.0))

//     val variables = mutableMapOf(
//         "x" to 2.0,
//         "y" to 20.0
//     )

//     println("Value: ${expr.evaluate(variables)}") // Outputs: Value: 8.0
// }

fun prettyPrintTable(headers: List<String>, rows: List<List<String>>) {
    val columnWidths = mutableListOf<Int>()
    
    headers.forEachIndexed { index, header ->
        val maxInColumn = rows.map { it[index].length }.maxOrNull() ?: 0
        columnWidths.add(maxOf(header.length, maxInColumn))
    }

    val horizontalSeparator = columnWidths.joinToString("+", "+", "+") { "-".repeat(it) }

    println(horizontalSeparator)
    println(headers.joinToString("|", "|", "|") { it.padEnd(columnWidths[headers.indexOf(it)]) })
    println(horizontalSeparator)
    
    rows.forEach { row ->
        println(row.joinToString("|", "|", "|") { it.padEnd(columnWidths[row.indexOf(it)]) })
    }
    println(horizontalSeparator)
}

fun standBfs(hand: Hand, playerShowing: Int, variables: MutableMap<String, Variable>, wrapper: MutableMap<String, Int>, skipA: Boolean = false, skipT: Boolean = false) {
    // print("hand: ${hand.print()}\n")
    if (!hand.shouldDealerDraw()) {
        val key = hand.cards.drop(1).map {it.denom}.sorted().toCharArray().joinToString("")
        if (hand.isBust()) {
            wrapper[key] = wrapper.getOrDefault(key, 0) + 1
        } else if (hand.value().value < playerShowing) {
            wrapper[key] = wrapper.getOrDefault(key, 0) + 1
        } else if (hand.value().value > playerShowing) {
            wrapper[key] = wrapper.getOrDefault(key, 0) - 1
        }
        return
    }

    all_cards.filter { card ->
        !(skipA && card.denom == 'A') && !(skipT && card.denom == 'T')
    }.forEach { card ->
        hand.addCard(card)
        standBfs(hand, playerShowing, variables, wrapper)
        hand.popCard()
    }
}

fun computeStand(variables: MutableMap<String, Variable>, playerShowing: Int, dealerShowing: Card): Expression {
    val hand = Hand(-1)
    hand.addCard(dealerShowing)

    val skipA = dealerShowing.denom == 'T'
    val skipT = dealerShowing.denom == 'A'

    val wrapper = mutableMapOf<String, Int>()
    standBfs(hand, playerShowing, variables, wrapper, skipA = skipA, skipT = skipT)
    val summands = mutableListOf<Expression>()
    for ((key, value) in wrapper) {
        summands.add(Constant(value.toDouble()) as Expression * Product(key.toCharArray().map { variables["p_$it"] as Expression }) as Expression)
    }
    var expr: Expression = Sum(summands)
    if (skipA) {
        expr /= (Constant(1.0) - variables["p_A"] as Expression)
    }
    if (skipT) {
        expr /= (Constant(1.0) - variables["p_T"] as Expression)
    }
    return expr
}

fun computeHit(variables: MutableMap<String, Variable>, playerShowing: Int, dealerShowing: Card): Expression {
    val summands = mutableListOf<Expression>()
    all_cards.forEach {
        var newShowing = playerShowing + cardValue(it, /*canBeSoft=*/true)
        if (it.denom == 'A') {
            if (newShowing == 21 + 11) {
                // Dumb edge case
                summands.add(Constant(-1.0) as Expression * variables["p_${it.denom}"] as Expression)
            } else if (newShowing > 21) {
                summands.add(variables["p_${it.denom}"] as Expression * variables["ehi_${newShowing-10}_${dealerShowing.denom}"] as Expression)
            } else {
                summands.add(variables["p_${it.denom}"] as Expression * variables["ehi_s${newShowing}_${dealerShowing.denom}"] as Expression)
            }
        } else {
            if (newShowing > 21) {
                summands.add(Constant(-1.0) as Expression * variables["p_${it.denom}"] as Expression)
            } else {
                summands.add(variables["p_${it.denom}"] as Expression * variables["ehi_${newShowing}_${dealerShowing.denom}"] as Expression)
            }
        }
    }
    return Sum(summands)
}

fun computeSoftHit(variables: MutableMap<String, Variable>, playerShowing: Int, dealerShowing: Card): Expression {
    val summands = mutableListOf<Expression>()
    all_cards.forEach {
        val newShowing = playerShowing + cardValue(it, /*canBeSoft=*/false)
        if (newShowing > 21) {
            summands.add(variables["p_${it.denom}"] as Expression * variables["ehi_${newShowing-10}_${dealerShowing.denom}"] as Expression)
        } else {
            summands.add(variables["p_${it.denom}"] as Expression * variables["ehi_s${newShowing}_${dealerShowing.denom}"] as Expression)
        }
    }
    return Sum(summands)
}

fun computeDouble(variables: MutableMap<String, Variable>, playerShowing: Int, dealerShowing: Card): Expression {
    val summands = mutableListOf<Expression>()
    all_cards.forEach {
        var newShowing = playerShowing + cardValue(it, /*canBeSoft=*/true)
        if (newShowing > 21 && it.denom == 'A') {
            newShowing -= 10
        }
        if (newShowing > 21) {
            summands.add(Constant(-2.0) as Expression * variables["p_${it.denom}"] as Expression)
        } else {
            summands.add(Constant(2.0) as Expression * variables["p_${it.denom}"] as Expression * variables["est_${newShowing}_${dealerShowing.denom}"] as Expression)
        }
    }
    return Sum(summands)
}

fun computeSoftDouble(variables: MutableMap<String, Variable>, playerShowing: Int, dealerShowing: Card): Expression {
    val summands = mutableListOf<Expression>()
    all_cards.forEach {
        val newShowing = playerShowing + cardValue(it, /*canBeSoft=*/false)
        if (newShowing > 21) {
            summands.add(Constant(2.0) as Expression * variables["p_${it.denom}"] as Expression * variables["est_${newShowing-10}_${dealerShowing.denom}"] as Expression)
        } else {
            summands.add(Constant(2.0) as Expression * variables["p_${it.denom}"] as Expression * variables["est_s${newShowing}_${dealerShowing.denom}"] as Expression)
        }
    }
    return Sum(summands)
}

fun computeSplit(variables: MutableMap<String, Variable>, playerShowing: Int, dealerShowing: Card): Expression {
    if (playerShowing % 2 != 0) {
        return Constant(0.0)
    }
    val halfCardValue = playerShowing / 2
    val summands = mutableListOf<Expression>()
    all_cards.forEach { card ->
        if (cardValue(card, canBeSoft = false) != halfCardValue) {
            var newShowing = halfCardValue + cardValue(card, canBeSoft = true)
            if (card.denom == 'A') {
                summands.add(
                    variables["p_A"] as Expression *
                            variables["ehi_s${newShowing}_${dealerShowing.denom}"] as Expression
                )
            } else {
                summands.add(
                    variables["p_${card.denom}"] as Expression *
                            variables["ehi_${newShowing}_${dealerShowing.denom}"] as Expression
                )
            }
        }
    }
    val halfVariableName = if (playerShowing == 20) {
        "p_T"
    } else if (playerShowing == 2) {
        "p_A"  // Won't actually use this, but need to set the equation correctly
    } else {
        "p_${playerShowing / 2}"
    }
    return Sum(summands) * Constant(2.0) / (Constant(1.0) - variables[halfVariableName] as Expression)
}

fun computeSplitSoft(variables: MutableMap<String, Variable>, playerShowing: Int, dealerShowing: Card): Expression {
    if (playerShowing != 12) {
        return Constant(0.0)
    }
    val summands = mutableListOf<Expression>()
    all_cards.forEach {
        if (it.denom != 'A') {
            // We only have to think about the ace/non-ace case
            var newShowing = 11 + cardValue(it, /*canBeSoft=*/false)
            summands.add(variables["p_${it.denom}"] as Expression * variables["ehi_s${newShowing}_${dealerShowing.denom}"] as Expression)
        }
    }
    return Sum(summands) * Constant(2.0) / (Constant(1.0) - variables["p_A"] as Expression)
}

data class EquationSet(val eqn_st: MutableMap<String, Expression>, val eqn_hi: MutableMap<String, Expression>, val eqn_do: MutableMap<String, Expression>, val eqn_sp: MutableMap<String, Expression>)

fun createVariables() : EquationSet {
    val variables = mutableMapOf<String, Variable>()

    for (denom in all_denoms) {
        variables["p_$denom"] = Variable("p_$denom")
    }
    for (e in listOf("est", "ehi", "edo", "esp")) {
        for (playerShowing in 2..21) {
            for (dealerShowing in all_denoms) {
                variables["${e}_${playerShowing}_$dealerShowing"] = Variable("${e}_${playerShowing}_$dealerShowing")
                variables["${e}_s${playerShowing}_$dealerShowing"] = Variable("${e}_s${playerShowing}_$dealerShowing")
            }
        }
    }

    var eqns = EquationSet(mutableMapOf(), mutableMapOf(), mutableMapOf(), mutableMapOf())
    for (dealerShowing in all_cards) {
        for (playerShowing in 2..21) {
            eqns.eqn_st["${playerShowing}_${dealerShowing.denom}"] = computeStand(variables, playerShowing, dealerShowing)
            eqns.eqn_hi["${playerShowing}_${dealerShowing.denom}"] = computeHit(variables, playerShowing, dealerShowing)
            eqns.eqn_do["${playerShowing}_${dealerShowing.denom}"] = computeDouble(variables, playerShowing, dealerShowing)
            eqns.eqn_sp["${playerShowing}_${dealerShowing.denom}"] = computeSplit(variables, playerShowing, dealerShowing)
        }
        for (playerShowing in 12..21) {
            eqns.eqn_st["s${playerShowing}_${dealerShowing.denom}"] = computeStand(variables, playerShowing, dealerShowing)
            eqns.eqn_hi["s${playerShowing}_${dealerShowing.denom}"] = computeSoftHit(variables, playerShowing, dealerShowing)
            eqns.eqn_do["s${playerShowing}_${dealerShowing.denom}"] = computeSoftDouble(variables, playerShowing, dealerShowing)
            eqns.eqn_sp["s${playerShowing}_${dealerShowing.denom}"] = computeSplitSoft(variables, playerShowing, dealerShowing)
        }
    }

    return eqns
}

fun evaluateShoe(shoe: Shoe, eqns: EquationSet) : MutableMap<String, Double> {
    // TODO: Do this kotlin style
    var values = mutableMapOf<String, Double>()
    for ((key, value) in shoe.odds()) {
        values["p_${key.denom}"] = value
    }

    // Loop through large hards first
    for (playerShowing in 21 downTo 12) {
        for (dealerShowing in all_denoms) {
            val est = eqns.eqn_st["${playerShowing}_$dealerShowing"]!!.evaluate(values)
            values["est_${playerShowing}_$dealerShowing"] = est
            val ehi = eqns.eqn_hi["${playerShowing}_$dealerShowing"]!!.evaluate(values)
            if (est > ehi) {
                values["ehi_${playerShowing}_$dealerShowing"] = est
            } else {
                values["ehi_${playerShowing}_$dealerShowing"] = ehi
            }
            val edo = eqns.eqn_do["${playerShowing}_$dealerShowing"]!!.evaluate(values)
            if (edo > ehi && edo > est) {
                values["edo_${playerShowing}_$dealerShowing"] = edo
            } else {
                values["edo_${playerShowing}_$dealerShowing"] = values["ehi_${playerShowing}_$dealerShowing"]!!
            }
        }
    }

    // Loop through soft hands next
    for (playerShowing in 21 downTo 12) {
        for (dealerShowing in all_denoms) {
            val est = eqns.eqn_st["s${playerShowing}_$dealerShowing"]!!.evaluate(values)
            values["est_s${playerShowing}_$dealerShowing"] = est
            val ehi = eqns.eqn_hi["s${playerShowing}_$dealerShowing"]!!.evaluate(values)
            if (est > ehi) {
                values["ehi_s${playerShowing}_$dealerShowing"] = est
            } else {
                values["ehi_s${playerShowing}_$dealerShowing"] = ehi
            }
            val edo = eqns.eqn_do["s${playerShowing}_$dealerShowing"]!!.evaluate(values)
            if (edo > ehi && edo > est) {
                values["edo_s${playerShowing}_$dealerShowing"] = edo
            } else {
                values["edo_s${playerShowing}_$dealerShowing"] = values["ehi_s${playerShowing}_$dealerShowing"]!!
            }
        }
    }

    // Then loop through small hard
    for (playerShowing in 11 downTo 3) {
        for (dealerShowing in all_denoms) {
            val est = eqns.eqn_st["${playerShowing}_$dealerShowing"]!!.evaluate(values)
            values["est_${playerShowing}_$dealerShowing"] = est
            val ehi = eqns.eqn_hi["${playerShowing}_$dealerShowing"]!!.evaluate(values)
            if (est > ehi) {
                values["ehi_${playerShowing}_$dealerShowing"] = est
            } else {
                values["ehi_${playerShowing}_$dealerShowing"] = ehi
            }
            val edo = eqns.eqn_do["${playerShowing}_$dealerShowing"]!!.evaluate(values)
            if (edo > ehi && edo > est) {
                values["edo_${playerShowing}_$dealerShowing"] = edo
            } else {
                values["edo_${playerShowing}_$dealerShowing"] = values["ehi_${playerShowing}_$dealerShowing"]!!
            }
        }
    }

    // Finally loop through splits
    for (playerShowing in 4..20 step 2) {
        for (dealerShowing in all_denoms) {
            val edo = values["edo_${playerShowing}_$dealerShowing"]!!
            val esp = eqns.eqn_sp["${playerShowing}_$dealerShowing"]!!.evaluate(values)
            if (edo > esp) {
                values["esp_${playerShowing}_$dealerShowing"] = edo
            } else {
                values["esp_${playerShowing}_$dealerShowing"] = esp
            }
        }
    }
    for (dealerShowing in all_denoms) {
        val edo = values["edo_s12_$dealerShowing"]!!
        val esp = eqns.eqn_sp["s12_$dealerShowing"]!!.evaluate(values)
        if (edo > esp) {
            values["esp_s12_$dealerShowing"] = edo
        } else {
            values["esp_s12_$dealerShowing"] = esp
        }
    }

    return values
}

val equations = createVariables()

fun rowsFromShoe(shoe: Shoe) : List<List<String>> {
    // val equations = createVariables()
    val values = evaluateShoe(shoe, equations)

    val rows = mutableListOf<List<String>>()

    fun addRow(playerShowing: Int, playerName: String, split: Boolean) {
        val row = mutableListOf<String>(playerName)
        for (dealerShowing in all_denoms) {
            val decision = if (split) {
                if (values["esp_${playerShowing}_$dealerShowing"]!! > values["edo_${playerShowing}_$dealerShowing"]!!) {
                    " Y "
                } else {
                    " N "
                }
            } else {
                if (values["edo_${playerShowing}_$dealerShowing"]!! > values["ehi_${playerShowing}_$dealerShowing"]!!) {
                    " D "
                } else if (values["ehi_${playerShowing}_$dealerShowing"]!! > values["est_${playerShowing}_$dealerShowing"]!!) {
                    " H "
                } else {
                    " S "
                }
            }
            row.add(decision)
        }
        rows.add(row)
    }

    for (playerShowing in 21 downTo 3) {
        addRow(playerShowing, "$playerShowing", false)
    }
    for (playerShowing in 21 downTo 13) {
        addRow(playerShowing, "s$playerShowing", false)
    }
    for (split in all_denoms) {
        if (split == 'A') {
            addRow(12, "A/A", true)
        } else {
            val playerShowing = 2 * cardValue(Card(split), /*canBeSoft=*/false)
            addRow(playerShowing, "${split}/$split", true)
        }
    }

    return rows
}

val baselineRows = rowsFromShoe(Shoe())

fun main() {
    val dealerHandHeaders = listOf("P") + all_denoms.map { " $it " }
    val rows = rowsFromShoe(Shoe())

    prettyPrintTable(dealerHandHeaders, rows)

    SwingUtilities.invokeLater {
        val frame = JFrame("Table Example")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

        val tableModel = DefaultTableModel()
        dealerHandHeaders.forEach { tableModel.addColumn(it) }

        rows.forEach { row -> tableModel.addRow(row.toTypedArray()) }

        val table = JTable(tableModel)
        frame.add(JScrollPane(table))

        frame.setSize(300, 670)   
        frame.isVisible = true
    }
}
