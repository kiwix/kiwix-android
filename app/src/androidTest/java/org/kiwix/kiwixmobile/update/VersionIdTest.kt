package org.kiwix.kiwixmobile.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kiwix.kiwixmobile.core.BuildConfig
import org.kiwix.kiwixmobile.core.utils.workManager.VersionId

class VersionIdTest {
  @Test
  fun testVersionIdParse() {
    val version = VersionId("2.0")
    assertEquals(version.major, 2)
    assertEquals(version.minor, 0)
    assertEquals(version.build, 0)
    assertEquals(version.variantType, "")
    assertEquals(version.variantNumber, 0)
  }

  @Test
  fun testVersionIdCompare() {
    val version1 = VersionId("1.99.99")
    val version2 = VersionId("2.0.0")
    assertTrue(version1 < version2)
  }

  @Test
  fun testCurrentVersion() {
    val version1 = VersionId("2.4.6")
    val version2 = VersionId(BuildConfig.VERSION_NAME)
    assertTrue(version1 < version2)
  }
}
