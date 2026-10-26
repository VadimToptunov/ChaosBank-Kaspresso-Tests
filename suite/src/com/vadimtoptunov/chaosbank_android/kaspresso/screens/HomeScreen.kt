package com.vadimtoptunov.chaosbank_android.kaspresso.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.KNode

/** Home: total balance, today's change, quick actions, accounts, recent activity. */
class HomeScreen(provider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<HomeScreen>(semanticsProvider = provider) {

    val totalBalance: KNode = child { hasTestTag("home.totalBalance") }
    val todayChange: KNode = child { hasTestTag("home.todayChange") }
    val transferAction: KNode = child { hasTestTag("home.quickAction.transfer") }
    val exchangeAction: KNode = child { hasTestTag("home.quickAction.exchange") }
    val addMoneyAction: KNode = child { hasTestTag("home.quickAction.addMoney") }

    fun account(code: String): KNode = child { hasTestTag("home.account.$code") }
}
