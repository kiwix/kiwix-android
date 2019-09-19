package org.kiwix.kiwixmobile.language

import applyWithViewHierarchyPrinting
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.R

fun language(func: LanguageRobot.() -> Unit) = LanguageRobot().applyWithViewHierarchyPrinting(func)

class LanguageRobot : BaseRobot() {
  init {
    isVisible(ViewId(R.id.language_recycler_view))
  }
}
