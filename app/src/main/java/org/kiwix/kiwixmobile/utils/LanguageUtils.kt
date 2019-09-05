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

package org.kiwix.kiwixmobile.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.InflateException
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import org.kiwix.kiwixmobile.utils.Constants.TAG_KIWIX
import org.kiwix.kiwixmobile.utils.files.FileUtils
import java.text.Collator
import java.util.Collections
import java.util.HashMap
import java.util.Locale
import java.util.MissingResourceException

class LanguageUtils(private val mContext: Context) {
  private val mLanguageList: List<LanguageContainer> = sortWithCollator(setupLanguageList())
  private val mLocaleLanguageCodes: List<String> = getLanguageCodesFromAssets())

  private fun sortWithCollator(languageCodesFromAssets: List<LanguageContainer>): List<String> {
    val localeCollator = Collator.getInstance(mContext.resources.configuration.locale)
    localeCollator.strength = Collator.SECONDARY
    Collections.sort(languageCodesFromAssets) { o1, o2 -> localeCollator.compare(o1.languageName, o2) }
  }

  // Get a list of all the language names
  val values: List<String>
    get() = mLanguageList.map(LanguageContainer::languageName)

  // Get a list of all the language codes
  val keys: List<String>
    get() = mLanguageList.map(LanguageContainer::languageCode)

  init {

    sortLanguageList()
  }

  // Read the language codes, that are supported in this app from the locales.txt file
  private fun getLanguageCodesFromAssets() = FileUtils.readLocalesFromAssets(mContext)
    .filterNot(String::isEmpty)
    .map { locale -> locale.trim { it <= ' ' } }

  // Create a list containing the language code and the corresponding (english) langauge name
  private fun setupLanguageList() = mLocaleLanguageCodes.map(::LanguageContainer)

  // Sort the language list by the language name
  private fun sortLanguageList(locale: Locale) {

    Collections.sort(
      mLanguageList
    ) { a, b -> localeCollator.compare(a.languageName, b.languageName) }
  }

  // Check, if the selected Locale is supported and weather we actually need to change our font.
  // We do this by checking, if our Locale is available in the List, that Locale.getAvailableLocales() returns.
  private fun haveToChangeFont(sharedPreferenceUtil: SharedPreferenceUtil): Boolean {

    for (s in Locale.getAvailableLocales()) {
      if (s.language == Locale.getDefault().toString()) {
        return false
      }

      // Don't change the language, if the options hasn't been set
      val language = sharedPreferenceUtil.getPrefLanguage("")

      if (language.isEmpty()) {
        return false
      }
    }
    return true
  }

  // Change the font of all the TextViews and its subclasses in our whole app by attaching a custom
  // Factory to the LayoutInflater of the Activity.
  // The Factory is called on each element name as the xml is parsed and we can therefore modify
  // the parsed Elements.
  // A Factory can only be set once on a LayoutInflater. And since we are using the support Library,
  // which also sets a Factory on the LayoutInflator, we have to access the private field of the
  // LayoutInflater, that handles this restriction via Java's reflection API
  // and make it accessible set it to false again.
  fun changeFont(layoutInflater: LayoutInflater, sharedPreferenceUtil: SharedPreferenceUtil) {

    if (!haveToChangeFont(sharedPreferenceUtil)) {
      return
    }

    try {
      val field = LayoutInflater::class.java.getDeclaredField("mFactorySet")
      field.isAccessible = true
      field.setBoolean(layoutInflater, false)
      layoutInflater.factory = LayoutInflaterFactory(mContext, layoutInflater)
    } catch (e: NoSuchFieldException) {
      Log.w(
        TAG_KIWIX, "Font Change Failed: Could not access private field of the LayoutInflater",
        e
      )
    } catch (e: IllegalAccessException) {
      Log.w(
        TAG_KIWIX,
        "Font Change Failed: Could not access private field of the LayoutInflater",
        e
      )
    } catch (e: IllegalArgumentException) {
      Log.w(
        TAG_KIWIX,
        "Font Change Failed: Could not access private field of the LayoutInflater",
        e
      )
    }
  }

  // That's the Factory, that will handle the manipulation of all our TextView's and its subcalsses
  // while the content is being parsed
  class LayoutInflaterFactory(
    private val mContext: Context,
    private val mLayoutInflater: LayoutInflater
  ) : LayoutInflater.Factory {

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {

      // Apply the custom font, if the xml tag equals "TextView", "EditText" or "AutoCompleteTextView"
      if (name.equals("TextView", ignoreCase = true)
        || name.equals("EditText", ignoreCase = true)
        || name.equals("AutoCompleteTextView", ignoreCase = true)
      ) {

        try {
          val inflater = mLayoutInflater
          val view = inflater.createView(name, null, attrs)
          Handler().post {
            val textView = view as TextView

            // Set the custom typeface
            textView.typeface = Typeface.createFromAsset(
              mContext.assets,
              getTypeface(Locale.getDefault().language)
            )
            Log.d(TAG_KIWIX, "Applying custom font")

            // Reduce the text size
            textView.setTextSize(
              TypedValue.COMPLEX_UNIT_PX,
              textView.textSize - 2f
            )
          }

          return view
        } catch (e: InflateException) {
          Log.w(TAG_KIWIX, "Could not apply the custom font to $name", e)
        } catch (e: ClassNotFoundException) {
          Log.w(TAG_KIWIX, "Could not apply the custom font to $name", e)
        }
      }

      return null
    }
  }

  class LanguageContainer(val languageCode: String, val languageName: String) {
    constructor(languageCode: String) : this(languageCode, chooseLanguageName(languageCode))

    companion object {
      private fun chooseLanguageName(
        languageCode: String
      ): String {
        val displayLanguage = Locale(languageCode).displayLanguage
        return if (displayLanguage.length == 2 || displayLanguage.isEmpty()) {
          Locale.ENGLISH.displayLanguage
        } else {
          displayLanguage
        }
      }
    }
  }

  companion object {

    private var mLocaleMap: HashMap<String, Locale>? = null

    fun handleLocaleChange(
      context: Context,
      sharedPreferenceUtil: SharedPreferenceUtil
    ) {
      val language = sharedPreferenceUtil.getPrefLanguage("")

      if (language.isEmpty()) {
        return
      }

      handleLocaleChange(context, language)
    }

    fun handleLocaleChange(context: Context, language: String) {

      val locale = Locale(language)
      Locale.setDefault(locale)
      val config = Configuration()
      if (Build.VERSION.SDK_INT >= 17) {
        config.setLocale(locale)
        config.setLayoutDirection(locale)
      } else {
        config.locale = locale
      }
      context.resources
        .updateConfiguration(config, context.resources.displayMetrics)
    }

    /**
     * Converts ISO3 language code to [java.util.Locale].
     *
     * @param iso3 ISO3 language code
     * @return [java.util.Locale] that represents the language of the provided code
     */
    fun ISO3ToLocale(iso3: String): Locale? {
      if (mLocaleMap == null) {
        val locales = Locale.getAvailableLocales()
        mLocaleMap = HashMap()
        for (locale in locales) {
          try {
            mLocaleMap!![locale.isO3Language.toUpperCase()] = locale
          } catch (e: MissingResourceException) {
            // Do nothing
          }
        }
      }
      return mLocaleMap!![iso3.toUpperCase()]
    }

    fun getCurrentLocale(context: Context): Locale {
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        context.resources.configuration.locales.get(0)
      } else {

        context.resources.configuration.locale
      }
    }

    // This method will determine which font will be applied to the not-supported-locale.
    // You can define exceptions to the default DejaVu font in the 'exceptions' Hashmap:
    fun getTypeface(languageCode: String): String? {

      // Define the exceptions to the rule. The font has to be placed in the assets folder.
      // Key: the language code; Value: the name of the font.
      val exceptions = HashMap<String, String>()
      exceptions["km"] = "fonts/KhmerOS.ttf"
      exceptions["my"] = "fonts/Parabaik.ttf"
      exceptions["guj"] = "fonts/Lohit-Gujarati.ttf"
      exceptions["ori"] = "fonts/Lohit-Odia.ttf"
      exceptions["pan"] = "fonts/Lohit-Punjabi.ttf"
      exceptions["dzo"] = "fonts/DDC_Uchen.ttf"
      exceptions["bod"] = "fonts/DDC_Uchen.ttf"
      exceptions["sin"] = "fonts/Kaputa-Regular.ttf"

      // http://scriptsource.org/cms/scripts/page.php?item_id=entry_detail&uid=kstzk8hbg4
      // Link above shows that we are allowed to distribute this font
      exceptions["chr"] = "fonts/Digohweli.ttf"

      // These scripts could be supported via more Lohit fonts if DejaVu doesn't
      // support them.  That is untested now as they aren't even in the language
      // menu:
      //  * (no ISO code?) (Devanagari/Nagari) -- at 0% in translatewiki
      //  * mr (Marathi) -- at 21% in translatewiki

      // Check, if an exception applies to our current locale
      return if (exceptions.containsKey(languageCode)) {
        exceptions[languageCode]
      } else "fonts/DejaVuSansCondensed.ttf"

      // Return the default font
    }

    fun getResourceString(appContext: Context, str: String): String {
      var resourceName = str
      if (resourceName.contains("REPLACE_")) {
        resourceName = resourceName.replace("REPLACE_", "")
      }
      val resourceId = appContext.resources
        .getIdentifier(
          resourceName,
          "string",
          appContext.packageName
        )
      return appContext.resources.getString(resourceId)
    }
  }
}
