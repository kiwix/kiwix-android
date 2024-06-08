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

package org.kiwix.kiwixmobile.qr

import android.app.AlertDialog
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.databinding.DialogShareByQrCodeBinding
import org.kiwix.kiwixmobile.core.qr.GenerateQR

class ShareByQRCodeDialog : DialogFragment() {

  private val args: ShareByQRCodeDialogArgs by navArgs()

  private lateinit var binding: DialogShareByQrCodeBinding

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    binding = DialogShareByQrCodeBinding.inflate(layoutInflater)

    loadQrCode(args.uri.toUri())

    return AlertDialog.Builder(requireContext(), R.style.ThemeOverlay_Material3_Dialog)
      .setView(binding.root)
      .create()
  }

  private fun loadQrCode(uri: Uri) {
    val qr = GenerateQR().createQR(uri)

    binding.apply {
      qrCode.setImageBitmap(qr)
      qrCodeDescription.text = getString(
        R.string.qr_code_description,
        "$uri"
      )
    }
  }
}
