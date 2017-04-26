package org.kiwix.kiwixmobile.di.components;

import android.support.test.espresso.core.deps.dagger.Module;
import dagger.Component;
import javax.inject.Singleton;
import org.kiwix.kiwixmobile.di.modules.ApplicationModule;
import org.kiwix.kiwixmobile.di.modules.NetworkModule;
import org.kiwix.kiwixmobile.di.modules.TestModule;
import org.kiwix.kiwixmobile.utils.ZimTest;

/**
 * Created by mhutti1 on 13/04/17.
 */

@Singleton
@Component(modules = {
    ApplicationModule.class,
    NetworkModule.class,
    TestModule.class,
})
public interface TestComponent extends ApplicationComponent {

  void inject(ZimTest zimTest);
}
