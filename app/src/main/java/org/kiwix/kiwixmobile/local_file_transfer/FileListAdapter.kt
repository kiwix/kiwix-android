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
@file:Suppress("PackageNaming")

package org.kiwix.kiwixmobile.local_file_transfer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.local_file_transfer.FileItem.FileStatus.Companion.ERROR
import org.kiwix.kiwixmobile.local_file_transfer.FileItem.FileStatus.Companion.SENDING
import org.kiwix.kiwixmobile.local_file_transfer.FileItem.FileStatus.Companion.SENT
import org.kiwix.kiwixmobile.local_file_transfer.FileItem.FileStatus.Companion.TO_BE_SENT
import org.kiwix.kiwixmobile.local_file_transfer.FileListAdapter.FileViewHolder
import java.util.ArrayList

/**
 * Helper class, part of the local file sharing module.
 *
 * Defines the Adapter for the list of file-items displayed in {TransferProgressFragment}
 */
class FileListAdapter(private val fileItems: ArrayList<FileItem>) :
  RecyclerView.Adapter<FileViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
    val itemView = LayoutInflater.from(parent.context)
      .inflate(R.layout.item_transfer_list, parent, false)
    return FileViewHolder(itemView, this)
  }

  override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
    val fileItem = fileItems[position]
    val name = fileItem.fileName
    holder.fileName.text = name
    holder.statusImage.isVisible = fileItem.fileStatus != SENDING
    holder.progressBar.isVisible = fileItem.fileStatus == SENDING
    if (fileItem.fileStatus != TO_BE_SENT) {
      // Icon for TO_BE_SENT is assigned by default in the item layout
      holder.progressBar.visibility = View.GONE
      when (fileItem.fileStatus) {
        SENT -> holder.statusImage.setImageResource(R.drawable.ic_baseline_check_24px)
        ERROR -> holder.statusImage.setImageResource(R.drawable.ic_baseline_error_24px)
      }
    }
  }

  override fun getItemCount(): Int = fileItems.size

  inner class FileViewHolder(itemView: View, val fileListAdapter: FileListAdapter) :
    RecyclerView.ViewHolder(itemView) {
    var fileName: TextView = itemView.findViewById(R.id.text_view_file_item_name)
    var statusImage: ImageView = itemView.findViewById(R.id.image_view_file_transferred)
    var progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar_transferring_file)
  }
}
