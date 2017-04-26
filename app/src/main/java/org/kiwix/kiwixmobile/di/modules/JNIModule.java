package org.kiwix.kiwixmobile.di.modules;

import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import org.kiwix.kiwixlib.JNIKiwix;

/**
 * Created by mhutti1 on 14/04/17.
 */

@Module public class JNIModule {
  @Provides
  @Singleton
  public JNIKiwix providesJNIKiwix() {
    return new JNIKiwix();
  }


}
