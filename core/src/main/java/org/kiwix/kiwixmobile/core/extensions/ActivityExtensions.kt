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

package org.kiwix.kiwixmobile.core.extensions

import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavDirections
import org.kiwix.kiwixmobile.core.di.components.CoreActivityComponent
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.REQUEST_POST_NOTIFICATION_PERMISSION
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil

object ActivityExtensions {
  private val Activity.coreMainActivity: CoreMainActivity get() = this as CoreMainActivity

  fun AppCompatActivity.startActionMode(
    menuId: Int,
    idsToClickActions: Map<Int, () -> Any>,
    onDestroyAction: () -> Unit
  ): ActionMode? {
    return startSupportActionMode(object : ActionMode.Callback {
      override fun onActionItemClicked(mode: ActionMode, item: MenuItem) =
        idsToClickActions[item.itemId]?.let {
          it()
          mode.finish()
          true
        } ?: false

      override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean {
        mode.menuInflater.inflate(menuId, menu)
        return true
      }

      override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false

      override fun onDestroyActionMode(mode: ActionMode?) {
        onDestroyAction()
      }
    })
  }

  inline fun <reified T : Activity> Activity.start(
    noinline intentFunc: (Intent.() -> Unit)? = null
  ) {
    startActivity(intent<T> { intentFunc?.invoke(this) })
  }

  inline fun <reified T : Activity> Activity.intent(
    noinline intentFunc: (Intent.() -> Unit)? = null
  ) =
    Intent(this, T::class.java).apply { intentFunc?.invoke(this) }

  inline fun <reified T : ViewModel> FragmentActivity.viewModel(
    viewModelFactory: ViewModelProvider.Factory
  ) =
    ViewModelProviders.of(this, viewModelFactory)
      .get(T::class.java)

  fun Activity.navigate(action: NavDirections) {
    coreMainActivity.navigate(action)
  }

  val Activity.cachedComponent: CoreActivityComponent
    get() = coreMainActivity.cachedComponent

  fun Activity.setupDrawerToggle(shouldEnableRightDrawer: Boolean = false) =
    coreMainActivity.setupDrawerToggle(shouldEnableRightDrawer)

  fun Activity.navigate(route: String) {
    coreMainActivity.navigate(route)
  }

  fun Activity.navigate(fragmentId: Int, bundle: Bundle) {
    coreMainActivity.navigate(fragmentId, bundle)
  }

  fun Activity.popNavigationBackstack() {
    coreMainActivity.navController.popBackStack()
  }

  private fun <T> Activity.getObservableNavigationResult(key: String = "result") =
    coreMainActivity.navController.currentBackStackEntry?.savedStateHandle
      ?.getLiveData<T>(key)

  fun <T> Activity.observeNavigationResult(
    key: String,
    owner: LifecycleOwner,
    observer: Observer<T>
  ) {
    getObservableNavigationResult<T>(key)?.observe(owner) {
      observer.onChanged(it)
      coreMainActivity.consumeObservable<T>(key)
    }
  }

  fun <T> Activity.consumeObservable(key: String = "result") =
    coreMainActivity.navController.currentBackStackEntry?.savedStateHandle?.remove<T>(key)

  fun <T> Activity.setNavigationResult(result: T, key: String = "result") {
    coreMainActivity.navController.previousBackStackEntry?.savedStateHandle?.set(
      key,
      result
    )
  }

  fun <T> Activity.setNavigationResultOnCurrent(result: T, key: String = "result") {
    coreMainActivity.navController.currentBackStackEntry?.savedStateHandle?.set(
      key,
      result
    )
  }

  fun Activity.hasNotificationPermission(sharedPreferenceUtil: SharedPreferenceUtil?) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      sharedPreferenceUtil?.prefIsTest == false
    ) {
      ContextCompat.checkSelfPermission(
        this,
        POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }

  fun Activity.requestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ActivityCompat.requestPermissions(
        this,
        arrayOf(POST_NOTIFICATIONS),
        REQUEST_POST_NOTIFICATION_PERMISSION
      )
    }
  }

  /**
   * Checks if the package name of the current activity's application is not equal to
   * 'org.kiwix.kiwixmobile' or 'org.kiwix.kiwixmobile.standalone',
   * indicating that it is a custom application.
   */
  fun Activity.isCustomApp(): Boolean =
    packageName != "org.kiwix.kiwixmobile" && packageName != "org.kiwix.kiwixmobile.standalone"

  @SuppressLint("NewApi")
  fun Activity.isManageExternalStoragePermissionGranted(
    sharedPreferenceUtil: SharedPreferenceUtil?
  ): Boolean =
    if (sharedPreferenceUtil?.isNotPlayStoreBuildWithAndroid11OrAbove() == true &&
      !sharedPreferenceUtil.prefIsTest &&
      sharedPreferenceUtil.manageExternalFilesPermissionDialogOnRefresh
    ) {
      Environment.isExternalStorageManager()
    } else {
      true
    }

  fun Activity.isLandScapeMode(): Boolean =
    resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

  @Suppress("MagicNumber")
  fun Activity.isTablet(): Boolean {
    val configuration = resources.configuration
    val isLargeOrXLarge =
      configuration.screenLayout and
        Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE
    val isWideEnough = configuration.smallestScreenWidthDp >= 600
    return isLargeOrXLarge && isWideEnough
  }

  /**
   * Sets the window background color to black for Android 15 and above.
   *
   * In Android 15, the `setStatusBarColor` method is deprecated and no longer functional.
   * As a workaround, this method sets the window background color to black because the
   * status bar and navigation bar now inherit the background color of the window.
   *
   * @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
   */
  @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
  fun Activity.setWindowBackgroundColorForAndroid15AndAbove() {
    window.decorView.setBackgroundColor(Color.BLACK)
  }
}
