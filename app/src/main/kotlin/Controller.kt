class Controller(val model: Model, val view: View) {

    private fun drawCardUpdates(player: Player) : Card {
        val (card, distribution) = model.drawCardUpdateDistribution(player)
        view.updateHand(player, card)
        model.updateHand(player, card)
        view.updateDistribution(distribution)
        return card
    }

    private fun endOfHand(result: Result) {
        view.updateEndOfHand(result)
        model.updateEndOfHand()
        val profit = model.updateProfit(result)
        view.updateProfit(profit)
    }

    fun hit() {
        val card = drawCardUpdates(Player.Human)
        view.updateStatus(model.showing(Player.Dealer), model.showing(Player.Human), PlayerBoth.Human)
        if (model.isBust(Player.Human)) {
            val card = drawCardUpdates(Player.Dealer)
            endOfHand(Result.HumanBust)
        }
        view.doubleButton.disable()
        view.draw()
    }

    fun stand() {
        while (model.dealerShouldDraw()) {
            val card = drawCardUpdates(Player.Dealer)
        }
        view.updateStatus(model.showing(Player.Dealer), model.showing(Player.Human), PlayerBoth.Both)
        endOfHand(model.result())
        view.draw()
    }

    fun deal() {
        view.updateStartOfHand()
        model.updateStartOfHand()
        var card = drawCardUpdates(Player.Dealer)
        card = drawCardUpdates(Player.Human)
        card = drawCardUpdates(Player.Human)
        view.updateStatus(model.showing(Player.Dealer), model.showing(Player.Human))
        if (model.isBlackjack(Player.Human)) {
            // Have to check if it's a draw
            drawCardUpdates(Player.Dealer)
            if (model.isBlackjack(Player.Dealer)) {
                endOfHand(Result.Tie)
            } else {
                endOfHand(Result.HumanBlackjack)
            }
        }
        view.draw()
    }

    fun double() {
        val card = drawCardUpdates(Player.Human)
        if (model.isBust(Player.Human)) {
            val card = drawCardUpdates(Player.Dealer)
            endOfHand(Result.DoubleLoss)
        } else {
            while (model.dealerShouldDraw()) {
                val card = drawCardUpdates(Player.Dealer)
            }
            view.updateStatus(model.showing(Player.Dealer), model.showing(Player.Human), PlayerBoth.Both)
            val result = model.result()
            endOfHand(when (result) {
                Result.HumanBust -> Result.DoubleLoss
                Result.DealerBust -> Result.DoubleWin
                Result.Tie -> Result.DoubleTie
                Result.Human -> Result.DoubleWin
                Result.Dealer -> Result.DoubleLoss
                Result.HumanBlackjack -> Result.DoubleWin
                Result.DealerBlackjack -> Result.DoubleLoss
                else -> throw IllegalArgumentException("Unexpected result: $result")
            })
        }
        view.draw()
    }

    fun dispatch(button: Button) {
        when (button) {
            Button.Hit -> hit()
            Button.Stand -> stand()
            Button.Deal -> deal()
            Button.Double -> double()
        }
    }
}
