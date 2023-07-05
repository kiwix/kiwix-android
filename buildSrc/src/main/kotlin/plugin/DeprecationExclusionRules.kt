package plugin

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtImportDirective

class DeprecationExclusionRule : Rule() {
  override val issue = Issue(
    id = "DeprecationExclusion",
    severity = Severity.CodeSmell,
    description = "Deprecated imports should be excluded",
    debt = Debt.FIVE_MINS
  )

  override fun visitImportDirective(importDirective: KtImportDirective) {
    val importText = importDirective.text
    if (importText.contains("@Deprecated")) {
      report(
        CodeSmell(
          issue,
          Entity.from(importDirective),
          "Deprecated import found: $importText"
        )
      )
    }
  }
}
