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

package org.kiwix.kiwixmobile.utils;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DimenUtilsTest {

  @Mock Context context;
  @Mock Resources resources;

  //Test the Translucent bar height returned
  @Test
  public void checkTranslucentBarheight(){
    when(context.getResources()).thenReturn(resources);
    when(resources.getIdentifier("status_bar_height", "dimen", "android")).thenReturn(10);
    when(resources.getDimensionPixelSize(anyInt())).thenReturn(100);

    //Case 1 :build SDK version < Lollipop
    //(status bar height is 100px but it's not translucent)
    try{
      SetSDKVersion(Build.VERSION.class.getField("SDK_INT"), 20);
    }catch (Exception e){
      Log.d("DimenUtilsTest", "Unable to set Build SDK Version");
    }
    assertEquals("", 0, DimenUtils.getTranslucentStatusBarHeight(context));

    //Case 2 :build SDK version >= Lollipop
    //(status bar height is 100px and it's translucent)
    try{
      SetSDKVersion(Build.VERSION.class.getField("SDK_INT"), 21);
      assertEquals("", 100, DimenUtils.getTranslucentStatusBarHeight(context));
    }catch (Exception e){
      Log.d("DimenUtilsTest", "Unable to set Build SDK Version");
    }
  }

  //Sets the Build SDK version
  private static void SetSDKVersion(Field field, Object newValue) throws Exception {
    field.setAccessible(true);
    Field modifiersField = Field.class.getDeclaredField("modifiers");
    modifiersField.setAccessible(true);
    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    field.set(null, newValue);
  }
}
