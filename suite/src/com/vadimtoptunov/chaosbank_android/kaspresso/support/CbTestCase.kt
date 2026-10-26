package com.vadimtoptunov.chaosbank_android.kaspresso.support

import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.kaspersky.components.composesupport.config.withComposeSupport
import com.kaspersky.kaspresso.kaspresso.Kaspresso
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.vadimtoptunov.chaosbank_android.MainActivity
import org.junit.After
import org.junit.Rule

/**
 * Base for every ChaosBank Kaspresso test: launch helpers over ChaosBank's intent
 * extras (see its README, "Launch arguments") and the defect-injection hook.
 *
 * Re-run the whole suite against a defect build without editing a test by passing
 * instrumentation arguments, the Android twin of the iOS suite's
 * `TEST_RUNNER_CB_INJECT_*`:
 *
 *     ./gradlew :app:connectedStandardDebugAndroidTest \
 *       -Pandroid.testInstrumentationRunnerArguments.cbDefects=pnlSign
 */
abstract class CbTestCase : TestCase(kaspressoBuilder = Kaspresso.Builder.withComposeSupport()) {

    @get:Rule
    val compose = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    /** Boots straight to the unlocked app (`CHAOSBANK_START_UNLOCKED=1`), optionally on a tab. */
    protected fun launchUnlocked(tab: String? = null, extra: Map<String, String> = emptyMap()) =
        launch(mapOf("CHAOSBANK_START_UNLOCKED" to "1") + (tab?.let { mapOf("CHAOSBANK_TAB" to it) } ?: emptyMap()) + extra)

    /** A fresh, locked launch: the app opens on the auth ladder. [deepLink] opens a URI with it. */
    protected fun launchFresh(extra: Map<String, String> = emptyMap(), deepLink: String? = null) =
        launch(mapOf("CHAOSBANK_START_UNLOCKED" to "0") + extra, deepLink)

    private fun launch(extras: Map<String, String>, deepLink: String? = null) {
        scenario?.close()
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        deepLink?.let { intent.data = android.net.Uri.parse(it) }
        (extras + injected()).forEach { (key, value) -> intent.putExtra(key, value) }
        scenario = ActivityScenario.launch(intent)
    }

    /** `cbDefects` / `cbProfile` instrumentation arguments → ChaosBank's intent extras. */
    private fun injected(): Map<String, String> {
        val args = InstrumentationRegistry.getArguments()
        return buildMap {
            args.getString("cbDefects")?.takeIf { it.isNotBlank() }?.let { put("CHAOSBANK_DEFECTS", it) }
            args.getString("cbProfile")?.takeIf { it.isNotBlank() }?.let { put("CHAOSBANK_PROFILE", it) }
        }
    }

    @After
    fun closeApp() {
        scenario?.close()
        scenario = null
    }
}
