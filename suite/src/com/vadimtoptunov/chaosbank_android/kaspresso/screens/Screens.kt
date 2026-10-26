package com.vadimtoptunov.chaosbank_android.kaspresso.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.KNode

/**
 * Base screen object. Every node is matched by its ChaosBank `testTag` in the
 * unmerged tree, so a tag inside a clickable row (a market price, a holding's P&L)
 * is still reachable on its own. The tag strings are the iOS accessibility ids.
 */
abstract class CbScreen<T : CbScreen<T>>(provider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<T>(semanticsProvider = provider) {

    protected fun tag(id: String): KNode = child {
        hasTestTag(id)
        useUnmergedTree = true
    }
}

class TabBar(p: SemanticsNodeInteractionsProvider) : CbScreen<TabBar>(p) {
    val home = tag("tabBar.home")
    val markets = tag("tabBar.markets")
    val portfolio = tag("tabBar.portfolio")
    val card = tag("tabBar.card")
}

class HomeScreen(p: SemanticsNodeInteractionsProvider) : CbScreen<HomeScreen>(p) {
    val totalBalance = tag("home.totalBalance")
    val todayChange = tag("home.todayChange")
    val transferAction = tag("home.quickAction.transfer")
    val exchangeAction = tag("home.quickAction.exchange")
    val addMoneyAction = tag("home.quickAction.addMoney")
    val seeAllActivity = tag("home.seeAllActivity")
    val buildBadge = tag("build.badge")

    fun account(code: String) = tag("home.account.$code")
    fun currency(code: String) = tag("home.currencySegment.$code")
}

class TransferScreen(p: SemanticsNodeInteractionsProvider) : CbScreen<TransferScreen>(p) {
    val recipientField = tag("transfer.recipientField")
    val amountField = tag("transfer.amountField")
    val balanceAfter = tag("transfer.balanceAfter")
    val continueButton = tag("transfer.continueButton")
    val confirmButton = tag("transfer.confirmButton")
    val successToast = tag("transfer.successToast")
}

class ExchangeScreen(p: SemanticsNodeInteractionsProvider) : CbScreen<ExchangeScreen>(p) {
    val amountField = tag("exchange.amountField")
    val rate = tag("exchange.rate")
    val fee = tag("exchange.fee")
    val youGet = tag("exchange.youGet")
    val executeButton = tag("exchange.executeButton")
}

class TransactionsScreen(p: SemanticsNodeInteractionsProvider) : CbScreen<TransactionsScreen>(p) {
    val list = tag("transactions.list")
    val count = tag("transactions.count")
    val searchField = tag("transactions.searchField")
    val loadMore = tag("transactions.loadMore")
    val filterIn = tag("transactions.filter.in")
}

class MarketsScreen(p: SemanticsNodeInteractionsProvider) : CbScreen<MarketsScreen>(p) {
    val stocksSegment = tag("markets.segment.stocks")
    val cryptoSegment = tag("markets.segment.crypto")

    fun assetRow(symbol: String) = tag("markets.asset.$symbol")
    fun price(symbol: String) = tag("markets.asset.$symbol.price")
    fun change(symbol: String) = tag("markets.asset.$symbol.change")
}

class AssetScreen(p: SemanticsNodeInteractionsProvider) : CbScreen<AssetScreen>(p) {
    val price = tag("asset.price")
    val change = tag("asset.change")
    val buyButton = tag("asset.buyButton")
}

class OrderScreen(p: SemanticsNodeInteractionsProvider) : CbScreen<OrderScreen>(p) {
    val refPrice = tag("order.refPrice")
    val estTotal = tag("order.estTotal")
    val qtyValue = tag("order.qtyStepper.value")
    val qtyIncrement = tag("order.qtyStepper.increment")
    val qtyDecrement = tag("order.qtyStepper.decrement")
    val typeLimit = tag("order.type.limit")
    val limitPriceField = tag("order.limitPriceField")
    val reviewButton = tag("order.reviewButton")
    val placeButton = tag("order.placeButton")
}

class PortfolioScreen(p: SemanticsNodeInteractionsProvider) : CbScreen<PortfolioScreen>(p) {
    val totalValue = tag("portfolio.totalValue")
    val pnl = tag("portfolio.pnl")
    val list = tag("portfolio.list")

    fun holding(symbol: String) = tag("portfolio.holding.$symbol")
    fun holdingValue(symbol: String) = tag("portfolio.holding.$symbol.value")
    fun holdingPnl(symbol: String) = tag("portfolio.holding.$symbol.pnl")
}

class CardScreen(p: SemanticsNodeInteractionsProvider) : CbScreen<CardScreen>(p) {
    val number = tag("card.number")
    val cvv = tag("card.cvv")
    val freezeToggle = tag("card.freezeToggle")
    val frozenBadge = tag("card.frozenBadge")
    val pinButton = tag("card.pinButton")
    val virtualButton = tag("card.virtualButton")
    val virtualNumber = tag("card.virtualNumber")
    val loanButton = tag("card.loanButton")
}

class LoansScreen(p: SemanticsNodeInteractionsProvider) : CbScreen<LoansScreen>(p) {
    val apr = tag("loans.apr")
    val monthly = tag("loans.monthly")
    val total = tag("loans.total")
}

class DevMenuScreen(p: SemanticsNodeInteractionsProvider) : CbScreen<DevMenuScreen>(p) {
    val closeButton = tag("dev.close")
    val localeSample = tag("dev.localeSample")
    val localeCurrencySample = tag("dev.localeCurrencySample")
    val tokenStorage = tag("dev.tokenStorage")
    val syncButton = tag("dev.sync")

    /** A network-condition option by kind: "normal", "offline", "slow", "flaky". */
    fun networkCondition(kind: String) = tag("dev.networkCondition.$kind")

    /** A locale option by its raw value: "enUS", "deDE", "ar". */
    fun locale(id: String) = tag("dev.locale.$id")
}

class SyncScreen(p: SemanticsNodeInteractionsProvider) : CbScreen<SyncScreen>(p) {
    val counter = tag("sync.counter")
    val expected = tag("sync.expected")
    val runButton = tag("sync.runButton")
}

class AuthScreen(p: SemanticsNodeInteractionsProvider) : CbScreen<AuthScreen>(p) {
    val gate = tag("auth.gate")
    val loginButton = tag("auth.webLoginButton")
    val webSheet = tag("auth.webSheet")
    val otpField = tag("auth.otpField")
    val otpError = tag("auth.otpError")
    val passcodeField = tag("auth.passcodeField")
}
