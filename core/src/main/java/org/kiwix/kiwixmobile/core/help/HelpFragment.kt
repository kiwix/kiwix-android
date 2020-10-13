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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.DividerItemDecoration
import kotlinx.android.synthetic.main.fragment_help.activity_help_feedback_image_view
import kotlinx.android.synthetic.main.fragment_help.activity_help_feedback_text_view
import kotlinx.android.synthetic.main.fragment_help.activity_help_recycler_view
import kotlinx.android.synthetic.main.fragment_help.diagnostic_clickable_area
import kotlinx.android.synthetic.main.layout_toolbar.toolbar
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.error.DiagnosticReportActivity
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.start
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.CONTACT_EMAIL_ADDRESS
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.getCurrentLocale

abstract class HelpFragment : BaseFragment() {

  protected open fun rawTitleDescriptionMap() = listOf(
    R.string.help_2 to R.array.description_help_2,
    R.string.help_5 to R.array.description_help_5
  )

  private val titleDescriptionMap by lazy {
    rawTitleDescriptionMap().associate { (title, description) ->
      getString(title) to resources.getStringArray(description)
        .joinToString(separator = "\n")
    }
  }

  override fun inject(baseActivity: BaseActivity) {
    (baseActivity as CoreMainActivity).cachedComponent.inject(this)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    val activity = requireActivity() as AppCompatActivity
    activity_help_feedback_text_view.setOnClickListener { sendFeedback() }
    activity_help_feedback_image_view.setOnClickListener { sendFeedback() }
    diagnostic_clickable_area.setOnClickListener { sendDiagnosticReport() }
    activity.setSupportActionBar(toolbar)
    toolbar.setNavigationOnClickListener { requireActivity().onBackPressed() }
    activity.supportActionBar?.let {
      it.setDisplayHomeAsUpEnabled(true)
      it.setTitle(R.string.menu_help)
    }
    activity_help_recycler_view.addItemDecoration(
      DividerItemDecoration(activity, DividerItemDecoration.VERTICAL)
    )
    activity_help_recycler_view.adapter = HelpAdapter(titleDescriptionMap)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View = inflater.inflate(R.layout.fragment_help, container, false)

  private fun sendDiagnosticReport() {
    requireActivity().start<DiagnosticReportActivity>()
  }

  private fun sendFeedback() {
    val intent = Intent(Intent.ACTION_SENDTO)
    intent.data = ("mailto:${Uri.encode(CONTACT_EMAIL_ADDRESS)}" +
      "?subject=${Uri.encode(
        "Feedback in ${getCurrentLocale(requireActivity()).displayLanguage}"
      )}")
      .toUri()
    startActivity(Intent.createChooser(intent, "Send Feedback via Email"))
  }
}
