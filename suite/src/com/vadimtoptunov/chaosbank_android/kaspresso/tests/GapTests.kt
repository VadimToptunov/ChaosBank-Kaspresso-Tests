package com.vadimtoptunov.chaosbank_android.kaspresso.tests

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.AssetScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.AuthScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.CardScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.DevMenuScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.HomeScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.MarketsScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.OrderScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.SyncScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.TabBar
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.TransactionsScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.TransferScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.support.CbTestCase
import com.vadimtoptunov.chaosbank_android.kaspresso.support.rapidDoubleTap
import com.vadimtoptunov.chaosbank_android.kaspresso.support.lastNumber
import com.vadimtoptunov.chaosbank_android.kaspresso.support.number
import com.vadimtoptunov.chaosbank_android.kaspresso.support.textOf
import com.vadimtoptunov.chaosbank_android.kaspresso.support.textsUnderRows
import com.vadimtoptunov.chaosbank_android.kaspresso.support.waitForGone
import com.vadimtoptunov.chaosbank_android.kaspresso.support.waitForTag
import com.vadimtoptunov.chaosbank_android.kaspresso.support.eventually
import com.vadimtoptunov.chaosbank_android.kaspresso.support.waitForRows
import com.vadimtoptunov.chaosbank_android.kaspresso.support.waitEnabled
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Number, currency and text formatting. Port of the iOS suite's LocalizationTests. */
class LocalizationTests : CbTestCase() {

    /** "1,000.50" is a thousand and fifty cents. Catches `localeParse`. */
    @Test
    fun transfer_amount_parsesThousandsSeparators() = run {
        launchUnlocked()
        compose.waitForTag("home.account.EUR")
        val eur = compose.lastNumber("home.account.EUR")
        onComposeScreen<HomeScreen>(compose) { transferAction.performClick() }
        onComposeScreen<TransferScreen>(compose) {
            recipientField.performTextInput("Alex")
            amountField.performTextInput("1,000.50")
        }
        compose.eventually { kotlin.math.abs(compose.number("transfer.balanceAfter") - (eur - 1000.50)) < 0.005 }
        assertEquals(eur - 1000.50, compose.number("transfer.balanceAfter"), 0.005)
    }

    /** The USD total shows "$". Catches `balanceWrongCurrencySymbol`. */
    @Test
    fun home_total_usesTheSelectedCurrencySymbol() = run {
        launchUnlocked()
        onComposeScreen<HomeScreen>(compose) { currency("USD").performClick() }
        compose.waitForIdle()
        val text = compose.textOf("home.totalBalance")
        assertTrue("USD total should show \$: '$text'", text.contains("$"))
    }

    /** Prices keep their cents. Catches `priceMissingDecimals`. */
    @Test
    fun markets_prices_keepTheirCents() = run {
        launchUnlocked(tab = "markets")
        compose.waitForTag("markets.asset.AAPL.price")
        val text = compose.textOf("markets.asset.AAPL.price")
        assertTrue("price should end in cents: '$text'", Regex("\\.\\d{2}$").containsMatchIn(text.trim()))
    }

    /** Search ignores the spaces around the query. Catches `searchTrimsNothing`. */
    @Test
    fun transactions_search_ignoresSurroundingSpaces() = run {
        launchUnlocked()
        onComposeScreen<HomeScreen>(compose) { seeAllActivity.performClick() }
        compose.waitForRows("transactions.row.")
        onComposeScreen<TransactionsScreen>(compose) { searchField.performTextInput("  Salary  ") }
        compose.waitForIdle()
        val rows = compose.textsUnderRows("transactions.row.")
        assertTrue("a Salary row should be listed: $rows", rows.any { it.contains("Salary") })
    }

    /** de-DE groups as 1.234.567,89. Catches `numberGroupingIgnoresLocale`. */
    @Test
    fun devMenu_germanLocale_groupsNumbersTheGermanWay() = run {
        launchUnlocked(extra = mapOf("CHAOSBANK_SHOW_DEV" to "1"))
        compose.onNodeWithTag("dev.locale.deDE", useUnmergedTree = true).performScrollTo()
        onComposeScreen<DevMenuScreen>(compose) { locale("deDE").performClick() }
        compose.waitForIdle()
        assertTrue(compose.textOf("dev.localeSample").contains("1.234.567,89"))
    }

