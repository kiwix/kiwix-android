/*
 * Copyright (c) 2019 Kiwix
 * All rights reserved.
 */

package org.kiwix.kiwixmobile.extensions

import android.view.View
import android.widget.TextView

fun TextView.setTextAndVisibility(nullableText: String?) =
  if (nullableText != null && nullableText.isNotEmpty()) {
    text = nullableText
    visibility = View.VISIBLE
  } else {
    visibility = View.GONE
  }
