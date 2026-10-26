package com.vadimtoptunov.chaosbank_android.kaspresso.tests

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.DriverAtoms.webKeys
import androidx.test.espresso.web.webdriver.Locator
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.AssetScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.AuthScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.CardScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.ExchangeScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.HomeScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.LoansScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.MarketsScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.OrderScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.PortfolioScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.TabBar
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.TransactionsScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.TransferScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.support.CbTestCase
import com.vadimtoptunov.chaosbank_android.kaspresso.support.rapidDoubleTap
import com.vadimtoptunov.chaosbank_android.kaspresso.support.number
import com.vadimtoptunov.chaosbank_android.kaspresso.support.numbers
import com.vadimtoptunov.chaosbank_android.kaspresso.support.textOf
import com.vadimtoptunov.chaosbank_android.kaspresso.support.textsUnderRows
import com.vadimtoptunov.chaosbank_android.kaspresso.support.waitForGone
import com.vadimtoptunov.chaosbank_android.kaspresso.support.waitForTag
import com.vadimtoptunov.chaosbank_android.kaspresso.support.eventually
import com.vadimtoptunov.chaosbank_android.kaspresso.support.waitForRows
import com.vadimtoptunov.chaosbank_android.kaspresso.support.waitEnabled
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The auth ladder: unlock affordance, the locked gate, and the full web → OTP → passcode ladder. */
class AuthTests : CbTestCase() {

    @Test
    fun startUnlocked_reachesHome() = run {
        launchUnlocked()
        onComposeScreen<HomeScreen>(compose) { totalBalance.assertIsDisplayed() }
    }

    @Test
    fun freshLaunch_showsLoginGate() = run {
        launchFresh()
        onComposeScreen<AuthScreen>(compose) { loginButton.assertIsDisplayed() }
    }

    @Test
    fun fullAuthLadder_reachesHome() = run {
        launchFresh()
        onComposeScreen<AuthScreen>(compose) { loginButton.performClick() }
        compose.waitForTag("auth.webSheet")
        submitWebLogin()
        compose.waitForTag("auth.otpField")
        onComposeScreen<AuthScreen>(compose) { otpField.performTextInput("424242") }       // auto-submits
        compose.waitForTag("auth.passcodeField")
        onComposeScreen<AuthScreen>(compose) { passcodeField.performTextInput("135790") }  // auto-submits
        compose.waitForTag("home.totalBalance", 15_000)
    }
}

/** Fills and submits the HTML form inside the web-login WebView (Espresso Web, "Mode 1"). */
fun submitWebLogin() {
    val web = onWebView().forceJavascriptEnabled()
    web.withElement(findElement(Locator.ID, "web-username")).perform(webKeys("tester"))
    web.withElement(findElement(Locator.ID, "web-password")).perform(webKeys("hunter2"))
    web.withElement(findElement(Locator.ID, "web-submit")).perform(webClick())
}

class HomeTests : CbTestCase() {

    @Test
    fun home_showsBalanceAndQuickActions() = run {
        launchUnlocked()
        onComposeScreen<HomeScreen>(compose) {
            totalBalance.assertIsDisplayed()
            transferAction.assertIsDisplayed()
            exchangeAction.assertIsDisplayed()
            addMoneyAction.assertIsDisplayed()
        }
    }

    @Test
    fun home_showsPerCurrencyAccounts() = run {
        launchUnlocked()
        compose.waitForTag("home.account.GBP")
        onComposeScreen<HomeScreen>(compose) {
            account("EUR").assertExists()
            account("USD").assertExists()
            account("GBP").assertExists()
        }
    }
}

class MarketsTests : CbTestCase() {

    @Test
    fun markets_listsWatchlistAssets() = run {
        launchUnlocked(tab = "markets")
        onComposeScreen<MarketsScreen>(compose) {
            price("AAPL").assertIsDisplayed()
            price("NVDA").assertIsDisplayed()
            price("BTC").assertIsDisplayed()
        }
    }

    @Test
    fun openAsset_pushesDetail() = run {
        launchUnlocked(tab = "markets")
        onComposeScreen<MarketsScreen>(compose) { assetRow("AAPL").performClick() }
        onComposeScreen<AssetScreen>(compose) {
            price.assertIsDisplayed()
            buyButton.assertExists()
        }
    }

    @Test
    fun eachSegment_rendersItsAssets() = run {
        for ((segment, symbol) in listOf("watchlist" to "AAPL", "stocks" to "MSFT", "crypto" to "ETH")) {
            launchUnlocked(tab = "markets")
            onComposeScreen<MarketsScreen>(compose) {
                when (segment) {
                    "stocks" -> stocksSegment.performClick()
                    "crypto" -> cryptoSegment.performClick()
                }
                price(symbol).assertIsDisplayed()
            }
        }
    }
}

class TransferTests : CbTestCase() {

