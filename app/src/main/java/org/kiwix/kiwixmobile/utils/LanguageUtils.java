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

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import org.kiwix.kiwixmobile.utils.files.FileUtils;

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

  public static void handleLocaleChange(Context context,
      SharedPreferenceUtil sharedPreferenceUtil) {
    String language = sharedPreferenceUtil.getPrefLanguage("");

    if (language.isEmpty()) {
      return;
    }

    handleLocaleChange(context, language);
  }

  public static void handleLocaleChange(Context context, String language) {

    Locale locale = new Locale(language);
    Locale.setDefault(locale);
    Configuration config = new Configuration();
    if (Build.VERSION.SDK_INT >= 17) {
      config.setLocale(locale);
      config.setLayoutDirection(locale);
    } else {
      config.locale = locale;
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

  public static Locale getCurrentLocale(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      return context.getResources().getConfiguration().getLocales().get(0);
    } else {
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
    exceptions.put("sin", "fonts/Kaputa-Regular.ttf");

    // http://scriptsource.org/cms/scripts/page.php?item_id=entry_detail&uid=kstzk8hbg4
    // Link above shows that we are allowed to distribute this font
    exceptions.put("chr", "fonts/Digohweli.ttf");

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

  public static String getResourceString(Context appContext, String str) {
    String resourceName = str;
    if (resourceName.contains("REPLACE_")) {
      resourceName = resourceName.replace("REPLACE_", "");
    }
    int resourceId = appContext.getResources()
        .getIdentifier(
            resourceName,
            "string",
            appContext.getPackageName()
        );
    return appContext.getResources().getString(resourceId);
  }

  // Read the language codes, that are supported in this app from the locales.txt file
  private void getLanguageCodesFromAssets() {
    for (String locale : FileUtils.readLocalesFromAssets(mContext)) {
      if (!locale.isEmpty()) {
        mLocaleLanguageCodes.add(locale.trim());
      }
    }
  }

  // Create a list containing the language code and the corresponding (english) langauge name
  private void setupLanguageList() {

    for (String languageCode : mLocaleLanguageCodes) {
      mLanguageList.add(new LanguageContainer().findLanguageName(languageCode));
    }
  }

  // Sort the language list by the language name
  private void sortLanguageList(Locale locale) {

    Collator localeCollator = Collator.getInstance(locale);
    localeCollator.setStrength(Collator.SECONDARY);

    Collections.sort(mLanguageList,
        (a, b) -> localeCollator.compare(a.getLanguageName(), b.getLanguageName()));
  }

  // Check, if the selected Locale is supported and weather we actually need to change our font.
  // We do this by checking, if our Locale is available in the List, that Locale.getAvailableLocales() returns.
  private boolean haveToChangeFont(SharedPreferenceUtil sharedPreferenceUtil) {

    for (Locale s : Locale.getAvailableLocales()) {
      if (s.getLanguage().equals(Locale.getDefault().toString())) {
        return false;
      }

      // Don't change the language, if the options hasn't been set
      String language = sharedPreferenceUtil.getPrefLanguage("");

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
  public void changeFont(LayoutInflater layoutInflater, SharedPreferenceUtil sharedPreferenceUtil) {

    if (!haveToChangeFont(sharedPreferenceUtil)) {
      return;
    }

    try {
      Field field = LayoutInflater.class.getDeclaredField("mFactorySet");
      field.setAccessible(true);
      field.setBoolean(layoutInflater, false);
      layoutInflater.setFactory(new LayoutInflaterFactory(mContext, layoutInflater));
    } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
      Log.w(TAG_KIWIX, "Font Change Failed: Could not access private field of the LayoutInflater",
          e);
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
          new Handler().post(() -> {
            TextView textView = ((TextView) view);

            // Set the custom typeface
            textView.setTypeface(Typeface.createFromAsset(mContext.getAssets(),
                getTypeface(Locale.getDefault().getLanguage())));
            Log.d(TAG_KIWIX, "Applying custom font");

            // Reduce the text size
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                textView.getTextSize() - 2f);
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
    public LanguageContainer findLanguageName(String languageCode) {
      mLanguageCode = languageCode;

      try {
        mLanguageName = new Locale(languageCode).getDisplayLanguage();

        // Use the English name of the language, if the language name is not
        // available in the current Locale
        if (mLanguageName.length() == 2) {
          mLanguageName = new Locale(languageCode).getDisplayLanguage(new Locale("en"));
        }
        return this;
      } catch (Exception e) {
        mLanguageName = "";
        return this;
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
