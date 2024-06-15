/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.qr

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.databinding.DialogQrCodeBinding
import javax.inject.Inject

class QRCodeDialog : DialogFragment() {

  private lateinit var binding: DialogQrCodeBinding

  @Inject
  lateinit var generateQR: GenerateQR

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    CoreApp.coreComponent
      .activityComponentBuilder()
      .activity(requireActivity())
      .build()
      .inject(this)
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    binding = DialogQrCodeBinding.inflate(layoutInflater)

    val uri = checkNotNull(arguments?.getString(ARG_URI)) { "URI must be provided as an argument" }

    loadQrCode(uri)

    return AlertDialog.Builder(requireContext(), R.style.ThemeOverlay_Material3_Dialog)
      .setView(binding.root)
      .create()
  }

  /**
   * Load the QR code for the given [uri].
   */
  private fun loadQrCode(uri: String) {
    val qr = generateQR.createQR(uri)

    binding.apply {
      qrCode.setImageBitmap(qr)
      qrCodeDescription.text = getString(
        R.string.qr_code_description,
        uri
      )
    }
  }

  companion object {
    /**
     * Argument key for the URI to display as a QR code.
     */
    const val ARG_URI: String = "uri"
  }
}
