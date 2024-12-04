class Controller(val model: Model, val view: View) {

    private fun drawCardUpdates(player: Player) : Card {
        val (card, distribution) = model.drawCardUpdateDistribution(player)
        view.addCard(player, card)
        model.addCard(player, card)
        view.updateDistribution(distribution)
        return card
    }

    private fun updateButtons() {
        val activeButtons = model.activeButtons()
        view.updateButtons(activeButtons)
    }

    private fun endOfHand() {
        // Display the outcome of each hand
        var i = 1
        for (resultProfit in model.resultsUpdateProfit()) {
            val (result, profit) = resultProfit
            view.updateResult(result, i)
            view.updateProfit(profit)
            i += 1
        }

        // Broadcast the end of a hand
        view.updateEndOfHand()
        model.updateEndOfHand()

        // Disable buttons
        updateButtons()
    }

    private fun dealerPlays() {
        if (model.allHandsBlackjackOrBust()) {
            drawCardUpdates(Player(DEALER_INDEX))
        } else {
            while (model.dealerShouldDraw()) {
                drawCardUpdates(Player(DEALER_INDEX))
            }
        }

        view.writeText(model.writeScores())

        endOfHand()
    }

    fun advanceToNextHandOrDealer() {
        if (model.isAnotherHand()) {
            model.advanceToNextHand()
            view.advanceToNextHand()
            updateButtons()
        } else {
            dealerPlays()
        }
    }

    fun hit() {
        // Draw a card
        val card = drawCardUpdates(model.humanHandIndex())

        // Write the new human showing total
        view.writeText(model.writeScores())

        // If bust, we move on to the next hand
        if (model.isBust()) {
            advanceToNextHandOrDealer()
        }

        // Disable buttons
        updateButtons()

        view.draw()
    }

    fun stand() {
        // Move to next hand
        advanceToNextHandOrDealer()

        view.draw()
    }

    fun double() {
        // Draw a card
        val card = drawCardUpdates(model.humanHandIndex())

        // Write the new human showing total
        view.writeText(model.writeScores())

        // Record that we bet two on this hand
        model.doubleBet()

        // Move to the next hand
        advanceToNextHandOrDealer()

        view.draw()
    }

    fun split() {
        val card = model.getSplitCard()

        // Overwrite the existing hand
        view.updateHand(model.humanHandIndex(), card)
        model.updateHand(model.humanHandIndex(), card)
        drawCardUpdates(model.humanHandIndex())

        // Create a new hand
        view.updateHand(Player(model.humanHandsLength() + 1), card)  // Should create hands
        model.updateHand(Player(model.humanHandsLength() + 1), card)  // Increases length
        drawCardUpdates(Player(model.humanHandsLength()))

        // Check the current hand for a black jack
        if (model.isBlackjack(model.humanHandIndex())) {
            advanceToNextHandOrDealer()
        }

        // Activate the right buttons for the new hand
        updateButtons()

        view.draw()
    }

    fun deal() {
        // Broadcast the start of a new hand
        view.updateStartOfHand()
        model.updateStartOfHand()

        // Deal the cards
        var card = drawCardUpdates(Player(DEALER_INDEX))
        card = drawCardUpdates(Player(1))
        card = drawCardUpdates(Player(1))

        // Check for a blackjack
        if (model.isBlackjack(model.humanHandIndex())) {
            dealerPlays()
        }

        // Write the hand totals
        view.writeText(model.writeScores())

        // Enable buttons
        updateButtons()

        view.draw()
    }

    fun newShoe() {
        model.newShoe()
        deal()
    }

    fun dispatch(button: Button) {
        when (button) {
            Button.Hit -> hit()
            Button.Stand -> stand()
            Button.Deal -> deal()
            Button.Double -> double()
            Button.Split -> split()
            Button.NewShoe -> newShoe()
        }
    }
}
