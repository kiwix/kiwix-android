/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
 *
 */

package org.kiwix.kiwixmobile.core.utils

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
import org.kiwix.kiwixmobile.core.extensions.locale
import org.kiwix.kiwixmobile.core.utils.Constants.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import java.text.Collator
import java.util.Locale
import java.util.MissingResourceException

class LanguageUtils(private val context: Context) {
  private val localeLanguageCodes: List<String> = languageCodesFromAssets()
  private val languageList: List<LanguageContainer> = languageContainersFrom(localeLanguageCodes)
  val keys = languageList.map(LanguageContainer::languageCode)

  private fun languageContainersFrom(languageCodes: List<String>) =
    sortWithCollator(languageCodes.map(::LanguageContainer).toMutableList())

  private fun languageCodesFromAssets(): List<String> {
    return FileUtils.readLocalesFromAssets(context)
      .filter(String::isNotEmpty)
      .map { locale -> locale.trim { it <= ' ' } }
  }

  private fun sortWithCollator(languageCodesFromAssets: MutableList<LanguageContainer>):
    MutableList<LanguageContainer> {
    val localeCollator =
      Collator.getInstance(context.locale).apply { strength = Collator.SECONDARY }
    languageCodesFromAssets.sortWith(Comparator { o1, o2 ->
      localeCollator.compare(
        o1.languageName,
        o2.languageName
      )
    })
    return languageCodesFromAssets
  }

  private fun haveToChangeFont(sharedPreferenceUtil: SharedPreferenceUtil): Boolean {
    if (sharedPreferenceUtil.getPrefLanguage("").isEmpty()) {
      return false
    }
    return Locale.getAvailableLocales().firstOrNull { locale ->
      locale.language == Locale.getDefault().toString()
    } == null
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
      layoutInflater.factory = LayoutInflaterFactory(
        context,
        layoutInflater
      )
    } catch (e: NoSuchFieldException) {
      Log.w(
        TAG_KIWIX,
        "Font Change Failed: Could not access private field of the LayoutInflater",
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

    @SuppressWarnings("ImplicitSamInstance")
    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {

      // Apply the custom font, if the xml equals "TextView", "EditText" or "AutoCompleteTextView"
      if (name.equals("TextView", ignoreCase = true) ||
        name.equals("EditText", ignoreCase = true) ||
        name.equals("AutoCompleteTextView", ignoreCase = true)
      ) {
        try {
          val view = mLayoutInflater.createView(name, null, attrs)
          Handler().post {
            (view as TextView).apply {
              typeface = Typeface.createFromAsset(
                mContext.assets,
                getTypeface(Locale.getDefault().language)
              )
              setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize - 2f)
            }
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

  companion object {

    private var isO3LanguageToLocaleMap: Map<String, Locale> =
      Locale.getAvailableLocales().associateBy {
        try {
          it.isO3Language.toUpperCase(Locale.ROOT)
        } catch (ignore: MissingResourceException) {
          it.language.toUpperCase(Locale.ROOT)
        }
      }

    private var fontExceptions = mapOf(
      "km" to "fonts/KhmerOS.ttf",
      "my" to "fonts/Parabaik.ttf",
      "guj" to "fonts/Lohit-Gujarati.ttf",
      "ori" to "fonts/Lohit-Odia.ttf",
      "pan" to "fonts/Lohit-Punjabi.ttf",
      "dzo" to "fonts/DDC_Uchen.ttf",
      "bod" to "fonts/DDC_Uchen.ttf",
      "sin" to "fonts/Kaputa-Regular.ttf",
      // http://scriptsource.org/cms/scripts/page.php?item_id=entry_detail&uid=kstzk8hbg4
      // Link above shows that we are allowed to distribute this font
      "chr" to "fonts/Digohweli.ttf"
    )

    @JvmStatic
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

    @JvmStatic
    fun handleLocaleChange(context: Context, language: String) {
      val locale = Locale(language)
      Locale.setDefault(locale)
      val config = Configuration()
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
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
    @JvmStatic
    fun iSO3ToLocale(iso3: String?): Locale? =
      iso3?.let { isO3LanguageToLocaleMap[it.toUpperCase(Locale.ROOT)] }

    @JvmStatic
    fun getCurrentLocale(context: Context) = context.locale

    @JvmStatic
    fun getTypeface(languageCode: String) =
      fontExceptions[languageCode] ?: "fonts/DejaVuSansCondensed.ttf"

    @JvmStatic
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
