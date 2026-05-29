/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.zimManager

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.kiwix.kiwixmobile.core.zim_manager.MountInfo
import org.kiwix.kiwixmobile.core.zim_manager.MountPointProducer
import org.kiwix.kiwixmobile.zimManager.FileSystemCapability.CANNOT_WRITE_4GB
import org.kiwix.kiwixmobile.zimManager.FileSystemCapability.CAN_WRITE_4GB
import org.kiwix.kiwixmobile.zimManager.FileSystemCapability.INCONCLUSIVE

class MountFileSystemCheckerTest {
  private val mountPointProducer: MountPointProducer = mockk()
  private lateinit var mountFileSystemChecker: MountFileSystemChecker

  @Before
  fun setup() {
    mountFileSystemChecker = MountFileSystemChecker(mountPointProducer)
  }

  @After
  fun teardown() {
    unmockkAll()
    clearAllMocks()
  }

  @Test
  fun `returns INCONCLUSIVE when there are no mount points`() {
    every { mountPointProducer.produce() } returns emptyList()
    val capability = mountFileSystemChecker.checkFilesystemSupports4GbFiles("/storage/emulated/0")
    assertThat(capability).isEqualTo(INCONCLUSIVE)
  }

  @Test
  fun `returns INCONCLUSIVE when no mount points match path`() {
    val mountPoints = listOf(
      MountInfo("device1", "/other/path", "ext4")
    )
    every { mountPointProducer.produce() } returns mountPoints
    val capability = mountFileSystemChecker.checkFilesystemSupports4GbFiles("no_match")
    assertThat(capability).isEqualTo(INCONCLUSIVE)
  }

  @Test
  fun `returns CAN_WRITE_4GB when matching mount point supports 4GB`() {
    val mountPoints = listOf(
      MountInfo("device1", "/storage/emulated/0", "ext4"),
      MountInfo("device2", "/other", "vfat")
    )
    every { mountPointProducer.produce() } returns mountPoints
    val capability =
      mountFileSystemChecker.checkFilesystemSupports4GbFiles("/storage/emulated/0/Download")
    assertThat(capability).isEqualTo(CAN_WRITE_4GB)
  }

  @Test
  fun `returns CANNOT_WRITE_4GB when matching mount point does not support 4GB`() {
    val mountPoints = listOf(
      MountInfo("device1", "/storage/emulated/0", "vfat")
    )
    every { mountPointProducer.produce() } returns mountPoints
    val capability =
      mountFileSystemChecker.checkFilesystemSupports4GbFiles("/storage/emulated/0/Download")
    assertThat(capability).isEqualTo(CANNOT_WRITE_4GB)
  }

  @Test
  fun `recursively determines filesystem when matched mount point is virtual`() {
    val mountPoints = listOf(
      MountInfo("/dev/block/vold", "/storage/emulated/0", "fuse"),
      MountInfo("/data/media", "/dev/block/vold", "ext4")
    )
    every { mountPointProducer.produce() } returns mountPoints
    val capability =
      mountFileSystemChecker.checkFilesystemSupports4GbFiles("/storage/emulated/0/Download")
    assertThat(capability).isEqualTo(CAN_WRITE_4GB)
  }

  @Test
  fun `returns INCONCLUSIVE when matching mount point has unknown filesystem type`() {
    val mountPoints = listOf(
      MountInfo("device1", "/storage/emulated/0", "unknown_fs")
    )
    every { mountPointProducer.produce() } returns mountPoints
    val capability =
      mountFileSystemChecker.checkFilesystemSupports4GbFiles("/storage/emulated/0/Download")
    assertThat(capability).isEqualTo(INCONCLUSIVE)
  }

  @Test
  fun `uses most specific matching mount point`() {
    val mountPoints = listOf(
      MountInfo("device1", "/storage", "vfat"),
      MountInfo("device2", "/storage/emulated/0", "ext4")
    )

    every { mountPointProducer.produce() } returns mountPoints
    val capability = mountFileSystemChecker
      .checkFilesystemSupports4GbFiles("/storage/emulated/0/Download")
    assertThat(capability).isEqualTo(CAN_WRITE_4GB)
  }
}
