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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class LanguageUtilsTest {

  @Mock Context context;
  @Mock Resources resources;
  @Mock Configuration configuration;
  //@Mock SharedPreferenceUtil sharedPreferenceUtil;


  @Before
  public void executeBefore(){
    Locale locale = new Locale("en");

    //when(context.getResources()).thenReturn(resources);
    //when(resources.getConfiguration()).thenReturn(configuration);
    //when(configuration.locale).thenReturn(locale);
    //TODO : fix the exception: when() requires an argument which has to be 'a method call on a mock'in the previous line
    //LanguageUtils t = new LanguageUtils(context);
  }


  /*
   * test whether the UI is updated if preffered language is different
   */
  @Test
  public void test(){
  //  when(context.getResources()).thenReturn(resources);
  //  when(resources.getConfiguration()).thenReturn(configuration);
  //  when(sharedPreferenceUtil.getPrefLanguage(anyString())).thenReturn("he");
  //  try{
  //    setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 17);
  //    t.handleLocaleChange(context,sharedPreferenceUtil);
  //    verify(resources).updateConfiguration(any(), any());
  //
  //  }catch (Exception e){
  //    //do nothing
  //  }
  //
  //  assertEquals("","","");
  //
  }

  //TODO : test whether the UI is updated on locale change

  //TODO : test font changes on UI elements


  public static void setFinalStatic(Field field, Object newValue) throws Exception {
    field.setAccessible(true);
    Field modifiersField = Field.class.getDeclaredField("modifiers");
    modifiersField.setAccessible(true);
    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    field.set(null, newValue);
  }
}
