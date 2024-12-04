data class Card(val denom: Char);
val all_denoms = listOf('A', '2', '3', '4', '5', '6', '7', '8', '9', 'T')
val all_cards = all_denoms.map { Card(it) }

data class HandValue(var value: Int, var soft: Boolean)

fun handValueStr(value: HandValue) : String {
    return when (value.soft) {
        true -> "Soft ${value.value}"
        false -> "${value.value}"
    }
}

val DEALER_INDEX = 0
data class Player(val index: Int)

enum class PlayerBoth {
    Dealer, Human, Both
}

enum class Result {
    Human, Dealer, Tie, HumanBust, DealerBust, HumanBlackjack, HumanBlackjackTwoToOne, DealerBlackjack, DoubleWin, DoubleLoss, DoubleDealerBust, DoubleHumanBust
}

enum class Button {
    Hit, Stand, Deal, Double, Split, NewShoe
}

fun cardValue(card: Card, canBeSoft: Boolean) : Int {
    return when (card.denom) {
        'A' -> if (canBeSoft) 11 else 1
        'T' -> 10
        else -> card.denom.toString().toInt()
    }
}
