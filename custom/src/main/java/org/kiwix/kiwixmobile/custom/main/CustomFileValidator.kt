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

package org.kiwix.kiwixmobile.custom.main

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import androidx.core.content.ContextCompat
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.custom.main.ValidationState.HasBothFiles
import org.kiwix.kiwixmobile.custom.main.ValidationState.HasFile
import org.kiwix.kiwixmobile.custom.main.ValidationState.HasNothing
import java.io.File
import java.io.IOException
import javax.inject.Inject

class CustomFileValidator @Inject constructor(private val context: Context) {

  fun validate(onFilesFound: (ValidationState) -> Unit, onNoFilesFound: () -> Unit) =
    when (val installationState = detectInstallationState()) {
      is HasBothFiles,
      is HasFile -> onFilesFound(installationState)

      HasNothing -> onNoFilesFound()
    }

  private fun detectInstallationState(
    obbFiles: List<File> = obbFiles(),
    zimFiles: List<File> = zimFiles(),
    assetFileDescriptorList: List<AssetFileDescriptor> =
      getAssetFileDescriptorListFromPlayAssetDelivery()
  ): ValidationState {
    return when {
      assetFileDescriptorList.isNotEmpty() -> HasFile(null, assetFileDescriptorList)
      obbFiles.isNotEmpty() && zimFiles().isNotEmpty() -> HasBothFiles(obbFiles[0], zimFiles[0])
      obbFiles.isNotEmpty() -> HasFile(obbFiles[0])
      zimFiles.isNotEmpty() -> HasFile(zimFiles[0])
      else -> HasNothing
    }
  }

  @Suppress("MagicNumber")
  fun getAssetFileDescriptorListFromPlayAssetDelivery(): List<AssetFileDescriptor> {
    try {
      val assetManager = context.createPackageContext(context.packageName, 0).assets
      val assetFileDescriptorList: ArrayList<AssetFileDescriptor> = arrayListOf()
      getChunksList(assetManager).forEach {
        assetFileDescriptorList.add(assetManager.openFd(it))
      }

      return assetFileDescriptorList
    } catch (packageNameNotFoundException: PackageManager.NameNotFoundException) {
      Log.w(
        "ASSET_PACKAGE_DELIVERY",
        "Asset package is not found ${packageNameNotFoundException.message}"
      )
    } catch (ioException: IOException) {
      Log.w("ASSET_PACKAGE_DELIVERY", "Unable to copy the content of asset $ioException")
    }
    return emptyList()
  }

  private fun getChunksList(assetManager: AssetManager): List<String> {
    val chunkFiles = mutableListOf<String>()

    try {
      // List of all files in the asset directory
      val assets = assetManager.list("") ?: emptyArray()

      // Filter and count chunk files.
      assets.filterTo(chunkFiles) { it.startsWith("chunk") && it.endsWith(".zim") }
      chunkFiles.sortBy { it.substringAfter("chunk").substringBefore(".zim").toInt() }
    } catch (ioException: IOException) {
      ioException.printStackTrace()
    }

    return chunkFiles
  }

  private fun obbFiles() =
    scanDirs(
      ContextCompat.getObbDirs(context).filterNotNull().filter(File::isFileExist).toTypedArray(),
      "obb"
    )

  private fun zimFiles(): List<File> {
    // Create a list to store the parent directories
    val directoryList = mutableListOf<File>()

    // Get the external files directories for the app
    ContextCompat.getExternalFilesDirs(context, null).filterNotNull()
      .filter(File::isFileExist)
      .forEach { dir ->
        // Check if the directory's parent is not null
        dir.parent?.let { parentPath ->
          // Add the parent directory to the list, so we can scan all the files contained in the folder.
          // We are doing this because ContextCompat.getExternalFilesDirs(context, null) method returns the path to the
          // "files" folder, which is specific to the app's package name, both for internal and SD card storage.
          // By obtaining the parent directory, we can scan files from the app-specific directory itself.
          directoryList.add(File(parentPath))
        } ?: kotlin.run {
          // If the parent directory is null, it means the current directory is the target folder itself.
          // Add the current directory to the list, as it represents the app-specific directory for both internal
          // and SD card storage. This allows us to scan files directly from this directory.
          directoryList.add(dir)
        }
      }
    return scanDirs(directoryList.toTypedArray(), "zim").filterNot {
      // Excluding the demo.zim file from the list as it is used for demonstration purposes
      // on the ZimHostFragment for hosting the ZIM file on the server.
      // Since we are now using the "asset delivery mode", in this we are using the
      // assetFileDescriptor instead of a regular file.
      it.name.equals("demo.zim", ignoreCase = true)
    }
  }

  private fun scanDirs(dirs: Array<out File?>?, extensionToMatch: String): List<File> =
    dirs?.filterNotNull()?.fold(listOf()) { acc, dir ->
      acc + dir.walk().filter { it.extension.startsWith(extensionToMatch) }.toList()
    } ?: emptyList()
}

sealed class ValidationState {
  data class HasBothFiles(val obbFile: File, val zimFile: File) : ValidationState()
  data class HasFile(
    val file: File?,
    val assetFileDescriptorList: List<AssetFileDescriptor> = emptyList()
  ) :
    ValidationState()

  object HasNothing : ValidationState()
}
