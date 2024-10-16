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

package org.kiwix.kiwixmobile.custom.download.main

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.custom.main.CustomFileValidator
import org.kiwix.kiwixmobile.custom.main.ValidationState
import java.io.File

class CustomFileValidatorTest {

  private lateinit var context: Context
  private lateinit var customFileValidator: CustomFileValidator
  private lateinit var assetManager: AssetManager

  @BeforeEach
  fun setUp() {
    context = mockk(relaxed = true)
    assetManager = mockk(relaxed = true)
    customFileValidator = CustomFileValidator(context)
  }

  @Test
  fun `validate should call onFilesFound when both OBB and ZIM files are found`() {
    val obbFile = mockk<File>()
    val zimFile = mockk<File>()
    mockZimFiles(arrayOf(obbFile), "obb")
    mockZimFiles(arrayOf(zimFile), "zim")

    customFileValidator.validate(
      onFilesFound = {
        assertTrue(it is ValidationState.HasBothFiles)
        assertEquals(obbFile, (it as ValidationState.HasBothFiles).obbFile)
        assertEquals(zimFile, it.zimFile)
      },
      onNoFilesFound = { fail("Should not call onNoFilesFound") }
    )
  }

  @Test
  fun `validate should call onFilesFound when only OBB file is found`() {
    val obbFile = mockk<File>()
    mockZimFiles(arrayOf(obbFile), "obb")
    mockZimFiles(arrayOf(), "zim")

    customFileValidator.validate(
      onFilesFound = {
        assertTrue(it is ValidationState.HasFile)
        assertEquals(obbFile, (it as ValidationState.HasFile).file)
      },
      onNoFilesFound = { fail("Should not call onNoFilesFound") }
    )
  }

  @Test
  fun `validate should call onFilesFound when only ZIM file is found`() {
    val zimFile = mockk<File>()
    mockZimFiles(arrayOf(), "obb")
    mockZimFiles(arrayOf(zimFile), "zim")

    customFileValidator.validate(
      onFilesFound = {
        assertTrue(it is ValidationState.HasFile)
        assertEquals(zimFile, (it as ValidationState.HasFile).file)
      },
      onNoFilesFound = { fail("Should not call onNoFilesFound") }
    )
  }

  @Test
  fun `validate should call onNoFilesFound when no OBB or ZIM files are found`() {
    mockZimFiles(arrayOf(), extension = "zim")
    mockZimFiles(arrayOf(), extension = "obb")

    customFileValidator.validate(
      onFilesFound = { fail("Should not call onFilesFound") },
      onNoFilesFound = { }
    )
  }

  @Test
  fun `validate should call onNoFilesFound when directories are null`() {
    mockZimFiles(null, "zim")
    mockZimFiles(null, "obb")

    customFileValidator.validate(
      onFilesFound = { fail("Should not call onFilesFound") },
      onNoFilesFound = { }
    )
  }

  @Test
  fun `validate should call onNoFilesFound when no matching files are found`() {
    val textFile = mockk<File>()
    mockZimFiles(arrayOf(textFile), "txt")

    customFileValidator.validate(
      onFilesFound = { fail("Should not call onFilesFound") },
      onNoFilesFound = { }
    )
  }

  @Test
  fun `validate should call onFilesFound for case insensitive file extensions`() {
    val zimFile = mockk<File>()
    mockZimFiles(arrayOf(zimFile), "ZIM")

    customFileValidator.validate(
      onFilesFound = {
        fail("Should not call onFilesFound")
      },
      onNoFilesFound = {}
    )
  }

  @Test
  fun `getAssetFileDescriptorListFromPlayAssetDelivery returns empty list when exception occurs`() {
    every {
      context.createPackageContext(
        any(),
        any()
      ).assets
    } throws PackageManager.NameNotFoundException()

    val assetList = customFileValidator.getAssetFileDescriptorListFromPlayAssetDelivery()

    assertTrue(assetList.isEmpty())
  }

  @Test
  fun `getAssetFileDescriptorListFromPlayAssetDelivery returns list of asset descriptors`() {
    val descriptor = mockk<AssetFileDescriptor>()
    every { context.createPackageContext(any(), any()).assets } returns assetManager
    every { assetManager.openFd(any()) } returns descriptor
    every { assetManager.list("") } returns arrayOf("chunk1.zim", "chunk2.zim")

    val assetList = customFileValidator.getAssetFileDescriptorListFromPlayAssetDelivery()

    assertEquals(2, assetList.size)
    assertEquals(descriptor, assetList[0])
  }

  private fun mockZimFiles(
    zimFilesArray: Array<File?>?,
    extension: String
  ) {
    zimFilesArray?.forEach {
      it?.let {
        every { it.exists() } returns true
        every { it.isFile } returns true
        every { it.extension } returns extension
        every { it.isDirectory } returns false
        every { it.name } returns "sample.$extension"
      }
    }
    val storageDirectory = mockk<File>()
    every { storageDirectory.exists() } returns true
    every { storageDirectory.isDirectory } returns true
    every { storageDirectory.extension } returns ""
    every { storageDirectory.parent } returns null
    every { storageDirectory.listFiles() } returns zimFilesArray

    if (extension == "zim") {
      every {
        ContextCompat.getExternalFilesDirs(context, null)
      } returns arrayOf(storageDirectory)
    } else {
      every { ContextCompat.getObbDirs(context) } returns arrayOf(storageDirectory)
    }
  }
}
