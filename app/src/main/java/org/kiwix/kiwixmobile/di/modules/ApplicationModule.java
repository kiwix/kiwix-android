package org.kiwix.kiwixmobile.di.modules;

import android.app.NotificationManager;
import android.content.Context;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import org.kiwix.kiwixlib.JNIKiwix;
import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.utils.BookUtils;

@Module public class ApplicationModule {
  private final KiwixApplication application;

  public ApplicationModule(KiwixApplication application) {
    this.application = application;
  }

  @Provides @Singleton Context provideApplicationContext() {
    return this.application;
  }

  @Provides @Singleton NotificationManager provideNotificationManager(Context context){
    return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
  }

  @Provides @Singleton
  BookUtils provideBookUtils() {
    return new BookUtils();
  }
}
