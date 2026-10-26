package com.vadimtoptunov.chaosbank_android.kaspresso.support

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput

/**
 * Every signed number in a ChaosBank label, in order, as en-US formatted:
 * "+€69.52 · +0.40% today" → [69.52, 0.40]; "−$282.72" → [-282.72].
 * The app formats negatives with U+2212 MINUS SIGN, not an ASCII hyphen.
 */
fun numbers(label: String): List<Double> =
    Regex("([+\\-−]?)[€$£]?([0-9][0-9,]*(?:\\.[0-9]+)?)").findAll(label).mapNotNull { m ->
        val value = m.groupValues[2].replace(",", "").toDoubleOrNull() ?: return@mapNotNull null
        if (m.groupValues[1] == "-" || m.groupValues[1] == "−") -value else value
    }.toList()

/** All the text under the node tagged [tag] (its own and its descendants'), joined with spaces. */
fun SemanticsNodeInteractionsProvider.textOf(tag: String): String =
    collectText(onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()).joinToString(" ")

private fun collectText(node: SemanticsNode): List<String> {
    val own = (node.config.getOrNull(SemanticsProperties.EditableText)?.let { listOf(it.text) }
        ?: node.config.getOrNull(SemanticsProperties.Text)?.map { it.text }.orEmpty())
    return own + node.children.flatMap(::collectText)
}

/** The [index]-th signed number in the text of the node tagged [tag]. */
fun SemanticsNodeInteractionsProvider.number(tag: String, index: Int = 0): Double {
    val text = textOf(tag)
    return numbers(text).getOrNull(index) ?: error("no number #$index in '$text' ($tag)")
}

/** The last signed number in the text of the node tagged [tag]. */
fun SemanticsNodeInteractionsProvider.lastNumber(tag: String): Double {
    val text = textOf(tag)
    return numbers(text).lastOrNull() ?: error("no number in '$text' ($tag)")
}

/** Waits until a node tagged [tag] exists. */
fun ComposeTestRule.waitForTag(tag: String, timeoutMillis: Long = 10_000) =
    waitUntil(timeoutMillis) { onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

/** Waits until no node tagged [tag] exists. */
fun ComposeTestRule.waitForGone(tag: String, timeoutMillis: Long = 20_000) =
    waitUntil(timeoutMillis) { onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }

/**
 * The texts under every node whose testTag starts with [prefix] (e.g. "transactions.row."),
 * rows ordered top to bottom.
 */
fun SemanticsNodeInteractionsProvider.textsUnderRows(prefix: String): List<String> =
    onAllNodes(
        SemanticsMatcher("testTag starts with $prefix") {
            it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
        },
        useUnmergedTree = true,
    ).fetchSemanticsNodes()
        .sortedBy { it.boundsInRoot.top }
        .flatMap(::collectText)

/** The texts of the given tags, all read from one semantics snapshot, so values that tick
 *  (prices, P&L) come from the same frame. */
fun SemanticsNodeInteractionsProvider.snapshotTexts(tags: Set<String>): Map<String, String> =
    onAllNodes(
        SemanticsMatcher("testTag in $tags") { it.config.getOrNull(SemanticsProperties.TestTag) in tags },
        useUnmergedTree = true,
    ).fetchSemanticsNodes()
        .associate { it.config[SemanticsProperties.TestTag] to collectText(it).joinToString(" ") }

/** Waits until the node tagged [tag] exists and is enabled (data-driven buttons start disabled). */
fun ComposeTestRule.waitEnabled(tag: String, timeoutMillis: Long = 10_000) =
    waitUntil(timeoutMillis) {
        onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes()
            .any { !it.config.contains(SemanticsProperties.Disabled) }
    }

/** Waits until at least one row tagged with [prefix] is rendered. */
fun ComposeTestRule.waitForRows(prefix: String, timeoutMillis: Long = 10_000) =
    waitUntil(timeoutMillis) { textsUnderRows(prefix).isNotEmpty() }

/** Gives async data up to [timeoutMillis] to satisfy [condition], then returns; the caller
 *  asserts afterwards, so a defect still fails with a readable message, not a timeout. */
fun ComposeTestRule.eventually(timeoutMillis: Long = 10_000, condition: () -> Boolean) {
    runCatching { waitUntil(timeoutMillis) { runCatching(condition).getOrDefault(false) } }
}

/** Two taps in one injected gesture. Two `performClick()` calls each wait for idle in
 * between, so the second lands after the first request has settled and never races it. */
fun ComposeTestRule.rapidDoubleTap(tag: String) {
    onNodeWithTag(tag, useUnmergedTree = true).performTouchInput { click(); advanceEventTime(80); click() }
}
