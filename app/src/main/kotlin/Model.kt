import kotlin.collections.toMutableMap
import kotlin.random.Random


class Shoe() {
    val distribution: MutableMap<Card, Int> = all_cards.map { it to if (it.denom == 'T') 16 else 4 }.toMap().toMutableMap()

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
                val result = key
                distribution[result] = distribution[result]!! - 1
                return result
            }
        }

        throw IllegalArgumentException("Ran out of cards.")
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
