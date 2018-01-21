/*
 * Copyright 2013  Rashiq Ahmad <rashiq.z@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU  General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.kiwix.kiwixmobile.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.kiwix.kiwixmobile.library.LibraryAdapter;
import org.kiwix.kiwixmobile.utils.files.FileUtils;

import java.lang.reflect.Field;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

import static org.kiwix.kiwixmobile.utils.Constants.PREF_LANG;
import static org.kiwix.kiwixmobile.utils.Constants.TAG_KIWIX;

public class LanguageUtils {

  private static HashMap<String, Locale> mLocaleMap;
  private List<LanguageContainer> mLanguageList;
  private List<String> mLocaleLanguageCodes;
  private Context mContext;

  public LanguageUtils(Context context) {
    mContext = context;
    mLanguageList = new ArrayList<>();
    mLocaleLanguageCodes = new ArrayList<>();
    getLanguageCodesFromAssets();
    setupLanguageList();
    sortLanguageList(context.getResources().getConfiguration().locale);
  }

  public static void handleLocaleChange(Context context) {

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String language = prefs.getString(PREF_LANG, "");

    if (language.isEmpty()) {
      return;
    }

    handleLocaleChange(context, language);
  }

  public static void handleLocaleChange(Context context, String language) {

    Locale locale = new Locale(language);
    Locale.setDefault(locale);
    Configuration config = new Configuration();
    config.locale = locale;
    if (Build.VERSION.SDK_INT >= 17) {
      config.setLayoutDirection(locale);
    }
    context.getResources()
        .updateConfiguration(config, context.getResources().getDisplayMetrics());
  }

  /**
   * Converts ISO3 language code to {@link java.util.Locale}.
   *
   * @param iso3 ISO3 language code
   * @return {@link java.util.Locale} that represents the language of the provided code
   */
  public static Locale ISO3ToLocale(String iso3) {
    if (mLocaleMap == null) {
      Locale[] locales = Locale.getAvailableLocales();
      mLocaleMap = new HashMap<>();
      for (Locale locale : locales) {
        try {
          mLocaleMap.put(locale.getISO3Language().toUpperCase(), locale);
        } catch (MissingResourceException e) {
          // Do nothing
        }
      }
    }
    return mLocaleMap.get(iso3.toUpperCase());
  }

  @TargetApi(Build.VERSION_CODES.N)
  public static Locale getCurrentLocale(Context context){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
      return context.getResources().getConfiguration().getLocales().get(0);
    } else{
      //noinspection deprecation
      return context.getResources().getConfiguration().locale;
    }
  }

  // This method will determine which font will be applied to the not-supported-locale.
  // You can define exceptions to the default DejaVu font in the 'exceptions' Hashmap:
  public static String getTypeface(String languageCode) {

    // Define the exceptions to the rule. The font has to be placed in the assets folder.
    // Key: the language code; Value: the name of the font.
    HashMap<String, String> exceptions = new HashMap<>();
    exceptions.put("km", "fonts/KhmerOS.ttf");
    exceptions.put("my", "fonts/Parabaik.ttf");
    exceptions.put("guj", "fonts/Lohit-Gujarati.ttf");
    exceptions.put("ori", "fonts/Lohit-Odia.ttf");
    exceptions.put("pan", "fonts/Lohit-Punjabi.ttf");
    exceptions.put("dzo", "fonts/DDC_Uchen.ttf");
    exceptions.put("bod", "fonts/DDC_Uchen.ttf");
    exceptions.put("chr", "fonts/Digohweli.ttf");
    exceptions.put("sin", "fonts/Kaputaunicode.ttf");

    // These scripts could be supported via more Lohit fonts if DejaVu doesn't
    // support them.  That is untested now as they aren't even in the language
    // menu:
    //  * (no ISO code?) (Devanagari/Nagari) -- at 0% in translatewiki
    //  * mr (Marathi) -- at 21% in translatewiki

    // Check, if an exception applies to our current locale
    if (exceptions.containsKey(languageCode)) {
      return exceptions.get(languageCode);
    }

    // Return the default font
    return "fonts/DejaVuSansCondensed.ttf";
  }

  // Read the language codes, that are supported in this app from the locales.txt file
  private void getLanguageCodesFromAssets() {

    List<String> locales = new ArrayList<>(FileUtils.readLocalesFromAssets(mContext));

    for (String locale : locales) {

      if (!locale.isEmpty()) {
        mLocaleLanguageCodes.add(locale.trim());
      }
    }
  }

  // Create a list containing the language code and the corresponding (english) langauge name
  private void setupLanguageList() {

    for (String languageCode : mLocaleLanguageCodes) {
      mLanguageList.add(new LanguageContainer(languageCode));
    }
  }

  // Sort the language list by the language name
  private void sortLanguageList(Locale locale) {

    Collator localeCollator = Collator.getInstance(locale);
    localeCollator.setStrength(Collator.SECONDARY);

    Collections.sort(mLanguageList, new Comparator<LanguageContainer>() {
      @Override
      public int compare(LanguageContainer a, LanguageContainer b) {
        return localeCollator.compare(a.getLanguageName(), b.getLanguageName());
      }
    });
  }

  // Check, if the selected Locale is supported and weather we actually need to change our font.
  // We do this by checking, if our Locale is available in the List, that Locale.getAvailableLocales() returns.
  private boolean haveToChangeFont() {

    for (Locale s : Locale.getAvailableLocales()) {
      if (s.getLanguage().equals(Locale.getDefault().toString())) {
        return false;
      }

      // Don't change the language, if the options hasn't been set
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
      String language = prefs.getString(PREF_LANG, "");

      if (language.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  // Change the font of all the TextViews and its subclasses in our whole app by attaching a custom
  // Factory to the LayoutInflater of the Activity.
  // The Factory is called on each element name as the xml is parsed and we can therefore modify
  // the parsed Elements.
  // A Factory can only be set once on a LayoutInflater. And since we are using the support Library,
  // which also sets a Factory on the LayoutInflator, we have to access the private field of the
  // LayoutInflater, that handles this restriction via Java's reflection API
  // and make it accessible set it to false again.
  public void changeFont(LayoutInflater layoutInflater) {

    if (!haveToChangeFont()) {
      return;
    }

    try {
      Field field = LayoutInflater.class.getDeclaredField("mFactorySet");
      field.setAccessible(true);
      field.setBoolean(layoutInflater, false);
      layoutInflater.setFactory(new LayoutInflaterFactory(mContext, layoutInflater));
    } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
      Log.w(TAG_KIWIX, "Font Change Failed: Could not access private field of the LayoutInflater", e);
    }
  }

  // Get a list of all the language names
  public List<String> getValues() {

    List<String> values = new ArrayList<>();

    for (LanguageContainer value : mLanguageList) {
      values.add(value.getLanguageName());
    }

    return values;
  }

  public List<LibraryAdapter.Language> getLanguageList() {
    List<LibraryAdapter.Language> values = new ArrayList<>();

    for (LanguageContainer value : mLanguageList) {
      values.add(new LibraryAdapter.Language(value.getLanguageCode(), false));
    }

    return values;
  }

  // Get a list of all the language codes
  public List<String> getKeys() {

    List<String> keys = new ArrayList<>();

    for (LanguageContainer key : mLanguageList) {
      keys.add(key.getLanguageCode());
    }

    return keys;
  }

  // That's the Factory, that will handle the manipulation of all our TextView's and its subcalsses
  // while the content is being parsed
  public static class LayoutInflaterFactory implements LayoutInflater.Factory {

    private Context mContext;

    private LayoutInflater mLayoutInflater;

    public LayoutInflaterFactory(Context context, LayoutInflater layoutInflater) {
      mContext = context;
      mLayoutInflater = layoutInflater;
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {

      // Apply the custom font, if the xml tag equals "TextView", "EditText" or "AutoCompleteTextView"
      if (name.equalsIgnoreCase("TextView")
          || name.equalsIgnoreCase("EditText")
          || name.equalsIgnoreCase("AutoCompleteTextView")) {

        try {
          LayoutInflater inflater = mLayoutInflater;
          final View view = inflater.createView(name, null, attrs);
          new Handler().post(new Runnable() {
            public void run() {
              TextView textView = ((TextView) view);

              // Set the custom typeface
              textView.setTypeface(Typeface.createFromAsset(mContext.getAssets(),
                      getTypeface(Locale.getDefault().getLanguage())));
              Log.d(TAG_KIWIX, "Applying custom font");

              // Reduce the text size
              textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                  textView.getTextSize() - 2f);
            }
          });

          return view;
        } catch (InflateException | ClassNotFoundException e) {
          Log.w(TAG_KIWIX, "Could not apply the custom font to " + name, e);
        }
      }

      return null;
    }
  }

  public static class LanguageContainer {

    private String mLanguageCode;

    private String mLanguageName;

    // This constructor will take care of creating a language name for the given ISO 639-1 language code.
    // The language name will always be in english to ensure user friendliness and to prevent
    // possible incompatibilities, since not all language names are available in all languages.
    public LanguageContainer(String languageCode) {
      mLanguageCode = languageCode;

      try {
        mLanguageName = new Locale(languageCode).getDisplayLanguage();

        // Use the English name of the language, if the language name is not
        // available in the current Locale
        if (mLanguageName.length() == 2) {
          mLanguageName = new Locale(languageCode).getDisplayLanguage(new Locale("en"));
        }
      } catch (Exception e) {
        mLanguageName = "";
      }
    }

    public String getLanguageCode() {
      return mLanguageCode;
    }

    public String getLanguageName() {
      return mLanguageName;
    }
  }
}
