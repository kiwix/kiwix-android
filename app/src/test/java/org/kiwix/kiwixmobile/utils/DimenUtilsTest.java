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

  DimenUtils t = new DimenUtils();
  @Mock Context context;
  @Mock Resources resources;


  public static void setFinalStatic(Field field, Object newValue) throws Exception {
    field.setAccessible(true);

    Field modifiersField = Field.class.getDeclaredField("modifiers");
    modifiersField.setAccessible(true);
    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

    field.set(null, newValue);
  }

  @Test
  public void checkTranslucentBarheight(){

    when(context.getResources()).thenReturn(resources);
    when(resources.getIdentifier("status_bar_height", "dimen", "android")).thenReturn(0);
    try{
      setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 15);
      assertEquals("",0,t.getTranslucentStatusBarHeight(context));
    }catch (Exception e){
      //do nothing
    }


    when(context.getResources()).thenReturn(resources);
    when(resources.getIdentifier("status_bar_height", "dimen", "android")).thenReturn(3);
    when(resources.getDimensionPixelSize(anyInt())).thenReturn(100);
    try{
      setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 44);
      assertEquals("",100,t.getTranslucentStatusBarHeight(context));
    }catch (Exception e){
      //do nothing
    }
    //TODO : make more indepth case analysis for each possible test case
  }

  @Test
  public void testToolBarHeight(){
    //TODO : check if correct toolbar hieght is returned
  }
}