    /** de-DE puts the symbol last: "1.234,56 €". Catches `currencySymbolPlacementIgnoresLocale`. */
    @Test
    fun devMenu_germanLocale_putsTheCurrencySymbolLast() = run {
        launchUnlocked(extra = mapOf("CHAOSBANK_SHOW_DEV" to "1"))
        compose.onNodeWithTag("dev.locale.deDE", useUnmergedTree = true).performScrollTo()
        onComposeScreen<DevMenuScreen>(compose) { locale("deDE").performClick() }
        compose.waitForIdle()
        val text = compose.textOf("dev.localeCurrencySample").trim()
        assertTrue("symbol should come last: '$text'", text.endsWith("€"))
    }
}

/**
 * Races and double submits. Port of the iOS ConcurrencyTests.
 *
 * The double-tap tests run on the normal network, unlike iOS. [rapidDoubleTap] puts the
 * taps 80 ms apart, so the second always lands in flight. Under "Slow" the screen pops
 * 1.4 s after the first success and cancels its scope while the second request is still
 * in its 3 s delay, so the defect could never show.
 */
class ConcurrencyTests : CbTestCase() {

    /** A double-tap on Confirm debits once. Catches `doubleCharge`. */
    @Test
    fun transfer_rapidDoubleTapConfirm_debitsOnce() = run {
        launchUnlocked()
        compose.waitForTag("home.account.EUR", 20_000)
        val eur = compose.lastNumber("home.account.EUR")
        onComposeScreen<HomeScreen>(compose) { transferAction.performClick() }
        onComposeScreen<TransferScreen>(compose) {
            recipientField.performTextInput("Alex")
            amountField.performTextInput("100")
        }
        compose.waitEnabled("transfer.continueButton")
        onComposeScreen<TransferScreen>(compose) { continueButton.performClick() }
        compose.waitEnabled("transfer.confirmButton")
        compose.rapidDoubleTap("transfer.confirmButton")
        compose.waitForGone("transfer.confirmButton", 30_000)
        compose.waitUntil(15_000) { runCatching { compose.lastNumber("home.account.EUR") < eur }.getOrDefault(false) }
        Thread.sleep(4_000)   // let a second, racing debit land if there is one
        assertEquals("one transfer, one debit", eur - 100, compose.lastNumber("home.account.EUR"), 0.005)
    }

    /** A double-tap on Place buys one share. Catches `orderDoubleSubmit`. */
    @Test
    fun order_rapidDoubleTapPlace_buysOnce() = run {
        launchUnlocked()
        onComposeScreen<TabBar>(compose) { markets.performClick() }
        onComposeScreen<MarketsScreen>(compose) { assetRow("AAPL").performClick() }
        compose.onNodeWithTag("asset.buyButton", useUnmergedTree = true).performScrollTo()
        onComposeScreen<AssetScreen>(compose) { buyButton.performClick() }
        compose.waitEnabled("order.reviewButton")
        onComposeScreen<OrderScreen>(compose) { reviewButton.performClick() }
        compose.waitForTag("order.placeButton")
        compose.rapidDoubleTap("order.placeButton")
        compose.waitForGone("order.reviewButton", 30_000)
        Thread.sleep(4_000)   // let a second, racing order land if there is one
        androidx.test.espresso.Espresso.pressBack()   // the asset screen stays over the tabs
        onComposeScreen<TabBar>(compose) { portfolio.performClick() }
        compose.waitForTag("portfolio.holding.AAPL", 30_000)
        compose.eventually(15_000) { compose.textOf("portfolio.holding.AAPL").contains(" @ ") }
        val row = compose.textOf("portfolio.holding.AAPL")
        assertTrue("12 seeded + one bought: '$row'", row.contains("13 @"))
    }

    /** The ticket keeps the captured price; it doesn't follow the feed. Catches `livePriceRace`. */
    @Test
    fun order_referencePrice_staysAtTheCapturedPrice() = run {
        launchUnlocked(tab = "markets")
        onComposeScreen<MarketsScreen>(compose) { assetRow("AAPL").performClick() }
        compose.onNodeWithTag("asset.buyButton", useUnmergedTree = true).performScrollTo()
        onComposeScreen<AssetScreen>(compose) { buyButton.performClick() }
        compose.waitForTag("order.refPrice")
        val first = compose.number("order.refPrice")
        Thread.sleep(3_000)   // several ticks
        assertEquals("reference price should not tick", first, compose.number("order.refPrice"), 0.0)
    }

    /** The Sync playground's concurrent +1 batch loses nothing. Catches `syncLostUpdate`. */
    @Test
    fun syncPlayground_concurrentIncrements_loseNothing() = run {
        launchUnlocked(extra = mapOf("CHAOSBANK_SHOW_DEV" to "1"))
        onComposeScreen<DevMenuScreen>(compose) { syncButton.performClick() }
        compose.waitEnabled("sync.runButton")
        val expected = compose.lastNumber("sync.expected")   // read before the run recomputes it
        onComposeScreen<SyncScreen>(compose) { runButton.performClick() }
        compose.eventually(15_000) { compose.lastNumber("sync.counter") >= expected }
        Thread.sleep(1_000)
        assertEquals("every +1 should land", expected, compose.lastNumber("sync.counter"), 0.0)
    }
}

