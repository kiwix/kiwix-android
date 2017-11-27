package org.kiwix.kiwixmobile.di.modules;

import org.apache.commons.io.IOUtils;
import org.kiwix.kiwixlib.JNIKiwix;
import org.kiwix.kiwixlib.JNIKiwixString;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

/**
 * Created by mhutti1 on 13/04/17.
 */

@Module
public class TestJNIModule{

  @Provides
  @Singleton
  public JNIKiwix providesJNIKiwix() {
    JNIKiwix jniKiwix = Mockito.mock(JNIKiwix.class);

    /*doReturn("A/index.htm").when(jniKiwix).getMainPage();
    doReturn(true).when(jniKiwix).loadZIM(any());
    doReturn(true).when(jniKiwix).loadFulltextIndex(any());
    doReturn("mockid").when(jniKiwix).getId();
    doReturn("mockname").when(jniKiwix).getName();
    doReturn("Test Description").when(jniKiwix).getDescription();
    doAnswer(invocation -> {
      ((JNIKiwixString) invocation.getArgument(0)).value = "Test Title";
      return true;
    }).when(jniKiwix).getTitle(any());
    doReturn("Test Publisher").when(jniKiwix).getPublisher();
    doReturn("Test Date").when(jniKiwix).getDate();
    doReturn("Test Language").when(jniKiwix).getLanguage();

    try {
      InputStream inStream = TestJNIModule.class.getClassLoader().getResourceAsStream("summary");
      byte[] summary = IOUtils.toByteArray(inStream);
      InputStream inStream2 = TestJNIModule.class.getClassLoader().getResourceAsStream("testpage");
      byte[] fool = IOUtils.toByteArray(inStream2);
      doReturn(summary).when(jniKiwix).getContent(eq("A/index.htm"),any(),any(),any());
      doReturn(fool).when(jniKiwix).getContent(eq("A/A_Fool_for_You.html"),any(),any(),any());
    } catch (IOException e) {
      e.printStackTrace();
    }*/

    return jniKiwix;
  }

}
