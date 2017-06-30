package org.kiwix.kiwixmobile.di.modules;

import org.kiwix.kiwixlib.JNIKiwix;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

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
