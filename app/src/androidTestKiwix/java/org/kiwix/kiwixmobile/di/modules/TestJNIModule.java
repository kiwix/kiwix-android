package org.kiwix.kiwixmobile.di.modules;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import java.io.IOException;
import java.io.InputStream;
import javax.inject.Singleton;
import org.apache.commons.io.IOUtils;
import org.kiwix.kiwixlib.JNIKiwix;
import org.mockito.Mockito;

/**
 * Created by mhutti1 on 13/04/17.
 */

@Module
public class TestJNIModule{

  @Provides
  @Singleton
  public JNIKiwix providesJNIKiwix(Context context) {
    JNIKiwix jniKiwix = Mockito.mock(JNIKiwix.class);
    try {
      InputStream inStream = TestJNIModule.class.getClassLoader().getResourceAsStream("summary");
      byte[] summary = IOUtils.toByteArray(inStream);
      InputStream inStream2 = TestJNIModule.class.getClassLoader().getResourceAsStream("testpage");
      byte[] fool = IOUtils.toByteArray(inStream2);
      doReturn(summary).when(jniKiwix).getContent(eq("A/index.htm"),any(),any());
      doReturn(fool).when(jniKiwix).getContent(eq("A/A_Fool_for_You.html"),any(),any());
      doReturn("A/index.htm").when(jniKiwix).getMainPage();
      doReturn(true).when(jniKiwix).loadZIM(any());
      doReturn(true).when(jniKiwix).loadFulltextIndex(any());
      doReturn("mockid").when(jniKiwix).getId();
      doReturn("mockname").when(jniKiwix).getName();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return jniKiwix;
  }

}
