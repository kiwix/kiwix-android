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
package org.kiwix.kiwixmobile.di.modules;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import org.kiwix.kiwixlib.JNIKiwix;
import org.kiwix.kiwixlib.JNIKiwixSearcher;

/**
 * Created by mhutti1 on 14/04/17.
 */

@Module public class JNIModule {
  @Provides
  @Singleton
  public JNIKiwix providesJNIKiwix(@NonNull Context context) {
    return new JNIKiwix(context);
  }

  @Provides
  @Singleton
  @Nullable
  public JNIKiwixSearcher providesJNIKiwixSearcher() {
    try {
      return new JNIKiwixSearcher();
    } catch (UnsatisfiedLinkError ignore) {
      return null;
    }
  }
}
