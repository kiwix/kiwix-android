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
package org.kiwix.kiwixmobile.di.components;

import android.content.Context;
import dagger.BindsInstance;
import dagger.Component;
import javax.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.data.DataModule;
import org.kiwix.kiwixmobile.data.ZimContentProvider;
import org.kiwix.kiwixmobile.di.modules.ApplicationModule;
import org.kiwix.kiwixmobile.di.modules.JNIModule;
import org.kiwix.kiwixmobile.di.modules.NetworkModule;
import org.kiwix.kiwixmobile.downloader.DownloadService;
import org.kiwix.kiwixmobile.language.LanguageActivity;
import org.kiwix.kiwixmobile.main.KiwixWebView;
import org.kiwix.kiwixmobile.search.AutoCompleteAdapter;
import org.kiwix.kiwixmobile.settings.KiwixSettingsActivity;
import org.kiwix.kiwixmobile.zim_manager.DownloadNotificationClickedReceiver;
import org.kiwix.kiwixmobile.zim_manager.ZimManageActivity;

@Singleton
@Component(modules = {
    ApplicationModule.class,
    NetworkModule.class,
    JNIModule.class,
    DataModule.class
})
public interface ApplicationComponent {

  @Component.Builder
  interface Builder {
    @BindsInstance Builder context(Context context);

    ApplicationComponent build();
  }

  ActivityComponent.Builder activityComponent();

  void inject(KiwixApplication application);

  void inject(DownloadService service);

  void inject(ZimContentProvider zimContentProvider);

  void inject(KiwixWebView kiwixWebView);

  void inject(KiwixSettingsActivity.PrefsFragment prefsFragment);

  void inject(AutoCompleteAdapter autoCompleteAdapter);

  void inject(DownloadNotificationClickedReceiver downloadNotificationClickedReceiver);

  void inject(@NotNull ZimManageActivity zimManageActivity);

  void inject(@NotNull LanguageActivity languageActivity);
}
