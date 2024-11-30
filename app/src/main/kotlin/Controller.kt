class Controller(val model: Model, val view: View) {
    fun hit() {
        val card = model.drawCardUpdateHand(Player.Human)
        view.updateHand(Player.Human, card)
        view.updateStatus(model.showing(Player.Dealer), model.showing(Player.Human), PlayerBoth.Human)
        if (model.isBust(Player.Human)) {
            view.updateEndOfHand(Result.HumanBust)
            val card = model.drawCardUpdateHand(Player.Dealer)
            view.updateHand(Player.Dealer, card)
            model.updateEndOfHand()
        }
        view.draw()
    }

    fun stand() {
        while (model.dealerShouldDraw()) {
            val card = model.drawCardUpdateHand(Player.Dealer)
            view.updateHand(Player.Dealer, card)
        }
        view.updateStatus(model.showing(Player.Dealer), model.showing(Player.Human), PlayerBoth.Both)
        model.updateEndOfHand()
        view.updateEndOfHand(model.result())
        view.draw()
    }

    fun deal() {
        view.updateStartOfHand()
        model.updateStartOfHand()
        var card = model.drawCardUpdateHand(Player.Dealer)
        view.updateHand(Player.Dealer, card)
        card = model.drawCardUpdateHand(Player.Human)
        view.updateHand(Player.Human, card)
        card = model.drawCardUpdateHand(Player.Human)
        view.updateHand(Player.Human, card)
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
