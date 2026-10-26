package com.vadimtoptunov.chaosbank_android.kaspresso.tests

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.AssetScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.CardScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.ExchangeScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.HomeScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.MarketsScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.OrderScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.TabBar
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.TransferScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.support.CbTestCase
import com.vadimtoptunov.chaosbank_android.kaspresso.support.lastNumber
import com.vadimtoptunov.chaosbank_android.kaspresso.support.number
import com.vadimtoptunov.chaosbank_android.kaspresso.support.numbers
import com.vadimtoptunov.chaosbank_android.kaspresso.support.snapshotTexts
import com.vadimtoptunov.chaosbank_android.kaspresso.support.textOf
import com.vadimtoptunov.chaosbank_android.kaspresso.support.waitForGone
import com.vadimtoptunov.chaosbank_android.kaspresso.support.waitForTag
import com.vadimtoptunov.chaosbank_android.kaspresso.support.eventually
import com.vadimtoptunov.chaosbank_android.kaspresso.support.waitEnabled
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Value checks: each test compares a number with what it should be, so it catches the
 * money defects that presence checks let through. Port of the iOS suite's *ValueTests.
 */
class HomeValueTests : CbTestCase() {

    /** Total = the three accounts in EUR at 1 EUR = 1.08 USD = 0.85 GBP, to the cent.
     *  Catches `homeTotalOmitsAccount`, `balanceFloorRounded`. */
    @Test
    fun home_total_equalsSumOfAccounts() = run {
        launchUnlocked()
        compose.waitForTag("home.account.GBP")
        val eur = compose.lastNumber("home.account.EUR")
        val usd = compose.lastNumber("home.account.USD")
        val gbp = compose.lastNumber("home.account.GBP")
        val expected = ((eur + usd / 1.08 + gbp / 0.85) * 100).roundToLong() / 100.0
        assertEquals(expected, compose.number("home.totalBalance"), 0.005)
    }

    /** The seeded day is up 0.40%. Catches `todayChangeSignFlipped`. */
    @Test
    fun home_todayChange_isAGainOfTheTotal() = run {
        launchUnlocked()
        compose.waitForTag("home.account.GBP")
        compose.eventually { numbers(compose.textOf("home.todayChange"))[0] != 0.0 }
        val total = compose.number("home.totalBalance")
        val values = numbers(compose.textOf("home.todayChange"))
        assertTrue("today's change should be a gain: $values", values[0] > 0)
        assertEquals(0.40, values[1], 0.001)
        assertEquals(total * values[1] / 100, values[0], 0.01)
    }
}

class TransferValueTests : CbTestCase() {

    /** "Balance after" = EUR balance − amount. Catches `balanceAfterAdds`. */
    @Test
    fun transfer_balanceAfter_subtractsTheAmount() = run {
        launchUnlocked()
        compose.waitForTag("home.account.EUR")
        val eur = compose.lastNumber("home.account.EUR")
        onComposeScreen<HomeScreen>(compose) { transferAction.performClick() }
        onComposeScreen<TransferScreen>(compose) {
            recipientField.performTextInput("Alex")
            amountField.performTextInput("100")
        }
        compose.eventually { kotlin.math.abs(compose.number("transfer.balanceAfter") - (eur - 100)) < 0.005 }
        assertEquals(eur - 100, compose.number("transfer.balanceAfter"), 0.005)
    }

    /** €100.40 leaves EUR by exactly that and USD untouched.
     *  Catches `transferRoundsUp`, `transferDebitsWrongAccount`. */
    @Test
    fun transfer_debitsExactlyTheAmountFromEUR() = run {
        launchUnlocked()
        compose.waitForTag("home.account.EUR")
        val eur = compose.lastNumber("home.account.EUR")
        val usd = compose.lastNumber("home.account.USD")
        onComposeScreen<HomeScreen>(compose) { transferAction.performClick() }
        onComposeScreen<TransferScreen>(compose) {
            recipientField.performTextInput("Alex")
            amountField.performTextInput("100.40")
        }
        compose.waitEnabled("transfer.continueButton")
        onComposeScreen<TransferScreen>(compose) { continueButton.performClick() }
        compose.waitEnabled("transfer.confirmButton")
        onComposeScreen<TransferScreen>(compose) { confirmButton.performClick() }
        compose.waitForGone("transfer.confirmButton")
        compose.waitForTag("home.account.EUR")
        compose.waitUntil(10_000) { runCatching { compose.lastNumber("home.account.EUR") != eur }.getOrDefault(false) }
        assertEquals("EUR should drop by exactly the amount", eur - 100.40, compose.lastNumber("home.account.EUR"), 0.005)
        assertEquals("USD should be untouched", usd, compose.lastNumber("home.account.USD"), 0.005)
    }
}

