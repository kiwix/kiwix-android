package org.kiwix.kiwixmobile.di.components;

import dagger.Component;
import javax.inject.Singleton;
import org.kiwix.kiwixmobile.di.modules.ApplicationModule;
import org.kiwix.kiwixmobile.di.modules.TestJNIModule;
import org.kiwix.kiwixmobile.di.modules.TestNetworkModule;
import org.kiwix.kiwixmobile.tests.NetworkTest;
import org.kiwix.kiwixmobile.utils.TestNetworkInterceptor;
import org.kiwix.kiwixmobile.tests.ZimTest;

/**
 * Created by mhutti1 on 13/04/17.
 */

@Singleton
@Component(modules = {
    ApplicationModule.class,
    TestNetworkModule.class,
    TestJNIModule.class,
})
public interface TestComponent extends ApplicationComponent {

  void inject(ZimTest zimTest);

  void inject(NetworkTest networkTest);

  void inject(TestNetworkInterceptor testNetworkInterceptor);
}