/** Card data, the auth ladder and storage. Port of the iOS SecurityTests. */
class SecurityTests : CbTestCase() {

    /** PAN masked except the last four. Catches `cardNumberFullyVisible`. */
    @Test
    fun card_number_isMaskedExceptTheLastFour() = run {
        launchUnlocked(tab = "card")
        compose.waitForTag("card.number")
        val pan = compose.textOf("card.number")
        assertTrue("PAN should be masked: '$pan'", Regex("^•••• •••• •••• \\d{4}$").matches(pan))
    }

    /** Show PIN keeps it masked. Catches `pinShownPlaintext`. */
    @Test
    fun card_showPin_keepsThePinMasked() = run {
        launchUnlocked(tab = "card")
        compose.onNodeWithTag("card.pinButton", useUnmergedTree = true).performScrollTo()
        onComposeScreen<CardScreen>(compose) { pinButton.performClick() }
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText("Your PIN is", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasText("Your PIN is ••••")).assertExists()
    }

    /** A virtual card doesn't reuse the real number. Catches `virtualCardShowsRealPan`. */
    @Test
    fun card_virtualCard_doesNotReuseTheRealNumber() = run {
        launchUnlocked(tab = "card")
        compose.waitForTag("card.number")
        val lastFour = compose.textOf("card.number").takeLast(4)
        compose.onNodeWithTag("card.virtualButton", useUnmergedTree = true).performScrollTo()
        onComposeScreen<CardScreen>(compose) { virtualButton.performClick() }
        compose.waitForTag("card.virtualNumber")
        val virtual = compose.textOf("card.virtualNumber").replace(" ", "")
        assertFalse("virtual '$virtual' should not end like the real card ($lastFour)", virtual.endsWith(lastFour))
    }

    private fun reachOtp() {
        launchFresh()
        onComposeScreen<AuthScreen>(compose) { loginButton.performClick() }
        compose.waitForTag("auth.webSheet")
        submitWebLogin()
        compose.waitForTag("auth.otpField")
    }

    /** A wrong OTP is rejected. Catches `otpAcceptsAnyCode`. */
    @Test
    fun otp_wrongCode_isRejected() = run {
        reachOtp()
        onComposeScreen<AuthScreen>(compose) { otpField.performTextInput("000000") }
        compose.waitForTag("auth.otpError")
        onComposeScreen<AuthScreen>(compose) { passcodeField.assertDoesNotExist() }
    }

    /** Three wrong codes lock the step. Catches `otpNoLockout`. */
    @Test
    fun otp_threeWrongCodes_lockTheStep() = run {
        reachOtp()
        repeat(3) {
            onComposeScreen<AuthScreen>(compose) {
                otpField.performTextClearance()
                otpField.performTextInput("000000")
            }
            compose.waitForTag("auth.otpError")
        }
        onComposeScreen<AuthScreen>(compose) {
            otpField.performTextClearance()
            otpField.performTextInput("424242")
        }
        compose.waitForIdle()
        assertTrue(compose.textOf("auth.otpError").contains("locked"))
        onComposeScreen<AuthScreen>(compose) { passcodeField.assertDoesNotExist() }
    }

    /** A deep link opened while locked keeps the gate. Catches `deepLinkSkipsAuth`. */
    @Test
    fun deepLink_whileLocked_keepsTheAuthGate() = run {
        launchFresh(deepLink = "chaosbank://markets")
        compose.waitForIdle()
        onComposeScreen<AuthScreen>(compose) { loginButton.assertIsDisplayed() }
    }

    /** After a real login the token is in the Keystore. Catches `tokenInUserDefaults`. */
    @Test
    fun login_storesTheTokenInTheKeystore() = run {
        reachOtp()
        onComposeScreen<AuthScreen>(compose) { otpField.performTextInput("424242") }
        compose.waitForTag("auth.passcodeField")
        onComposeScreen<AuthScreen>(compose) { passcodeField.performTextInput("135790") }
        compose.waitForTag("build.badge", 15_000)
        compose.onNodeWithTag("build.badge", useUnmergedTree = true)
            .performTouchInput { longClick() }
        compose.waitForTag("dev.tokenStorage")
        val storage = compose.textOf("dev.tokenStorage")
        assertTrue("token should be in the Keystore: '$storage'", storage.contains("Keystore"))
    }
}
