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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import org.kiwix.kiwixmobile.R.drawable
import org.kiwix.kiwixmobile.R.id
import org.kiwix.kiwixmobile.core.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationBar(
  navController: NavController,
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

@Composable
fun MainNavGraph(
  fragmentManager: FragmentManager,
  navGraphId: Int
) {
  val navController = remember {
    fragmentManager.findNavController(R.id.nav_host_fragment)
  }

  // Drawer states
  val leftDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val rightDrawerVisible = remember { mutableStateOf(false) }

  // Bottom nav destinations
  val bottomNavDestinations = listOf(
    id.readerFragment,
    id.libraryFragment,
    id.downloadsFragment
  )

  // Observe current destination
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestinationId = navBackStackEntry?.destination?.id

  // Coroutine scope for drawer
  val scope = rememberCoroutineScope()

  ModalNavigationDrawer(
    drawerState = leftDrawerState,
    drawerContent = { DrawerContentLeft() }
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      // Fragment content
      FragmentContainer(
        fragmentManager = fragmentManager,
        containerId = R.id.nav_host_fragment,
        navGraphId = navGraphId
      )

      // Right drawer (slide in)
      AnimatedVisibility(
        visible = rightDrawerVisible.value,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it }),
        modifier = Modifier.align(Alignment.CenterEnd)
      ) {
        DrawerContentRight(
          onClose = { rightDrawerVisible.value = false }
        )
      }

      // Bottom nav only on selected destinations
      if (currentDestinationId in bottomNavDestinations) {
        BottomNavigationBar(
          navController = navController,
        )
      }
    }
  }
}