class ExchangeValueTests : CbTestCase() {

    /** Selling €25 for USD: EUR −25, USD + "you get". Catches `exchangeCreditsWrongAccount`. */
    @Test
    fun exchange_movesMoneyBetweenTheRightAccounts() = run {
        launchUnlocked()
        compose.waitForTag("home.account.EUR")
        val eur = compose.lastNumber("home.account.EUR")
        val usd = compose.lastNumber("home.account.USD")
        onComposeScreen<HomeScreen>(compose) { exchangeAction.performClick() }
        onComposeScreen<ExchangeScreen>(compose) { amountField.performTextInput("25") }
        compose.waitEnabled("exchange.executeButton")
        val youGet = compose.number("exchange.youGet")
        onComposeScreen<ExchangeScreen>(compose) { executeButton.performClick() }
        compose.waitForGone("exchange.executeButton")
        compose.waitUntil(10_000) { runCatching { compose.lastNumber("home.account.EUR") != eur }.getOrDefault(false) }
        assertEquals(eur - 25, compose.lastNumber("home.account.EUR"), 0.005)
        assertEquals(usd + youGet, compose.lastNumber("home.account.USD"), 0.005)
    }
}

class LoansValueTests : CbTestCase() {

    /** The payment is the amortised payment on €5,000 over 24 months at the advertised APR.
     *  Catches `loanAprUnderstated`. */
    @Test
    fun loans_monthlyPayment_matchesTheAdvertisedApr() = run {
        launchUnlocked(tab = "card")
        compose.onNodeWithTag("card.loanButton", useUnmergedTree = true).performScrollTo()
        onComposeScreen<CardScreen>(compose) { loanButton.performClick() }
        compose.waitForTag("loans.apr")
        val apr = compose.number("loans.apr")
        val rate = apr / 100 / 12
        val expected = 5000 * rate / (1 - (1 + rate).pow(-24))
        val monthly = compose.number("loans.monthly")
        assertEquals("payment should match $apr% APR", expected, monthly, 0.01)
        assertEquals(monthly * 24, compose.number("loans.total"), 0.25)
    }
}

class MarketsValueTests : CbTestCase() {

    /** Day open implied by price and % change, both read from one snapshot. */
    private fun impliedOpen(priceTag: String, changeTag: String): Double {
        val texts = compose.snapshotTexts(setOf(priceTag, changeTag))
        val price = numbers(texts.getValue(priceTag)).first()
        val change = numbers(texts.getValue(changeTag)).first()
        return price / (1 + change / 100)
    }

    /** Detail and row agree on the day open, which doesn't tick.
     *  Catches `changePctSignFlipped`, `detailPriceOffset`. */
    @Test
    fun assetDetail_agreesWithTheMarketRow() = run {
        launchUnlocked(tab = "markets")
        compose.waitForTag("markets.asset.AAPL.change")
        val rowOpen = impliedOpen("markets.asset.AAPL.price", "markets.asset.AAPL.change")
        onComposeScreen<MarketsScreen>(compose) { assetRow("AAPL").performClick() }
        compose.waitForTag("asset.change")
        val detailOpen = impliedOpen("asset.price", "asset.change")
        assertEquals("detail and row should agree on the day open", rowOpen, detailOpen, rowOpen * 0.0003)
    }
}

class OrderValueTests : CbTestCase() {

    private fun openBuyTicket() {
        launchUnlocked(tab = "markets")
        onComposeScreen<MarketsScreen>(compose) { assetRow("AAPL").performClick() }
        compose.onNodeWithTag("asset.buyButton", useUnmergedTree = true).performScrollTo()
        onComposeScreen<AssetScreen>(compose) { buyButton.performClick() }
        compose.waitForTag("order.estTotal")
    }

