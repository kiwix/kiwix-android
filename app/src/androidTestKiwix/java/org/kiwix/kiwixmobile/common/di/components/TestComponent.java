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
