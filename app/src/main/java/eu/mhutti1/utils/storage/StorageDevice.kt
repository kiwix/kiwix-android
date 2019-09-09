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

import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

const val LOCATION_EXTENSION = "storageLocationMarker"

data class StorageDevice(val file: File, val isInternal: Boolean) {

  constructor(path: String, internal: Boolean) : this(File(path), internal)

  init {
    if (file.exists()) {
      createLocationCode()
    }
  }

  var isDuplicate = false
    private set

  val name: String
    get() = file.path

  // Create unique file to identify duplicate devices.
  private fun createLocationCode() {
    if (!getLocationCodeFromFolder(file)) {
      File(file, ".$LOCATION_EXTENSION").let { locationCode ->
        try {
          locationCode.createNewFile()
          FileWriter(locationCode).use { it.write(file.path) }
        } catch (ioException: IOException) {
          Log.d("StorageDevice", "could not write file $file", ioException)
        }
      }
    }
  }

  // Check if there is already a device code in our path
  private fun getLocationCodeFromFolder(folder: File): Boolean {
    val locationCode = File(folder, ".$LOCATION_EXTENSION")
    if (locationCode.exists()) {
      try {
        BufferedReader(FileReader(locationCode)).use { br ->
          if (br.readLine() == file.path) {
            isDuplicate = false
          } else {
            isDuplicate = true
            return@getLocationCodeFromFolder true
          }
        }
      } catch (e: Exception) {
        return true
      }
    }
    val parent = folder.parentFile
    if (parent == null) {
      isDuplicate = false
      return false
    }
    return getLocationCodeFromFolder(parent)
  }
}