    /** Market order: est. total = quantity × reference price. Catches `estTotalIgnoresQty`. */
    @Test
    fun order_estimatedTotal_isQuantityTimesPrice() = run {
        openBuyTicket()
        onComposeScreen<OrderScreen>(compose) {
            qtyIncrement.performClick()
            qtyIncrement.performClick()
        }
        compose.waitForIdle()
        val qty = compose.number("order.qtyStepper.value")
        assertEquals("precondition: three shares", 3.0, qty, 0.0)
        val ref = compose.number("order.refPrice")
        assertEquals(qty * ref, compose.number("order.estTotal"), qty * ref * 0.005)
    }

    /** Limit order: est. total uses the limit price. Catches `limitExecutesAtMarket`. */
    @Test
    fun order_limitEstimatedTotal_usesTheLimitPrice() = run {
        openBuyTicket()
        onComposeScreen<OrderScreen>(compose) { typeLimit.performClick() }
        compose.waitForTag("order.limitPriceField")
        val limit = Math.round(compose.number("order.refPrice") - 10).toDouble()
        compose.onNodeWithTag("order.limitPriceField", useUnmergedTree = true).performTextClearance()
        onComposeScreen<OrderScreen>(compose) { limitPriceField.performTextInput(String.format(java.util.Locale.US, "%.2f", limit)) }
        compose.waitForIdle()
        assertEquals("one share at the limit price", limit, compose.number("order.estTotal"), 0.005)
    }
}

class PortfolioValueTests : CbTestCase() {

    /** TSLA (8 @ $280) is held at a loss; U+2212, not "-". Catches `pnlSign`. */
    @Test
    fun portfolio_tslaLoss_isRenderedAsLoss() = run {
        launchUnlocked(tab = "portfolio")
        compose.waitForTag("portfolio.holding.TSLA.pnl")
        val text = compose.textOf("portfolio.holding.TSLA.pnl")
        assertTrue("TSLA P&L should read as a loss: '$text'", text.contains("−"))
    }

    /** Total = sum of holdings, one snapshot. Catches `totalValueOmitsHolding`. */
    @Test
    fun portfolio_total_equalsSumOfHoldings() = run {
        launchUnlocked(tab = "portfolio")
        compose.waitForTag("portfolio.holding.ETH.value")
        val tags = listOf("AAPL", "TSLA", "ETH").map { "portfolio.holding.$it.value" }
        val texts = compose.snapshotTexts(tags.toSet() + "portfolio.totalValue")
        val sum = tags.sumOf { numbers(texts.getValue(it)).first() }
        assertEquals("total should be the sum of the holdings", sum, numbers(texts.getValue("portfolio.totalValue")).first(), 0.02)
    }

    /** AAPL value ÷ 12 shares ≈ the AAPL price on Markets. Catches `holdingValueUsesCost`. */
    @Test
    fun portfolio_holdingValue_usesTheMarketPrice() = run {
        launchUnlocked(tab = "portfolio")
        compose.waitForTag("portfolio.holding.AAPL.value")
        val perShare = compose.number("portfolio.holding.AAPL.value") / 12
        onComposeScreen<TabBar>(compose) { markets.performClick() }
        compose.waitForTag("markets.asset.AAPL.price")
        val price = compose.number("markets.asset.AAPL.price")
        assertEquals(price, perShare, price * 0.03)
    }

    /** P&L % against the $12,440 cost basis, from one label. Catches `pnlPercentVsValue`. */
    @Test
    fun portfolio_pnlPercent_isAgainstTheCostBasis() = run {
        launchUnlocked(tab = "portfolio")
        compose.waitForTag("portfolio.pnl")
        // The label renders "+$0.00 · +0.00%" until the holdings load.
        compose.eventually { numbers(compose.textOf("portfolio.pnl")).first() != 0.0 }
        val values = numbers(compose.textOf("portfolio.pnl"))
        assertEquals(values[0] / 12_440 * 100, values[1], 0.01)
        assertEquals("the % should carry the P&L's sign", Math.signum(values[0]), Math.signum(values[1]), 0.0)
    }
}
