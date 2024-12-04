import kotlin.collections.toMutableMap
import kotlin.random.Random


class Shoe() {
    val distribution: MutableMap<Card, Int> = all_cards.map { it to 1*if (it.denom == 'T') 16 else 4 }.toMap().toMutableMap()

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

    fun finished() : Boolean {
        return distribution.values.sum() < 10
    }
}

class Hand(val index: Int) {
    val cards = mutableListOf<Card>()
    var doubled = false
    var hardened = false

    fun addCard(card: Card) {
        cards.add(card)
    }

    fun clear() {
        cards.clear()
    }

    fun updateHand(cards: List<Card>) {
        this.cards.clear()
        this.cards.addAll(cards)
        // For splitting aces
        hardened = false
    }

    fun double() {
        assert(index > 0)
        doubled = true
    }

    fun value(): HandValue {
        var totalValue = 0
        var isSoft = false
        for (card in cards) {
            totalValue += cardValue(card, /*canBeSoft=*/!hardened)
            if (card.denom == 'A' && !hardened) {
                isSoft = true
            }
        }
        if (totalValue > 21 && isSoft) {
            totalValue -= 10
            isSoft = false
            hardened = true
        }
        return HandValue(totalValue, isSoft)
    }

    fun nCards() : Int {
        return cards.size
    }

    fun isSplittable() : Boolean {
        return cards.size == 2 && cards[0].denom == cards[1].denom
    }

    fun isBlackjack() : Boolean {
        if (cards.size != 2) { return false }
        return value().value == 21
    }

    fun isBust() : Boolean {
        return value().value > 21
    }
}

class Model() {
    var shoe = Shoe()

    val dealerHand = Hand(0)
    var humanHands = mutableListOf<Hand>()
    var isEndOfHand = false
    var humanHandIndex = 0

    var profit: Float = 0.0f

    fun drawCardUpdateDistribution(player: Player) : Pair<Card, Map<Card, Int>> {
        val card = shoe.drawCard()
        return Pair(card, shoe.distribution)
    }

    fun addCard(player: Player, card: Card) {
        if (player.index == DEALER_INDEX) {
            dealerHand.addCard(card)
        } else {
            if (player.index > humanHands.size) {
                humanHands.add(Hand(player.index))
            }
            humanHands[player.index - 1].addCard(card)
        }
    }

    fun activeButtons() : List<Button> {
        if (isEndOfHand) {
            if (shoe.finished()) {
                return listOf(Button.NewShoe)
            }
            return listOf(Button.Deal, Button.NewShoe)
        }

        val isDoublable = humanHands[humanHandIndex].nCards() == 2
        val isSplittable = humanHands[humanHandIndex].isSplittable()

        return listOfNotNull<Button>(
            Button.Hit,
            Button.Stand,
            if (isDoublable) Button.Double else null,
            if (isSplittable) Button.Split else null
        )
    }

    fun resultsUpdateProfit() : List<Pair<Result, Float>> {
        val results = humanHands.map { hand ->
            when {
                hand.isBlackjack() && dealerHand.isBlackjack() -> Pair(Result.Tie, 0.0f)
                hand.isBlackjack() -> if (humanHands.size > 1)
                    Pair(Result.HumanBlackjackTwoToOne, 1.0f)
                else Pair(Result.HumanBlackjack, 1.5f)
                dealerHand.isBlackjack() -> Pair(Result.DealerBlackjack, -1.0f)
                hand.doubled -> when {
                    hand.value().value > 21 -> Pair(Result.DoubleHumanBust, -2.0f)
                    dealerHand.value().value > 21 -> Pair(Result.DoubleDealerBust, 2.0f)
                    hand.value().value > dealerHand.value().value -> Pair(Result.DoubleWin, 2.0f)
                    hand.value().value < dealerHand.value().value -> Pair(Result.DoubleLoss, -2.0f)
                    else -> Pair(Result.Tie, 0.0f)
                }
                hand.value().value > 21 -> Pair(Result.HumanBust, -1.0f)
                dealerHand.value().value > 21 -> Pair(Result.DealerBust, 1.0f)
                hand.value().value > dealerHand.value().value -> Pair(Result.Human, 1.0f)
                hand.value().value < dealerHand.value().value -> Pair(Result.Dealer, -1.0f)
                else -> Pair(Result.Tie, 0.0f)
            }
        }
        for (result in results) {
            profit += result.second
        }
        return results
    }

    fun updateEndOfHand() {
        isEndOfHand = true
    }

    fun allHandsBlackjackOrBust() : Boolean {
        return humanHands.all { it.isBlackjack() || it.value().value > 21 }
    }
    
    fun dealerShouldDraw() : Boolean {
        if (dealerHand.value().value < 17) {
            return true
        }
        if (dealerHand.value().soft && dealerHand.value().value < 18) {
            return true
        }
        return false
    }

    fun isAnotherHand() : Boolean {
        return humanHandIndex < humanHands.size - 1
    }

    fun advanceToNextHand() {
        humanHandIndex += 1
    }

    fun humanHandIndex() : Player {
        // Outside of this class we use 1-indexed
        return Player(humanHandIndex + 1)
    }

    fun isBust() : Boolean {
        return humanHands[humanHandIndex].isBust()
    }

    fun doubleBet() {
        humanHands[humanHandIndex].double()
    }

    fun getSplitCard() : Card {
        return humanHands[humanHandIndex].cards[0]
    }

    fun humanHandsLength() : Int {
        return humanHands.size
    }

    fun updateHand(player: Player, card: Card) {
        assert (player.index > DEALER_INDEX)
        if (player.index > humanHands.size) {
            humanHands.add(Hand(player.index))
        }
        humanHands[player.index - 1].updateHand(listOf(card))
    }

    fun isBlackjack(player: Player) : Boolean {
        if (player.index == DEALER_INDEX) {
            return dealerHand.isBlackjack()
        }
        return humanHands[player.index - 1].isBlackjack()
    }

    fun updateStartOfHand() {
        dealerHand.clear()
        humanHands = mutableListOf(Hand(1))
        isEndOfHand = false
        humanHandIndex = 0
    }

    fun writeScores(): String {
        return "Dealer: ${dealerHand.value().value}\n" + humanHands.joinToString("\n") { "Player ${it.index}: ${it.value().value}" } + "\n"
    }

    fun newShoe() {
        shoe = Shoe()
    }
}
