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

package org.kiwix.kiwixmobile.localFileTransfer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.base.adapter.BaseViewHolder
import org.kiwix.kiwixmobile.databinding.ItemTransferListBinding
import org.kiwix.kiwixmobile.localFileTransfer.FileItem.FileStatus.ERROR
import org.kiwix.kiwixmobile.localFileTransfer.FileItem.FileStatus.SENDING
import org.kiwix.kiwixmobile.localFileTransfer.FileItem.FileStatus.SENT
import org.kiwix.kiwixmobile.localFileTransfer.FileListAdapter.FileViewHolder

/**
 * Helper class, part of the local file sharing module.
 *
 * Defines the Adapter for the list of file-items displayed in {TransferProgressFragment}
 */
class FileListAdapter(private val fileItems: List<FileItem>) :
  RecyclerView.Adapter<FileViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder =
    FileViewHolder(
      ItemTransferListBinding.inflate(
        LayoutInflater.from(parent.context),
        parent,
        false
      ),
      this
    )

  override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
    holder.bind(fileItems[position])
  }

  override fun getItemCount(): Int = fileItems.size

  inner class FileViewHolder(
    private val itemTransferListBinding: ItemTransferListBinding,
    val fileListAdapter: FileListAdapter
  ) :
    BaseViewHolder<FileItem>(itemTransferListBinding.root) {
    override fun bind(item: FileItem) {
      itemTransferListBinding.textViewFileItemName.text = item.fileName
      itemTransferListBinding.imageViewFileTransferred.isVisible = item.fileStatus != SENDING
      itemTransferListBinding.progressBarTransferringFile.isVisible = item.fileStatus == SENDING
      if (item.fileStatus != FileItem.FileStatus.TO_BE_SENT) {
        // Icon for TO_BE_SENT is assigned by default in the item layout
        itemTransferListBinding.progressBarTransferringFile.visibility = View.GONE
        when (item.fileStatus) {
          SENDING -> itemTransferListBinding.progressBarTransferringFile.visibility = View.VISIBLE
          SENT -> {
            itemTransferListBinding.imageViewFileTransferred.setImageResource(
              R.drawable.ic_baseline_check_24px
            )
            itemTransferListBinding.progressBarTransferringFile.visibility = View.GONE
          }
          ERROR -> {
            itemTransferListBinding.imageViewFileTransferred.setImageResource(
              R.drawable.ic_baseline_error_24px
            )
            itemTransferListBinding.progressBarTransferringFile.visibility = View.GONE
          }
          else -> {
          }
        }
      }
    }
  }
}
