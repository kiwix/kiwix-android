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
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package org.kiwix.kiwixmobile.core.base

class BaseContract {
  /**
   * The contract for a view must extend this interface.
   *
   * @param <T> the type of presenter associated with the view
   */
  interface View<T>
  interface Presenter<in T> {
    /**
     * Binds presenter with a view when resumed. The Presenter will perform initialization here.
     *
     * @param view the view associated with this presenter
     */
    fun attachView(view: T)

    /**
     * Drops the reference to the view when destroyed
     */
    fun detachView()
  }
}
