/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
package org.kiwix.kiwixmobile.core.help

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.recyclerview.widget.DividerItemDecoration
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.getCurrentLocale
import kotlinx.android.synthetic.main.activity_help.activity_help_feedback_image_view
import kotlinx.android.synthetic.main.activity_help.activity_help_feedback_text_view
import kotlinx.android.synthetic.main.activity_help.activity_help_recycler_view
import kotlinx.android.synthetic.main.layout_toolbar.toolbar
import org.kiwix.kiwixmobile.core.utils.CONTACT_EMAIL_ADDRESS

class HelpActivity : BaseActivity() {
  private val titleDescriptionMap by lazy {
    listOf(
      R.string.help_2 to R.array.description_help_2,
      R.string.help_5 to R.array.description_help_5
    ).associate { (title, description) ->
      getString(title) to resources.getStringArray(description)
        .joinToString(separator = "\n")
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_help)

    activity_help_feedback_text_view.setOnClickListener { sendFeedback() }
    activity_help_feedback_image_view.setOnClickListener { sendFeedback() }
    setSupportActionBar(toolbar)
    toolbar.setNavigationOnClickListener { onBackPressed() }
    supportActionBar?.let {
      it.setDisplayHomeAsUpEnabled(true)
      it.setTitle(R.string.menu_help)
    }
    activity_help_recycler_view.addItemDecoration(
      DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
    )
    activity_help_recycler_view.adapter = HelpAdapter(titleDescriptionMap)
  }

  private fun sendFeedback() {
    val intent = Intent(Intent.ACTION_SENDTO)
    intent.data = ("mailto:${Uri.encode(CONTACT_EMAIL_ADDRESS)}" +
      "?subject=${Uri.encode("Feedback in ${getCurrentLocale(this).displayLanguage}")}")
      .toUri()
    startActivity(Intent.createChooser(intent, "Send Feedback via Email"))
  }

  override fun injection(coreComponent: CoreComponent) {
    coreComponent.inject(this)
  }
}