    /** The success toast lives ~1.4 s; `successToastMissing` never shows it. */
    @Test
    fun transfer_happyPath_completes() = run {
        launchUnlocked()
        onComposeScreen<HomeScreen>(compose) { transferAction.performClick() }
        onComposeScreen<TransferScreen>(compose) {
            recipientField.performTextInput("Alex")
            amountField.performTextInput("100")
        }
        compose.waitEnabled("transfer.continueButton")
        onComposeScreen<TransferScreen>(compose) { continueButton.performClick() }
        compose.waitEnabled("transfer.confirmButton")
        onComposeScreen<TransferScreen>(compose) { confirmButton.performClick() }
        compose.waitForTag("transfer.successToast", 3_000)
        compose.waitForGone("transfer.confirmButton")
    }

    @Test
    fun transfer_zeroAmount_disablesContinue() = run {
        launchUnlocked()
        onComposeScreen<HomeScreen>(compose) { transferAction.performClick() }
        onComposeScreen<TransferScreen>(compose) {
            recipientField.performTextInput("Alex")
            amountField.performTextInput("0")
            continueButton.assertIsNotEnabled()
        }
    }
}

class ExchangeTests : CbTestCase() {

    @Test
    fun exchange_happyPath_showsSuccess() = run {
        launchUnlocked()
        onComposeScreen<HomeScreen>(compose) { exchangeAction.performClick() }
        onComposeScreen<ExchangeScreen>(compose) { amountField.performTextInput("100") }
        compose.waitEnabled("exchange.executeButton")
        onComposeScreen<ExchangeScreen>(compose) { executeButton.performClick() }
        compose.waitForGone("exchange.executeButton")
    }

    @Test
    fun exchange_showsQuoteBreakdown() = run {
        launchUnlocked()
        onComposeScreen<HomeScreen>(compose) { exchangeAction.performClick() }
        onComposeScreen<ExchangeScreen>(compose) {
            rate.assertExists()
            fee.assertExists()
            youGet.assertExists()
        }
    }

    /** The exchange's history row credits exactly "you get". Catches `roundingDrift`. */
    @Test
    fun exchange_creditedAmount_matchesDisplayedYouGet() = run {
        launchUnlocked()
        onComposeScreen<HomeScreen>(compose) { exchangeAction.performClick() }
        onComposeScreen<ExchangeScreen>(compose) { amountField.performTextInput("25") }
        compose.waitEnabled("exchange.executeButton")
        val youGet = compose.number("exchange.youGet")
        onComposeScreen<ExchangeScreen>(compose) { executeButton.performClick() }
        compose.waitForGone("exchange.executeButton")
        onComposeScreen<HomeScreen>(compose) { seeAllActivity.performClick() }
        compose.waitForRows("transactions.row.")
        // The exchange's own row (titled "Exchange EUR → USD") credits exactly "you get".
        val rows = compose.textsUnderRows("transactions.row.")
        val title = rows.indexOfFirst { it.startsWith("Exchange") }
        assertTrue("no exchange row in $rows", title >= 0)
        val credited = rows.drop(title).first { it.startsWith("+") || it.startsWith("\u2212") }
        assertEquals("exchange row: $credited", youGet, numbers(credited).first(), 0.005)
    }

    /** A rapid double-tap exchanges once. Catches `exchangeDoubleSubmit`. */
    @Test
    fun exchange_rapidDoubleTapExecute_exchangesOnlyOnce() = run {
        launchUnlocked()
        onComposeScreen<HomeScreen>(compose) { seeAllActivity.performClick() }
        compose.waitForRows("transactions.row.")
        val countBefore = compose.number("transactions.count")
        androidx.test.espresso.Espresso.pressBack()
        onComposeScreen<HomeScreen>(compose) { exchangeAction.performClick() }
        onComposeScreen<ExchangeScreen>(compose) { amountField.performTextInput("100") }
        compose.waitEnabled("exchange.executeButton")
        compose.rapidDoubleTap("exchange.executeButton")
        compose.waitForGone("exchange.executeButton", 30_000)
        onComposeScreen<HomeScreen>(compose) { seeAllActivity.performClick() }
        compose.waitUntil(15_000) { runCatching { compose.number("transactions.count") > countBefore }.getOrDefault(false) }
        assertEquals(countBefore + 1, compose.number("transactions.count"), 0.001)
    }
}

class PortfolioTests : CbTestCase() {

    @Test
    fun portfolio_showsTotalAndPnl() = run {
        launchUnlocked(tab = "portfolio")
        compose.waitForTag("portfolio.list")
        onComposeScreen<PortfolioScreen>(compose) {
            totalValue.assertIsDisplayed()
            pnl.assertExists()
            list.assertExists()
        }
    }
}

class CardTests : CbTestCase() {

    @Test
    fun card_freeze_showsFrozenBadge() = run {
        launchUnlocked(tab = "card")
        onComposeScreen<CardScreen>(compose) {
            frozenBadge.assertDoesNotExist()
            freezeToggle.performClick()
        }
        compose.waitForTag("card.frozenBadge")
    }

