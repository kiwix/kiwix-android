package org.kiwix.kiwixmobile.intro

import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.StringId.TextId
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.main.MainRobot
import org.kiwix.kiwixmobile.main.main

fun intro(func: IntroRobot.() -> Unit) = IntroRobot().apply(func)

class IntroRobot : BaseRobot() {

  private val getStarted = ViewId(R.id.get_started)
  private val viewPager = ViewId(R.id.view_pager)

  init {
    isVisible(getStarted)
  }

  fun swipeLeft() {
    isVisible(viewPager).swipeLeft()
    isVisible(TextId(R.string.save_books_offline))
    isVisible(TextId(R.string.download_books_message))
  }

  fun swipeRight() {
    isVisible(viewPager).swipeRight()
    isVisible(TextId(R.string.welcome_to_the_family))
    isVisible(TextId(R.string.human_kind_knowledge))
  }

  infix fun clickGetStarted(func: MainRobot.() -> Unit): MainRobot {
    clickOn(getStarted)
    return main(func)
  }
}
