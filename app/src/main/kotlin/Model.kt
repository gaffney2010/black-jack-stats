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

    var profit: Float = 0.0f

    fun updateHand(player: Player, card: Card) {
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

    fun drawCardUpdateDistribution(player: Player) : Pair<Card, Map<Card, Int>> {
        val card = shoe.drawCard()
        return Pair(card, shoe.distribution)
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

    fun updateProfit(result: Result): Float {
        val delta = when (result) {
            Result.Human -> 1.0f
            Result.Dealer -> -1.0f
            Result.Tie -> 0.0f
            Result.HumanBust -> -1.0f
            Result.DealerBust -> 1.0f
        }
        profit += delta
        return profit
    }
}
