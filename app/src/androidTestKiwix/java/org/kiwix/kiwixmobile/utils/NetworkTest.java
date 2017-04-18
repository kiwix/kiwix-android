package org.kiwix.kiwixmobile.utils;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

import android.support.test.rule.ActivityTestRule;
import java.io.IOException;
import java.io.InputStream;
import javax.inject.Inject;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.ZimContentProvider;
import org.kiwix.kiwixmobile.di.components.DaggerTestComponent;
import org.kiwix.kiwixmobile.di.components.TestComponent;
import org.kiwix.kiwixmobile.di.modules.ApplicationModule;

/**
 * Created by mhutti1 on 14/04/17.
 */

public class NetworkTest {

  @Inject OkHttpClient okHttpClient;
  @Inject MockWebServer mockWebServer;


  @Rule
  public ActivityTestRule<KiwixMobileActivity> mActivityTestRule = new ActivityTestRule<>(
      KiwixMobileActivity.class, false, false);

  @Before
  public void setUp() {
    TestComponent component = DaggerTestComponent.builder().applicationModule
        (new ApplicationModule(
            (KiwixApplication) getInstrumentation().getTargetContext().getApplicationContext())).build();

    ((KiwixApplication) getInstrumentation().getTargetContext().getApplicationContext()).setApplicationComponent(component);

    new ZimContentProvider().setupDagger();
    component.inject(this);
    InputStream library = NetworkTest.class.getClassLoader().getResourceAsStream("library.xml");
    InputStream metalinks = NetworkTest.class.getClassLoader().getResourceAsStream("wikipedia_af_all_nopic_2016-05.zim.meta4");
    try {
      byte[] libraryBytes = IOUtils.toByteArray(library);
      mockWebServer.enqueue(new MockResponse().setBody(new String(libraryBytes)));
      byte[] metalinkBytes = IOUtils.toByteArray(metalinks);
      mockWebServer.enqueue(new MockResponse().setBody(new String(metalinkBytes)));
      mockWebServer.enqueue(new MockResponse().setHeader("Content-Length", 63973123));
      Buffer buffer = new Buffer();
      buffer.write(new byte[63973123]);
      buffer.close();
      mockWebServer.enqueue(new MockResponse().setBody(buffer));
      mockWebServer.enqueue(new MockResponse().setBody(buffer));
      mockWebServer.enqueue(new MockResponse().setBody(buffer));

    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  @Test
  public void zimTest() {

    mActivityTestRule.launchActivity(null);

    try {
      Thread.sleep(100000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
