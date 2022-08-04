/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.localFileTransfer

import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import applyWithViewHierarchyPrinting
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.DEFAULT_WAIT
import org.kiwix.kiwixmobile.Findable.StringId.TextId
import org.kiwix.kiwixmobile.R

/**
 * Authored by Ayush Shrivastava on 29/10/20
 */

fun localFileTransfer(func: LocalFileTransferRobot.() -> Unit) =
  LocalFileTransferRobot().applyWithViewHierarchyPrinting(func)

class LocalFileTransferRobot : BaseRobot() {

  init {
    waitTillLoad()
    isVisible(TextId(R.string.receive_files_title))
  }

  override fun waitTillLoad() {
    uiDevice.wait(Until.findObjects(By.res(R.string.receive_files_title.toString())), DEFAULT_WAIT)
  }
}
