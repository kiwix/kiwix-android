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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import org.kiwix.kiwixmobile.core.extensions.locale
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
    languageCodesFromAssets.sortWith(
      Comparator { o1, o2 ->
        localeCollator.compare(
          o1.languageName,
          o2.languageName
        )
      }
    )
    return languageCodesFromAssets
  }

  private fun haveToChangeFont(sharedPreferenceUtil: SharedPreferenceUtil): Boolean {
    if (sharedPreferenceUtil.prefLanguage == Locale.ROOT.toString()) {
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
  fun changeFont(
    activity: Activity,
    sharedPreferenceUtil: SharedPreferenceUtil
  ) {

    if (!haveToChangeFont(sharedPreferenceUtil)) {
      return
    }

    setTypeFace(activity.window.decorView as ViewGroup, activity)
  }

  private fun setTypeFace(viewGroup: ViewGroup, activity: Activity) {
    for (i in 0 until viewGroup.childCount) {
      val child = viewGroup.getChildAt(i)
      if (child is ViewGroup) {
        setTypeFace(child, activity)
        continue
      }
      if (child is TextView) {
        setTyfaceToTextView(child, activity)
      }
    }
  }

  private fun setTyfaceToTextView(child: TextView, activity: Activity) {
    Handler(Looper.getMainLooper()).post {
      child.apply {
        setTypeface(
          Typeface.createFromAsset(
            activity.assets,
            getTypeface(Locale.getDefault().language)
          ),
          Typeface.NORMAL
        )
        setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize - 2f)
      }
    }
  }

  companion object {

    private var isO3LanguageToLocaleMap: Map<String, Locale> =
      Locale.getAvailableLocales().associateBy {
        try {
          it.isO3Language.uppercase(Locale.ROOT)
        } catch (ignore: MissingResourceException) {
          it.language.uppercase(Locale.ROOT)
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
      activity: Activity,
      sharedPreferenceUtil: SharedPreferenceUtil
    ) {
      sharedPreferenceUtil.prefLanguage.takeIf { it != Locale.ROOT.toString() }?.let {
        handleLocaleChange(activity, it, sharedPreferenceUtil)
      }
    }

    @SuppressLint("AppBundleLocaleChanges")
    @Suppress("DEPRECATION")
    @JvmStatic
    fun handleLocaleChange(
      activity: Activity,
      language: String,
      sharedPreferenceUtil: SharedPreferenceUtil
    ) {
      val locale =
        if (language == Locale.ROOT.toString())
          Locale(sharedPreferenceUtil.prefDeviceDefaultLanguage)
        else
          Locale(language)
      Locale.setDefault(locale)
      val config = Configuration()
      config.setLocale(locale)
      config.setLayoutDirection(locale)
      activity.resources
        .updateConfiguration(config, activity.resources.displayMetrics).also {
          activity.applicationContext.resources
            .updateConfiguration(config, activity.resources.displayMetrics)
        }
      activity.onConfigurationChanged(config)
    }

    /**
     * Converts ISO3 language code to [java.util.Locale].
     *
     * @param iso3 ISO3 language code
     * @return [java.util.Locale] that represents the language of the provided code
     */
    @JvmStatic
    fun iSO3ToLocale(iso3: String?): Locale? =
      iso3?.let { isO3LanguageToLocaleMap[it.uppercase(Locale.ROOT)] }

    @JvmStatic
    fun getCurrentLocale(context: Context) = context.locale

    @JvmStatic
    fun getTypeface(languageCode: String) =
      fontExceptions[languageCode] ?: "fonts/DejaVuSansCondensed.ttf"
  }
}
