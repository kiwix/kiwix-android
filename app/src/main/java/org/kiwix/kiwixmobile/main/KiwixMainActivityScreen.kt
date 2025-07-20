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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import org.kiwix.kiwixmobile.R.drawable
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.main.DrawerMenuGroup
import org.kiwix.kiwixmobile.core.main.LeftDrawerMenu
import org.kiwix.kiwixmobile.core.ui.theme.Black
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.NAVIGATION_DRAWER_WIDTH
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.kiwixmobile.ui.KiwixNavGraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KiwixMainActivityScreen(
  navController: NavHostController,
  leftDrawerContent: List<DrawerMenuGroup>,
  rightDrawerContent: @Composable ColumnScope.() -> Unit,
  isBottomBarVisible: Boolean = true
) {
  val rightDrawerState = rememberDrawerState(DrawerValue.Closed)
  val coroutineScope = rememberCoroutineScope()
  val scrollingBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
  KiwixTheme {
    ModalNavigationDrawer(
      drawerContent = {
        Column(modifier = Modifier.fillMaxSize()) {
          LeftDrawerMenu(leftDrawerContent)
        }
      }
    ) {
      Box {
        Scaffold(
          bottomBar = {
            // if (isBottomBarVisible) {
            BottomNavigationBar(
              navController = navController,
              scrollBehavior = scrollingBehavior
            )
            // }
          }
        ) { paddingValues ->
          Box(modifier = Modifier.padding(paddingValues)) {
            KiwixNavGraph(
              navController = navController,
              modifier = Modifier.fillMaxSize()
            )
          }
        }

        // Right drawer overlay
        ModalDrawerSheet(
          drawerState = rightDrawerState,
          modifier = Modifier
            .fillMaxHeight()
            .align(Alignment.CenterEnd)
            .width(NAVIGATION_DRAWER_WIDTH)
        ) {
          rightDrawerContent()
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationBar(
  navController: NavHostController,
  scrollBehavior: BottomAppBarScrollBehavior
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

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestinationRoute = navBackStackEntry?.destination?.route
  BottomAppBar(
    containerColor = Black,
    contentColor = White,
    scrollBehavior = scrollBehavior
  ) {
    bottomNavItems.forEach { item ->
      NavigationBarItem(
        selected = currentDestinationRoute == item.route,
        onClick = { navController.navigate(item.route) },
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