    @Test
    fun card_cvv_hiddenByDefault() = run {
        launchUnlocked(tab = "card")
        onComposeScreen<CardScreen>(compose) {
            number.assertExists()
            cvv.assertDoesNotExist()
        }
    }
}

class TransactionsTests : CbTestCase() {

    @Test
    fun transactions_openFromHome_showsList() = run {
        launchUnlocked()
        onComposeScreen<HomeScreen>(compose) { seeAllActivity.performClick() }
        compose.waitForRows("transactions.row.")
        onComposeScreen<TransactionsScreen>(compose) {
            list.assertExists()
            count.assertExists()
            searchField.assertExists()
        }
    }

    /** Loading more twice adds no duplicate rows. Catches `paginationDup`. */
    @Test
    fun transactions_loadMore_noDuplicateRows() = run {
        launchUnlocked()
        onComposeScreen<HomeScreen>(compose) { seeAllActivity.performClick() }
        compose.waitForRows("transactions.row.")
        repeat(2) {
            compose.waitForTag("transactions.loadMore")
            compose.onNodeWithTag("transactions.loadMore", useUnmergedTree = true).performScrollTo()
            onComposeScreen<TransactionsScreen>(compose) { loadMore.performClick() }
            compose.waitForIdle()
        }
        val amounts = transactionAmountTexts()
        assertTrue("no rows", amounts.isNotEmpty())
        assertEquals("duplicate rows: $amounts", amounts.size, amounts.toSet().size)
    }

    /** The "Money in" filter shows only money-in rows. Catches `filterLeaksCategory`. */
    @Test
    fun transactions_moneyInFilter_showsOnlyMoneyIn() = run {
        launchUnlocked()
        onComposeScreen<HomeScreen>(compose) { seeAllActivity.performClick() }
        compose.waitForRows("transactions.row.")
        onComposeScreen<TransactionsScreen>(compose) { filterIn.performClick() }
        compose.waitForIdle()
        val amounts = transactionAmountTexts()
        assertTrue("no rows", amounts.isNotEmpty())
        assertTrue("expected only money-in rows: $amounts", amounts.all { it.startsWith("+") })
    }
}

class OrderTests : CbTestCase() {

    private fun openBuyTicket() {
        launchUnlocked(tab = "markets")
        onComposeScreen<MarketsScreen>(compose) { assetRow("AAPL").performClick() }
        compose.onNodeWithTag("asset.buyButton", useUnmergedTree = true).performScrollTo()
        onComposeScreen<AssetScreen>(compose) { buyButton.performClick() }
        compose.waitForTag("order.reviewButton")
    }

    /** Catches `wrongA11yLabel` (Buy labelled "Sell"). */
    @Test
    fun asset_buyButton_labelledBuy() = run {
        launchUnlocked(tab = "markets")
        onComposeScreen<MarketsScreen>(compose) { assetRow("AAPL").performClick() }
        compose.waitForTag("asset.buyButton")
        assertEquals("Buy", compose.textOf("asset.buyButton"))
        // The defect leaves the visible text alone and changes what TalkBack reads.
        compose.onNodeWithTag("asset.buyButton", useUnmergedTree = true).assertContentDescriptionEquals("Buy")
    }

    /** Catches `limitValidation` (zero quantity still reviewable). */
    @Test
    fun order_zeroQuantity_disablesReview() = run {
        openBuyTicket()
        onComposeScreen<OrderScreen>(compose) {
            qtyDecrement.performClick()
            qtyValue.assertTextContains("0")
            reviewButton.assertIsNotEnabled()
        }
    }

    /** A filled market order closes the ticket. Catches `orderStuckPending`. */
    @Test
    fun order_place_reportsFilled() = run {
        openBuyTicket()
        compose.waitEnabled("order.reviewButton")
        onComposeScreen<OrderScreen>(compose) { reviewButton.performClick() }
        compose.waitForTag("order.placeButton")
        onComposeScreen<OrderScreen>(compose) { placeButton.performClick() }
        compose.waitForGone("order.reviewButton")
    }
}

class LoansTests : CbTestCase() {

    @Test
    fun loans_showsAprMonthlyTotal() = run {
        launchUnlocked(tab = "card")
        compose.onNodeWithTag("card.loanButton", useUnmergedTree = true).performScrollTo()
        onComposeScreen<CardScreen>(compose) { loanButton.performClick() }
        onComposeScreen<LoansScreen>(compose) {
            apr.assertExists()
            monthly.assertExists()
            total.assertExists()
        }
    }
}

// --- shared steps --------------------------------------------------------------

/** The signed amount text of every rendered transaction row, top to bottom. */
fun CbTestCase.transactionAmountTexts(): List<String> =
    compose.textsUnderRows("transactions.row.").filter { it.startsWith("+") || it.startsWith("−") }

/** The signed amounts of every rendered transaction row, top to bottom. */
fun CbTestCase.transactionAmounts(): List<Double> = transactionAmountTexts().map { numbers(it).first() }
