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
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.content.ContextCompat
import org.kiwix.kiwixmobile.core.extensions.deleteFile
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.kiwixmobile.custom.main.ValidationState.HasBothFiles
import org.kiwix.kiwixmobile.custom.main.ValidationState.HasFile
import org.kiwix.kiwixmobile.custom.main.ValidationState.HasNothing
import java.io.File
import java.io.FileOutputStream
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
    assetFileDescriptor: AssetFileDescriptor? = getAssetFileDescriptorFromPlayAssetDelivery()
  ): ValidationState {
    return when {
      assetFileDescriptor != null -> HasFile(null, assetFileDescriptor)
      obbFiles.isNotEmpty() && zimFiles().isNotEmpty() -> HasBothFiles(obbFiles[0], zimFiles[0])
      obbFiles.isNotEmpty() -> HasFile(obbFiles[0])
      zimFiles.isNotEmpty() -> HasFile(zimFiles[0])
      else -> HasNothing
    }
  }

  @Suppress("MagicNumber")
  private fun getAssetFileDescriptorFromPlayAssetDelivery(): AssetFileDescriptor? {
    try {
      val context = context.createPackageContext(context.packageName, 0)
      val assetManager = context.assets
      val assetFileDescriptorList: ArrayList<AssetFileDescriptor> = arrayListOf()
      getChunksList(assetManager).forEach {
        assetFileDescriptorList.add(assetManager.openFd(it))
      }
      val combinedFilePath = FileUtils.getDemoFilePathForCustomApp(context)
      val demoFile = File(combinedFilePath)
      if (demoFile.isFileExist()) demoFile.deleteFile()
      demoFile.createNewFile()
      val combinedFileOutputStream = FileOutputStream(combinedFilePath)
      val chunkSize = 100 * 1024 * 1024

      for (chunkNumber in 0 until assetFileDescriptorList.size) {
        val chunkFileName = "chunk$chunkNumber.zim"
        val chunkFileInputStream =
          context.assets.open(chunkFileName)

        val buffer = ByteArray(4096)
        var bytesRead: Int

        while (chunkFileInputStream.read(buffer).also { bytesRead = it } != -1) {
          combinedFileOutputStream.write(
            buffer,
            0,
            bytesRead
          )
        }

        chunkFileInputStream.close()
      }
      return AssetFileDescriptor(
        ParcelFileDescriptor.open(
          demoFile,
          ParcelFileDescriptor.MODE_READ_ONLY
        ),
        0L,
        demoFile.length()
      )
    } catch (packageNameNotFoundException: PackageManager.NameNotFoundException) {
      Log.w(
        "ASSET_PACKAGE_DELIVERY",
        "Asset package is not found ${packageNameNotFoundException.message}"
      )
    } catch (ioException: IOException) {
      Log.w("ASSET_PACKAGE_DELIVERY", "Unable to copy the content of asset $ioException")
    }
    return null
  }

  private fun getChunksList(assetManager: AssetManager): MutableList<String> {
    val chunkFiles = mutableListOf<String>()

    try {
      // List all files in the asset directory
      val assets = assetManager.list("") ?: emptyArray()

      // Filter and count chunk files based on your naming convention
      assets.filterTo(chunkFiles) { it.startsWith("chunk") && it.endsWith(".zim") }
    } catch (ioException: IOException) {
      ioException.printStackTrace()
    }

    return chunkFiles
  }

  private fun obbFiles() = scanDirs(ContextCompat.getObbDirs(context), "obb")

  private fun zimFiles(): List<File> {
    // Create a list to store the parent directories
    val directoryList = mutableListOf<File>()

    // Get the external files directories for the app
    ContextCompat.getExternalFilesDirs(context, null).forEach { dir ->
      // Check if the directory's parent is not null
      dir?.parent?.let { parentPath ->
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
    return scanDirs(directoryList.toTypedArray(), "zim")
  }

  private fun scanDirs(dirs: Array<out File?>?, extensionToMatch: String): List<File> =
    dirs?.filterNotNull()?.fold(listOf()) { acc, dir ->
      acc + dir.walk().filter { it.extension.startsWith(extensionToMatch) }.toList()
    } ?: emptyList()
}

sealed class ValidationState {
  data class HasBothFiles(val obbFile: File, val zimFile: File) : ValidationState()
  data class HasFile(val file: File?, val assetFileDescriptor: AssetFileDescriptor? = null) :
    ValidationState()

  object HasNothing : ValidationState()
}
