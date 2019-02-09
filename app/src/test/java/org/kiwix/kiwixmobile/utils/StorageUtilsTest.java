package org.kiwix.kiwixmobile.utils;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class StorageUtilsTest {
  private static final String DNS_NAME_WWW_KIWIX_ORG = "www.kiwix.org";
  private String url;
  private String fileNameExpected;

  @Parameterized.Parameters
  public static Collection<Object[]> configs() {
    final Object[][] data = new Object[][] {
        { "https://www.kiwix.org", DNS_NAME_WWW_KIWIX_ORG }
      , { "http://www.kiwix.org", DNS_NAME_WWW_KIWIX_ORG }
      , { "http://www.kiwix.org?", DNS_NAME_WWW_KIWIX_ORG }
      , { "http://www.kiwix.org?a=b", DNS_NAME_WWW_KIWIX_ORG }
      , { "http://www.kiwix.org/a.html?a=b", "a.html" }
      , { "http://www.kiwix.org/x/a.html?a=b", "a.html" }
      , { "http://www.kiwix.org/x/a.meta4", "a" }
      , { "a.html.meta4", "a.html" }
    };
    return Arrays.asList(data);
  }

  public StorageUtilsTest(String url, String fileNameExpected) {
    this.url = url;
    this.fileNameExpected = fileNameExpected;
  }

  @Test
  public void getFileNameFromUrl_returns_expected_value() {
    assertEquals(fileNameExpected, StorageUtils.getFileNameFromUrl(url));
  }
}
