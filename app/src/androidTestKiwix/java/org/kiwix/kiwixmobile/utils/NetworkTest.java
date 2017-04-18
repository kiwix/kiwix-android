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
    InputStream inStream = NetworkTest.class.getClassLoader().getResourceAsStream("library.xml");
    try {
      byte[] summary = IOUtils.toByteArray(inStream);
      mockWebServer.enqueue(new MockResponse().setBody(new String(summary)));
      mockWebServer.enqueue(new MockResponse().setBody(new String(summary)));
      mockWebServer.enqueue(new MockResponse().setBody(new String(summary)));
      mockWebServer.enqueue(new MockResponse().setBody(new String(summary)));
      mockWebServer.enqueue(new MockResponse().setBody(new String(summary)));

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
