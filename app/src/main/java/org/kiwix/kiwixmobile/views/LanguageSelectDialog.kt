/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.views

import android.content.Context
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.language_selection.language_check_view
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.zim_manager.Language

/**
 * Created by judebrauer on 12/6/17
 */

class LanguageSelectDialog constructor(
  context: Context
) : AlertDialog(context) {

  class Builder : AlertDialog.Builder, LayoutContainer {
    private lateinit var dialogView: View
    override val containerView: View? by lazy { dialogView }
    lateinit var onOkClicked: (List<Language>) -> Unit
    var languages: List<Language> = listOf()

    constructor(context: Context) : super(context)

    constructor(
      context: Context,
      themeResId: Int
    ) : super(context, themeResId)

    override fun create(): AlertDialog {
      dialogView = View.inflate(context, R.layout.language_selection, null)
      val languageArrayAdapter = LanguageAdapter(languages.toMutableList())
      language_check_view.run {
        adapter = languageArrayAdapter
        layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        setHasFixedSize(true)
      }
      setView(dialogView)
      setPositiveButton(android.R.string.ok) { _, _ ->
        onOkClicked.invoke(languageArrayAdapter.listItems)
      }
      return super.create()
    }
  }
}
