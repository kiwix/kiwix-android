/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.common.di.components;

import org.kiwix.kiwixmobile.common.di.modules.ApplicationModule;
import org.kiwix.kiwixmobile.common.di.modules.JNIModule;
import org.kiwix.kiwixmobile.common.di.modules.TestNetworkModule;
import org.kiwix.kiwixmobile.tests.NetworkTest;
import org.kiwix.kiwixmobile.tests.ZimTest;
import org.kiwix.kiwixmobile.common.utils.TestNetworkInterceptor;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by mhutti1 on 13/04/17.
 */

@Singleton
@Component(modules = {
    ApplicationModule.class,
    TestNetworkModule.class,
    JNIModule.class,
})
public interface TestComponent extends ApplicationComponent {

  void inject(ZimTest zimTest);

  void inject(NetworkTest networkTest);

  void inject(TestNetworkInterceptor testNetworkInterceptor);
}
