// Copyright (C) 2023 Salesforce, Inc.
// Copyright 2022 Twitter, Inc.
// SPDX-License-Identifier: Apache-2.0
package slack.lint.compose

import com.android.tools.lint.checks.DataFlowAnalyzer
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import com.android.tools.lint.detector.api.getUMethod
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UReturnExpression
import slack.lint.compose.util.Priorities
import slack.lint.compose.util.emitsContent
import slack.lint.compose.util.isComposable
import slack.lint.compose.util.modifierParameter
import slack.lint.compose.util.sourceImplementation

class ModifierReusedDetector
@JvmOverloads
constructor(
  private val contentEmitterOption: ContentEmitterLintOption =
    ContentEmitterLintOption(CONTENT_EMITTER_OPTION)
) : ComposableFunctionDetector(contentEmitterOption), SourceCodeScanner {

  companion object {

    val CONTENT_EMITTER_OPTION = ContentEmitterLintOption.newOption()

    val ISSUE =
      Issue.create(
          id = "ComposeModifierReused",
          briefDescription = "Modifiers should only be used once",
          explanation =
            """
              Modifiers should only be used once and by the root level layout of a Composable. This is true even if appended to or with other modifiers e.g. `modifier.fillMaxWidth()`.\
              Use Modifier (with a capital 'M') to construct a new Modifier that you can pass to other composables.\
              See https://slackhq.github.io/compose-lints/rules/#dont-re-use-modifiers for more information.
            """,
          category = Category.PRODUCTIVITY,
          priority = Priorities.NORMAL,
          severity = Severity.ERROR,
          implementation = sourceImplementation<ModifierReusedDetector>(),
        )
        .setOptions(listOf(CONTENT_EMITTER_OPTION))
  }

  override fun visitComposable(context: JavaContext, method: UMethod, function: KtFunction) {
    if (!function.emitsContent(contentEmitterOption.value)) return
    val modifier = method.modifierParameter(context.evaluator) ?: return

    var callCount = 0
    val visitor = object : DataFlowAnalyzer(listOf(modifier)) {
      override fun argument(call: UCallExpression, reference: UElement) {
        callCount += 1

        if (callCount > 1) {
          context.report(
            ISSUE,
            call,
            context.getLocation(reference),
            ISSUE.getExplanation(TextFormat.TEXT),
          )
        }
      }
    }

    method.accept(visitor)
  }
}
