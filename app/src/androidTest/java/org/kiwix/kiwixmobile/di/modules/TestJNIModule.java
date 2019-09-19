/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.di.modules;

import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import org.kiwix.kiwixlib.JNIKiwix;
import org.mockito.Mockito;

/**
 * Created by mhutti1 on 13/04/17.
 */

@Module
public class TestJNIModule {

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
