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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.R.drawable
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.main.DrawerMenuGroup
import org.kiwix.kiwixmobile.core.main.LeftDrawerMenu
import org.kiwix.kiwixmobile.core.ui.theme.Black
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.kiwixmobile.ui.KiwixNavGraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KiwixMainActivityScreen(
  navController: NavHostController,
  leftDrawerContent: List<DrawerMenuGroup>,
  topLevelDestinationsRoute: Set<String>,
  leftDrawerState: DrawerState,
  uiCoroutineScope: CoroutineScope,
  enableLeftDrawer: Boolean,
  shouldShowBottomAppBar: Boolean,
  bottomAppBarScrollBehaviour: BottomAppBarScrollBehavior
) {
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  KiwixTheme {
    ModalNavigationDrawer(
      drawerState = leftDrawerState,
      drawerContent = {
        Column(modifier = Modifier.fillMaxSize()) {
          LeftDrawerMenu(leftDrawerContent)
        }
      },
      gesturesEnabled = enableLeftDrawer && navBackStackEntry?.destination?.route in topLevelDestinationsRoute
    ) {
      Box {
        Scaffold(
          bottomBar = {
            if (navBackStackEntry?.destination?.route in topLevelDestinationsRoute && shouldShowBottomAppBar) {
              BottomNavigationBar(
                navController = navController,
                bottomAppBarScrollBehaviour = bottomAppBarScrollBehaviour,
                navBackStackEntry = navBackStackEntry,
                leftDrawerState = leftDrawerState,
                uiCoroutineScope = uiCoroutineScope
              )
            }
          },
          modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
          Box(modifier = Modifier.padding(paddingValues)) {
            KiwixNavGraph(
              navController = navController,
              modifier = Modifier.fillMaxSize()
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationBar(
  navController: NavHostController,
  bottomAppBarScrollBehaviour: BottomAppBarScrollBehavior,
  navBackStackEntry: NavBackStackEntry?,
  leftDrawerState: DrawerState,
  uiCoroutineScope: CoroutineScope
) {
  val bottomNavItems = listOf(
    BottomNavItem(
      route = KiwixDestination.Reader.route,
      title = stringResource(id = R.string.reader),
      iconRes = drawable.ic_reader_navigation_white_24px
    ),
    BottomNavItem(
      route = KiwixDestination.Library.route,
      title = stringResource(id = R.string.library),
      iconRes = drawable.ic_library_navigation_white_24dp
    ),
    BottomNavItem(
      route = KiwixDestination.Downloads.route,
      title = stringResource(id = R.string.download),
      iconRes = drawable.ic_download_navigation_white_24dp
    )
  )
  val currentDestinationRoute = navBackStackEntry?.destination?.route
  BottomAppBar(
    containerColor = Black,
    contentColor = White,
    scrollBehavior = bottomAppBarScrollBehaviour
  ) {
    bottomNavItems.forEach { item ->
      NavigationBarItem(
        selected = currentDestinationRoute == item.route,
        onClick = {
          // Do not load again fragment that is already loaded.
          if (item.route == currentDestinationRoute) return@NavigationBarItem
          uiCoroutineScope.launch {
            leftDrawerState.close()
            navController.navigate(item.route)
          }
        },
        icon = {
          Icon(
            painter = painterResource(id = item.iconRes),
            contentDescription = item.title
          )
        },
        label = { Text(item.title) }
      )
    }
  }
}
