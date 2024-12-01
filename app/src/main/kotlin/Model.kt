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

    var dealerShowing = HandValue(0, false, 0)
    var humanShowing = HandValue(0, false, 0)
    var isEndOfHand = false

    var profit: Float = 0.0f

    fun updateHand(player: Player, card: Card) {
        if (player == Player.Dealer) {
            if (card == Card('A')) {
                dealerShowing = HandValue(dealerShowing.value + 11, true, dealerShowing.nCards + 1)
            } else {
                dealerShowing.value += cardValue(card)
                dealerShowing.nCards += 1
            }
            if (dealerShowing.value > 21 && dealerShowing.soft) {
                dealerShowing = HandValue(dealerShowing.value - 10, false, dealerShowing.nCards)
            }
        } else {
            if (card == Card('A')) {
                humanShowing = HandValue(humanShowing.value + 11, true, humanShowing.nCards + 1)
            } else {
                humanShowing.value += cardValue(card)
                humanShowing.nCards += 1
            }
            if (humanShowing.value > 21 && humanShowing.soft) {
                humanShowing = HandValue(humanShowing.value - 10, false, humanShowing.nCards)
            }
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

    fun showing(player: Player) : HandValue {
        return when (player) {
            Player.Dealer -> dealerShowing
            Player.Human -> humanShowing
        }
    }

    fun isBust(player: Player) : Boolean {
        return when (player) {
            Player.Dealer -> dealerShowing.value > 21
            Player.Human -> humanShowing.value > 21
        }
    }
    
    fun dealerShouldDraw() : Boolean {
        if (dealerShowing.value < 17) {
            return true
        }
        if (dealerShowing.soft && dealerShowing.value < 18) {
            return true
        }
        return false
    }

    fun isBlackjack(player: Player) : Boolean {
        return when (player) {
            Player.Dealer -> dealerShowing.value == 21 && dealerShowing.nCards == 2
            Player.Human -> humanShowing.value == 21 && humanShowing.nCards == 2
        }
    }

    fun result() : Result {
        return when {
            isBlackjack(Player.Dealer) -> Result.DealerBlackjack
            humanShowing.value > 21 -> Result.HumanBust
            dealerShowing.value > 21 -> Result.DealerBust
            humanShowing.value > dealerShowing.value -> Result.Human
            humanShowing.value < dealerShowing.value -> Result.Dealer
            else -> Result.Tie
        }
    }

    fun updateEndOfHand() {
        isEndOfHand = true
    }

    fun updateStartOfHand() {
        dealerShowing = HandValue(0, false, 0)
        humanShowing = HandValue(0, false, 0)
        isEndOfHand = false
    }

    fun updateProfit(result: Result): Float {
        val delta = when (result) {
            Result.Human -> 1.0f
            Result.Dealer -> -1.0f
            Result.Tie -> 0.0f
            Result.HumanBust -> -1.0f
            Result.DealerBust -> 1.0f
            Result.HumanBlackjack -> 1.5f
            Result.DealerBlackjack -> -1.0f
            Result.DoubleWin -> 2.0f
            Result.DoubleLoss -> -2.0f 
            Result.DoubleTie -> 0.0f
        }
        profit += delta
        return profit
    }
}
