package com.vadimtoptunov.chaosbank_android.kaspresso.tests

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.CardScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.HomeScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.KycScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.TabBar
import com.vadimtoptunov.chaosbank_android.kaspresso.screens.TransferScreen
import com.vadimtoptunov.chaosbank_android.kaspresso.support.CbTestCase
import com.vadimtoptunov.chaosbank_android.kaspresso.support.textOf
import com.vadimtoptunov.chaosbank_android.kaspresso.support.waitEnabled
import com.vadimtoptunov.chaosbank_android.kaspresso.support.waitForTag
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The mock KYC flow and the large-transfer gate it unlocks. Port of the iOS KycTests.
 * Every test starts unverified (`CHAOSBANK_KYC_UNVERIFIED=1`) and scripts what the
 * "camera" sees, so no hardware and no dev-menu switching is involved.
 */
class KycTests : CbTestCase() {

    /** Opens Card → Verify identity with the given scanner script. */
    private fun openKyc(document: String = "valid", selfie: String = "match") {
        launchUnlocked(
            tab = "card",
            extra = mapOf("CHAOSBANK_KYC_UNVERIFIED" to "1", "CHAOSBANK_KYC_DOCUMENT" to document, "CHAOSBANK_KYC_SELFIE" to selfie),
        )
        compose.onNodeWithTag("card.kycButton", useUnmergedTree = true).performScrollTo()   // below the fold
        onComposeScreen<CardScreen>(compose) { kycButton.performClick() }
    }

    /** Scan → selfie → submit, then wait for the result. */
    private fun runFlow() {
        onComposeScreen<KycScreen>(compose) { scanButton.performClick() }
        compose.waitEnabled("kyc.selfieButton")
        onComposeScreen<KycScreen>(compose) { selfieButton.performClick() }
        compose.waitForTag("kyc.selfieResult")
        compose.waitEnabled("kyc.submitButton")
        onComposeScreen<KycScreen>(compose) { submitButton.performClick() }
        compose.waitForTag("kyc.resultTitle")
    }

    /** The happy path verifies the identity, and the Card tab says so. Catches `kycStuckPending`. */
    @Test
    fun kyc_happyPath_verifiesTheIdentity() = run {
        openKyc()
        runFlow()
        assertEquals("Identity verified", compose.textOf("kyc.resultTitle"))
        onComposeScreen<KycScreen>(compose) { doneButton.performClick() }
        compose.waitForTag("card.kycStatus")
        assertEquals("Identity verified", compose.textOf("card.kycStatus"))
    }

    /** Catches `kycAcceptsExpiredDocument`. */
    @Test
    fun kyc_expiredDocument_isRejected() = run {
        openKyc(document = "expired")
        runFlow()
        assertEquals("Verification failed", compose.textOf("kyc.resultTitle"))
        assertEquals("Document expired", compose.textOf("kyc.resultReason"))
    }

    /** Catches `kycNameMismatchAccepted`. */
    @Test
    fun kyc_documentInAnotherName_isRejected() = run {
        openKyc(document = "nameMismatch")
        runFlow()
        assertEquals("Verification failed", compose.textOf("kyc.resultTitle"))
        assertEquals("Name doesn't match the account holder", compose.textOf("kyc.resultReason"))
    }

    /** Catches `kycSelfieMismatchAccepted`. */
    @Test
    fun kyc_mismatchedSelfie_isRejected() = run {
        openKyc(selfie = "mismatch")
        runFlow()
        assertEquals("Verification failed", compose.textOf("kyc.resultTitle"))
        assertEquals("Face doesn't match the document", compose.textOf("kyc.resultReason"))
    }

    /** An unreadable scan can't go further: no selfie step, no submit. */
    @Test
    fun kyc_unreadableDocument_cantBeSubmitted() = run {
        openKyc(document = "unreadable")
        onComposeScreen<KycScreen>(compose) { scanButton.performClick() }
        compose.waitForTag("kyc.scanError")
        onComposeScreen<KycScreen>(compose) {
            selfieButton.assertDoesNotExist()
            submitButton.assertIsNotEnabled()
        }
    }

    /** While unverified, a transfer over the €1,000 threshold can't continue. Catches `kycBypassAllowsTransfer`. */
    @Test
    fun transfer_overKycThreshold_isBlockedWhileUnverified() = run {
        launchUnlocked(extra = mapOf("CHAOSBANK_KYC_UNVERIFIED" to "1"))
        onComposeScreen<HomeScreen>(compose) { transferAction.performClick() }
        onComposeScreen<TransferScreen>(compose) {
            recipientField.performTextInput("Alex")
            amountField.performTextInput("500")
        }
        // Enabled under the threshold: the balance has loaded, so a disabled button below
        // is the KYC gate and not an empty balance.
        compose.waitEnabled("transfer.continueButton")
        onComposeScreen<TransferScreen>(compose) {
            amountField.performTextClearance()
            amountField.performTextInput("2000")
            continueButton.assertIsNotEnabled()
        }
    }

    /** After verifying, the same €2,000 transfer can continue. Catches `kycStuckPending` end to end. */
    @Test
    fun transfer_overKycThreshold_opensAfterVerifying() = run {
        openKyc()
        runFlow()
        onComposeScreen<KycScreen>(compose) { doneButton.performClick() }
        onComposeScreen<TabBar>(compose) { home.performClick() }
        onComposeScreen<HomeScreen>(compose) { transferAction.performClick() }
        onComposeScreen<TransferScreen>(compose) {
            recipientField.performTextInput("Alex")
            amountField.performTextInput("2000")
        }
        compose.waitEnabled("transfer.continueButton")
    }
}
