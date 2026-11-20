/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.kiwix.sharedFunctions

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider

private val HASH_CODE_SUFFIX = Regex("-?\\d{6,}$")
private val NUMERIC_SUFFIX = Regex("^(.*\\D)(\\d+)$")

/**
 * Fails when the semantics tree has a problem that the Accessibility Test Framework does not
 * report:
 *
 * - A content description ends in a `hashCode()`, for example "Stop-867512127".
 * - A content description is the text of the node plus a number, for example "Title1".
 * - Two or more content descriptions differ only by a number after the same label, for
 *   example "Favicon0" and "Favicon1".
 * - A node with a click action is hidden from accessibility services.
 *
 * TalkBack reads the content description, so a list index or a `hashCode()` must go in a
 * test tag.
 */
fun SemanticsNodeInteractionsProvider.assertAccessibilitySemantics() {
  val nodes = onAllNodes(SemanticsMatcher("any node") { true }, useUnmergedTree = true)
    .fetchSemanticsNodes(atLeastOneRootRequired = false)
  val problems = accessibilitySemanticsProblems(nodes)
  if (problems.isNotEmpty()) {
    throw AssertionError(
      "Accessibility semantics problems:\n" + problems.joinToString("\n") { "- $it" }
    )
  }
}

fun accessibilitySemanticsProblems(nodes: List<SemanticsNode>): List<String> =
  nodes.flatMap(::nodeProblems) + indexSuffixProblems(nodes)

private fun nodeProblems(node: SemanticsNode): List<String> {
  val problems = mutableListOf<String>()
  val texts = node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
  node.contentDescriptions().forEach { description ->
    when {
      HASH_CODE_SUFFIX.containsMatchIn(description) ->
        problems += "\"$description\" ends in a hashCode()"

      texts.any { it.isNotEmpty() && description != it && isNumberedLabel(description, it) } ->
        problems += "\"$description\" is the text of the node plus a number"
    }
  }
  if (node.config.contains(SemanticsActions.OnClick) && node.isHiddenFromAccessibility()) {
    problems += "A clickable node (${node.describe()}) is hidden from accessibility services"
  }
  return problems
}

private fun indexSuffixProblems(nodes: List<SemanticsNode>): List<String> =
  nodes.flatMap { it.contentDescriptions() }
    .mapNotNull { description ->
      NUMERIC_SUFFIX.matchEntire(description)
        ?.groupValues
        ?.takeIf { it[1].last() != ' ' }
        ?.let { it[1] to description }
    }
    .groupBy({ it.first }, { it.second })
    .filterValues { it.distinct().size > 1 }
    .map { (label, descriptions) ->
      "${descriptions.distinct()} are \"$label\" plus a number, for example a list index"
    }

private fun isNumberedLabel(description: String, label: String): Boolean =
  description.startsWith(label) && description.substring(label.length).all(Char::isDigit)

private fun SemanticsNode.contentDescriptions(): List<String> =
  config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()

private fun SemanticsNode.isHiddenFromAccessibility(): Boolean =
  generateSequence(this) { it.parent }
    .any { it.config.contains(SemanticsProperties.HideFromAccessibility) }

private fun SemanticsNode.describe(): String =
  config.getOrNull(SemanticsProperties.TestTag)
    ?: config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }
    ?: "id $id"
