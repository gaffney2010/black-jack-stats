class Controller(val model: Model, val view: View) {

    private fun drawCardUpdates(player: Player) : Card {
        val (card, distribution) = model.drawCardUpdateDistribution(player)
        view.updateHand(player, card)
        model.updateHand(player, card)
        view.updateDistribution(distribution)
        return card
    }

    fun hit() {
        val card = drawCardUpdates(Player.Human)
        view.updateStatus(model.showing(Player.Dealer), model.showing(Player.Human), PlayerBoth.Human)
        if (model.isBust(Player.Human)) {
            view.updateEndOfHand(Result.HumanBust)
            val card = drawCardUpdates(Player.Dealer)
            model.updateEndOfHand()
        }
        view.draw()
    }

    fun stand() {
        while (model.dealerShouldDraw()) {
            val card = drawCardUpdates(Player.Dealer)
        }
        view.updateStatus(model.showing(Player.Dealer), model.showing(Player.Human), PlayerBoth.Both)
        model.updateEndOfHand()
        view.updateEndOfHand(model.result())
        view.draw()
    }

    fun deal() {
        view.updateStartOfHand()
        model.updateStartOfHand()
        var card = drawCardUpdates(Player.Dealer)
        card = drawCardUpdates(Player.Human)
        card = drawCardUpdates(Player.Human)
        view.updateStatus(model.showing(Player.Dealer), model.showing(Player.Human))
        view.draw()
    }

    fun dispatch(button: Button) {
        when (button) {
            Button.Hit -> hit()
            Button.Stand -> stand()
            Button.Deal -> deal()
        }
    }
}
