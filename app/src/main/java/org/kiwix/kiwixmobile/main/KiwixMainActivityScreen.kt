/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.main

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.R.drawable
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.main.DrawerMenuGroup
import org.kiwix.kiwixmobile.core.main.LeftDrawerMenu
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.kiwixmobile.ui.KiwixNavGraph

const val BOTTOM_NAV_READER_ITEM_TESTING_TAG = "bottomNavReaderItemTestingTag"
const val BOTTOM_NAV_LIBRARY_ITEM_TESTING_TAG = "bottomNavLibraryItemTestingTag"
const val BOTTOM_NAV_DOWNLOADS_ITEM_TESTING_TAG = "bottomNavDownloadsItemTestingTag"

@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KiwixMainActivityScreen(
  navController: NavHostController,
  leftDrawerContent: List<DrawerMenuGroup>,
  startDestination: String,
  topLevelDestinationsRoute: Set<String>,
  leftDrawerState: DrawerState,
  uiCoroutineScope: CoroutineScope,
  enableLeftDrawer: Boolean,
  shouldShowBottomAppBar: Boolean,
  bottomAppBarScrollBehaviour: BottomAppBarScrollBehavior?
) {
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route
  val shouldShowBottomBar = currentRoute in topLevelDestinationsRoute && shouldShowBottomAppBar
  OnUserBackPressed(leftDrawerState, uiCoroutineScope, currentRoute, navController)
  KiwixTheme {
    ModalNavigationDrawer(
      drawerState = leftDrawerState,
      drawerContent = {
        Column(modifier = Modifier.fillMaxSize()) {
          LeftDrawerMenu(leftDrawerContent)
        }
      },
      gesturesEnabled = enableLeftDrawer &&
        currentRoute in topLevelDestinationsRoute &&
        // Fixing the webView scrolling is lagging when navigation gesture is enabled,
        // since navigation consumes the swipes event makes webView lagging.
        // However, on reader screen navigation drawer can be opened by clicking
        // on the hamburger button.
        (currentRoute != KiwixDestination.Reader.route || leftDrawerState.isOpen)
    ) {
      Scaffold(
        bottomBar = {
          if (shouldShowBottomBar) {
            BottomNavigationBar(
              navController = navController,
              bottomAppBarScrollBehaviour = bottomAppBarScrollBehaviour,
              navBackStackEntry = navBackStackEntry,
              leftDrawerState = leftDrawerState,
              uiCoroutineScope = uiCoroutineScope
            )
          }
        },
        modifier = Modifier
          .fillMaxSize()
          .systemBarsPadding()
      ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
          KiwixNavGraph(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
          )
        }
      }
    }
  }
}

@Composable
private fun OnUserBackPressed(
  leftDrawerState: DrawerState,
  uiCoroutineScope: CoroutineScope,
  currentRoute: String?,
  navController: NavHostController
) {
  val activity = LocalActivity.current
  BackHandler(enabled = true) {
    when {
      leftDrawerState.isOpen -> uiCoroutineScope.launch { leftDrawerState.close() }

      currentRoute == KiwixDestination.Reader.route &&
        navController.previousBackStackEntry?.destination?.route != KiwixDestination.Search.route -> {
        activity?.finish()
      }

      else -> {
        val popped = navController.popBackStack()
        if (!popped) {
          activity?.finish()
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationBar(
  navController: NavHostController,
  bottomAppBarScrollBehaviour: BottomAppBarScrollBehavior?,
  navBackStackEntry: NavBackStackEntry?,
  leftDrawerState: DrawerState,
  uiCoroutineScope: CoroutineScope
) {
  val bottomNavItems = listOf(
    BottomNavItem(
      route = KiwixDestination.Reader.route,
      title = stringResource(id = R.string.reader),
      iconRes = drawable.ic_reader_navigation_white_24px,
      testingTag = BOTTOM_NAV_READER_ITEM_TESTING_TAG
    ),
    BottomNavItem(
      route = KiwixDestination.Library.route,
      title = stringResource(id = R.string.library),
      iconRes = drawable.ic_library_navigation_white_24dp,
      testingTag = BOTTOM_NAV_LIBRARY_ITEM_TESTING_TAG
    ),
    BottomNavItem(
      route = KiwixDestination.Downloads.route,
      title = stringResource(id = R.string.download),
      iconRes = drawable.ic_download_navigation_white_24dp,
      testingTag = BOTTOM_NAV_DOWNLOADS_ITEM_TESTING_TAG
    )
  )
  val currentDestinationRoute = navBackStackEntry?.destination?.route
  BottomAppBar(
    containerColor = MaterialTheme.colorScheme.onPrimary,
    contentColor = White.copy(alpha = 0.5f),
    scrollBehavior = bottomAppBarScrollBehaviour
  ) {
    bottomNavItems.forEach { item ->
      NavigationBarItem(
        selected = currentDestinationRoute == item.route,
        onClick = {
          uiCoroutineScope.launch {
            leftDrawerState.close()
            navController.navigate(item.route) {
              // Avoid multiple copies of the same destination
              launchSingleTop = true

              // Pop up to the start destination of the graph to avoid building up a large stack
              popUpTo(navController.graph.findStartDestination().id) {
                // Bug fix #4392
                saveState = item.route != KiwixDestination.Reader.route
              }

              // Restore state when reselecting a previously selected tab
              restoreState = item.route != KiwixDestination.Reader.route
            }
          }
        },
        icon = {
          Icon(
            painter = painterResource(id = item.iconRes),
            contentDescription = item.title,
            tint = MaterialTheme.colorScheme.onBackground
          )
        },
        label = { Text(item.title, color = MaterialTheme.colorScheme.onBackground) },
        modifier = Modifier.semantics { testTag = item.testingTag },
        colors = NavigationBarItemDefaults.colors()
          .copy(selectedIndicatorColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
      )
    }
  }
}
