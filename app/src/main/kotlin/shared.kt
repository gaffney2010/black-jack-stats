data class Card(val denom: Char);
val all_denoms = listOf('A', '2', '3', '4', '5', '6', '7', '8', '9', 'T')
val all_cards = all_denoms.map { Card(it) }

data class HandValue(var value: Int, var soft: Boolean, var nCards: Int)

fun handValueStr(value: HandValue) : String {
    return when (value.soft) {
        true -> "Soft ${value.value}"
        false -> "${value.value}"
    }
}

enum class Player {
    Dealer, Human
}

enum class PlayerBoth {
    Dealer, Human, Both
}

enum class Result {
    Human, Dealer, Tie, HumanBust, DealerBust, HumanBlackjack, DealerBlackjack
}

enum class Button {
    Hit, Stand, Deal
}

fun cardValue(card: Card) : Int {
    return when (card.denom) {
        'A' -> 1
        'T' -> 10
        else -> card.denom.toString().toInt()
    }
}
