package com.vadimtoptunov.chaosbank_android.kaspresso.tests

import com.vadimtoptunov.chaosbank_android.kaspresso.screens.HomeScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.support.CbTestCase
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import org.junit.Test

class HomeTests : CbTestCase() {

    @Test
    fun home_showsBalanceAndQuickActions() = run {
        step("Launch unlocked") { launchUnlocked() }
        step("Balance and quick actions are shown") {
            onComposeScreen<HomeScreen>(compose) {
                totalBalance.assertIsDisplayed()
                transferAction.assertIsDisplayed()
                exchangeAction.assertIsDisplayed()
                addMoneyAction.assertIsDisplayed()
            }
        }
    }
}
