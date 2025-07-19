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
import org.kiwix.kiwixmobile.R.id
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.main.DrawerMenuGroup
import org.kiwix.kiwixmobile.core.main.LeftDrawerMenu
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.NAVIGATION_DRAWER_WIDTH
import org.kiwix.kiwixmobile.ui.KiwixNavGraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KiwixMainActivityScreen(
  navController: NavHostController,
  topLevelDestinations: List<Int>,
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
        Column(
          Modifier
            .fillMaxHeight()
            .width(NAVIGATION_DRAWER_WIDTH)
        ) {
          LeftDrawerMenu(leftDrawerContent)
        }
      },
      gesturesEnabled = true
    ) {
      Box {
        Scaffold(
          bottomBar = {
            if (isBottomBarVisible) {
              BottomNavigationBar(
                navController = navController,
                scrollBehavior = scrollingBehavior,
                topLevelDestinations = topLevelDestinations
              )
            }
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
  scrollBehavior: BottomAppBarScrollBehavior,
  topLevelDestinations: List<Int>
) {
  val bottomNavItems = listOf(
    BottomNavItem(
      id = id.readerFragment,
      title = stringResource(id = R.string.reader),
      iconRes = drawable.ic_reader_navigation_white_24px
    ),
    BottomNavItem(
      id = id.libraryFragment,
      title = stringResource(id = R.string.library),
      iconRes = drawable.ic_library_navigation_white_24dp
    ),
    BottomNavItem(
      id = id.downloadsFragment,
      title = stringResource(id = R.string.download),
      iconRes = drawable.ic_download_navigation_white_24dp
    )
  )

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestinationId = navBackStackEntry?.destination?.id

  if (currentDestinationId in topLevelDestinations) {
    BottomAppBar(scrollBehavior = scrollBehavior) {
      bottomNavItems.forEach { item ->
        NavigationBarItem(
          selected = currentDestinationId == item.id,
          onClick = { navController.navigate(item.id) },
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
}
