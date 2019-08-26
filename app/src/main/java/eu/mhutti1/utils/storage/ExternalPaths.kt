/*
 * Copyright 2016 Isaac Hutt <mhutti1@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU  General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package eu.mhutti1.utils.storage

import android.annotation.SuppressLint

internal object ExternalPaths {

  @SuppressLint("SdCardPath")
  val possiblePaths = arrayOf(
    "/storage/sdcard0",
    "/storage/sdcard1",
    "/storage/extsdcard",
    "/storage/extSdCard",
    "/storage/sdcard0/external_sdcard",
    "/mnt/sdcard/external_sd",
    "/mnt/external_sd",
    "/mnt/media_rw/*",
    "/removable/microsd",
    "/mnt/emmc",
    "/storage/external_SD",
    "/storage/ext_sd",
    "/storage/removable/sdcard1",
    "/data/sdext",
    "/data/sdext2",
    "/data/sdext3",
    "/data/sdext2",
    "/data/sdext3",
    "/data/sdext4",
    "/sdcard",
    "/sdcard1",
    "/sdcard2",
    "/storage/microsd",
    "/mnt/extsd",
    "/extsd",
    "/mnt/sdcard",
    "/misc/android"
  )
}
