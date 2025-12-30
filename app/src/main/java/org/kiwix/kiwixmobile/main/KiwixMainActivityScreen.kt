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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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

@Composable
private fun bottomNavItemList(): List<BottomNavItem> = listOf(
  BottomNavItem(
    route = KiwixDestination.Reader.route,
    title = stringResource(id = R.string.reader),
    selectedIcon = drawable.ic_reader_navigation_white_24px,
    unselectedIcon = drawable.ic_navigation_reader_unfilled,
    testingTag = BOTTOM_NAV_READER_ITEM_TESTING_TAG
  ),
  BottomNavItem(
    route = KiwixDestination.Library.route,
    title = stringResource(id = R.string.library),
    selectedIcon = drawable.ic_library_navigation_white_24dp,
    unselectedIcon = drawable.ic_navigation_library_unfilled,
    testingTag = BOTTOM_NAV_LIBRARY_ITEM_TESTING_TAG
  ),
  BottomNavItem(
    route = KiwixDestination.Downloads.route,
    title = stringResource(id = R.string.download),
    selectedIcon = drawable.ic_download_navigation_white_24dp,
    unselectedIcon = drawable.ic_navigation_download_unfilled,
    testingTag = BOTTOM_NAV_DOWNLOADS_ITEM_TESTING_TAG
  )
)

@Composable
private fun RowScope.BottomNavItemView(
  item: BottomNavItem,
  selected: Boolean,
  navController: NavHostController,
  leftDrawerState: DrawerState,
  uiCoroutineScope: CoroutineScope
) {
  val scale by animateFloatAsState(
    targetValue = if (selected) 1.15f else 1f,
    animationSpec = tween(durationMillis = 200),
    label = "BottomNavBounce"
  )
  val icon = if (selected) item.selectedIcon else item.unselectedIcon

  NavigationBarItem(
    selected = selected,
    onClick = {
      uiCoroutineScope.launch {
        leftDrawerState.close()
        navController.navigate(item.route) {
          launchSingleTop = true
          popUpTo(navController.graph.findStartDestination().id) {
            saveState = item.route != KiwixDestination.Reader.route
          }
          restoreState = item.route != KiwixDestination.Reader.route
        }
      }
    },
    icon = {
      Icon(
        painter = painterResource(icon),
        contentDescription = item.title,
        modifier = Modifier.graphicsLayer {
          scaleX = scale
          scaleY = scale
        }
      )
    },
    label = { Text(item.title) },
    modifier = Modifier.semantics { testTag = item.testingTag }
  )
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
  // Retrieve BottomNav Tabs Here
  val bottomNavItems = bottomNavItemList()
  val currentDestinationRoute = navBackStackEntry?.destination?.route
  BottomAppBar(
    containerColor = MaterialTheme.colorScheme.onPrimary,
    contentColor = White.copy(alpha = 0.5f),
    scrollBehavior = bottomAppBarScrollBehaviour
  ) {
    bottomNavItems.forEach { item ->

      val isSelected = currentDestinationRoute == item.route

      // Helper function to add BottomNav items
      BottomNavItemView(
        item = item,
        selected = isSelected,
        navController = navController,
        leftDrawerState = leftDrawerState,
        uiCoroutineScope = uiCoroutineScope
      )
    }
  }
}
