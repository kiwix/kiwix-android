package org.kiwix.kiwixmobile.main2;

import android.content.Context;
import android.content.Intent;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.OnApplyWindowInsetsListener;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.base.BaseActivity;
import org.kiwix.kiwixmobile.settings.KiwixSettingsActivity;

import butterknife.BindView;
import de.mrapp.android.tabswitcher.AddTabButtonListener;
import de.mrapp.android.tabswitcher.Animation;
import de.mrapp.android.tabswitcher.Layout;
import de.mrapp.android.tabswitcher.PeekAnimation;
import de.mrapp.android.tabswitcher.PullDownGesture;
import de.mrapp.android.tabswitcher.RevealAnimation;
import de.mrapp.android.tabswitcher.SwipeGesture;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.TabSwitcherDecorator;
import de.mrapp.android.tabswitcher.TabSwitcherListener;
import de.mrapp.android.util.ThemeUtil;

import static de.mrapp.android.util.DisplayUtil.getDisplayWidth;

/**
 * This activity hosts the tabs.
 */

public class MainActivity extends BaseActivity implements TabSwitcherListener {

  @BindView(R.id.toolbar)
  Toolbar toolbar;
  @BindView(R.id.tab_switcher)
  TabSwitcher tabSwitcher;

  private DrawerLayout drawer;
  private Snackbar snackbar;

  @Override
  protected final void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    drawer = findViewById(R.id.drawer_layout);
    Decorator decorator = new Decorator();
    tabSwitcher.clearSavedStatesWhenRemovingTabs(false);
    ViewCompat.setOnApplyWindowInsetsListener(tabSwitcher, createWindowInsetsListener());
    tabSwitcher.setDecorator(decorator);
    tabSwitcher.addListener(this);
    tabSwitcher.showToolbars(true);
    tabSwitcher.addTab(createTab(0));
    tabSwitcher.showAddTabButton(createAddTabButtonListener());
    tabSwitcher
        .setToolbarNavigationIcon(R.drawable.ic_add_box_white_32dp, createAddTabListener());
    TabSwitcher.setupWithMenu(tabSwitcher, createTabSwitcherButtonListener());

    toolbar.inflateMenu(R.menu.menu_main);
    toolbar.setOnMenuItemClickListener(createToolbarMenuListener());
    Menu menu = toolbar.getMenu();
    TabSwitcher.setupWithMenu(tabSwitcher, menu, createTabSwitcherButtonListener());

    inflateMenu();
  }

  @Override
  public void onBackPressed() {
    if (drawer.isDrawerOpen(GravityCompat.END)) {
      drawer.closeDrawer(GravityCompat.END);
      return;
    }
    super.onBackPressed();

  }

  /**
   * Creates a listener, which allows to apply the window insets to the tab switcher's padding.
   *
   * @return The listener, which has been created, as an instance of the type {@link
   * OnApplyWindowInsetsListener}. The listener may not be nullFG
   */
  @NonNull
  private OnApplyWindowInsetsListener createWindowInsetsListener() {
    return (v, insets) -> {
      int left = insets.getSystemWindowInsetLeft();
      int top = insets.getSystemWindowInsetTop();
      int right = insets.getSystemWindowInsetRight();
      int bottom = insets.getSystemWindowInsetBottom();
      tabSwitcher.setPadding(left, top, right, bottom);
      float touchableAreaTop = top;

      if (tabSwitcher.getLayout() == Layout.TABLET) {
        touchableAreaTop += getResources()
            .getDimensionPixelSize(R.dimen.tablet_tab_container_height);
      }

      RectF touchableArea = new RectF(left, touchableAreaTop,
          getDisplayWidth(MainActivity.this) - right, touchableAreaTop +
          ThemeUtil.getDimensionPixelSize(MainActivity.this, R.attr.actionBarSize));
      tabSwitcher.addDragGesture(
          new SwipeGesture.Builder().setTouchableArea(touchableArea).create());
      tabSwitcher.addDragGesture(
          new PullDownGesture.Builder().setTouchableArea(touchableArea).create());
      return insets;
    };
  }

  /**
   * Creates and returns a listener, which allows to add a tab to the activity's tab switcher,
   * when a button is clicked.
   *
   * @return The listener, which has been created, as an instance of the type {@link
   * View.OnClickListener}. The listener may not be null
   */
  @NonNull
  private View.OnClickListener createAddTabListener() {
    return view -> {
      int index = tabSwitcher.getCount();
      Animation animation = createRevealAnimation();
      tabSwitcher.addTab(createTab(index), 0, animation);
    };
  }

  /**
   * Creates and returns a listener, which allows to observe, when an item of the tab switcher's
   * toolbar has been clicked.
   *
   * @return The listener, which has been created, as an instance of the type {@link
   * Toolbar.OnMenuItemClickListener}. The listener may not be null
   */
  @NonNull
  private Toolbar.OnMenuItemClickListener createToolbarMenuListener() {
    return item -> {
      switch (item.getItemId()) {
        case R.id.add_tab_menu_item:
          int index = tabSwitcher.getCount();
          Tab tab = createTab(index);
          if (tabSwitcher.isSwitcherShown()) {
            tabSwitcher.addTab(tab, 0, createRevealAnimation());
          } else {
            tabSwitcher.addTab(tab, 0, createPeekAnimation());
          }
          return true;

        case R.id.clear_tabs_menu_item:
          tabSwitcher.clear();
          return true;

        case R.id.settings_menu_item:
        case R.id.menu_settings:
          Intent intent = new Intent(MainActivity.this, KiwixSettingsActivity.class);
          startActivity(intent);
          return true;

        default:
          return false;
      }
    };
  }

  /**
   * Inflates the tab switcher's menu.
   */
  private void inflateMenu() {
    tabSwitcher.inflateToolbarMenu(R.menu.tab_switcher, createToolbarMenuListener());
  }

  /**
   * Creates and returns a listener, which allows to toggle the visibility of the tab switcher,
   * when a button is clicked.
   *
   * @return The listener, which has been created, as an instance of the type {@link
   * View.OnClickListener}. The listener may not be null
   */
  @NonNull
  private View.OnClickListener createTabSwitcherButtonListener() {
    return view -> tabSwitcher.toggleSwitcherVisibility();
  }

  /**
   * Creates and returns a listener, which allows to add a new tab to the tab switcher, when the
   * corresponding button is clicked.
   *
   * @return The listener, which has been created, as an instance of the type {@link
   * AddTabButtonListener}. The listener may not be null
   */
  @NonNull
  private AddTabButtonListener createAddTabButtonListener() {
    return tabSwitcher -> {
      int index = tabSwitcher.getCount();
      Tab tab = createTab(index);
      tabSwitcher.addTab(tab, 0);
    };
  }

  /**
   * Creates and returns a listener, which allows to undo the removal of tabs from the tab
   * switcher, when the button of the activity's snackbar is clicked.
   *
   * @param snackbar The activity's snackbar as an instance of the class {@link Snackbar}. The snackbar
   *                 may not be null
   * @param index    The index of the first tab, which has been removed, as an {@link Integer} value
   * @param tabs     An array, which contains the tabs, which have been removed, as an array of the type
   *                 {@link Tab}. The array may not be null
   * @return The listener, which has been created, as an instance of the type {@link
   * View.OnClickListener}. The listener may not be null
   */
  @NonNull
  private View.OnClickListener createUndoSnackbarListener(@NonNull final Snackbar snackbar,
                                                          final int index,
                                                          @NonNull final Tab... tabs) {
    return view -> {
      snackbar.setAction(null, null);

      if (tabSwitcher.isSwitcherShown()) {
        tabSwitcher.addAllTabs(tabs, index);
      } else if (tabs.length == 1) {
        tabSwitcher.addTab(tabs[0], 0, createPeekAnimation());
      }

    };
  }

  /**
   * Creates and returns a callback, which allows to observe, when a snackbar, which allows to
   * undo the removal of tabs, has been dismissed.
   *
   * @param tabs An array, which contains the tabs, which have been removed, as an array of the type
   *             {@link Tab}. The tab may not be null
   * @return The callback, which has been created, as an instance of the type class {@link
   * BaseTransientBottomBar.BaseCallback}. The callback may not be null
   */
  @NonNull
  private BaseTransientBottomBar.BaseCallback<Snackbar> createUndoSnackbarCallback(
      final Tab... tabs) {
    return new BaseTransientBottomBar.BaseCallback<Snackbar>() {

      @Override
      public void onDismissed(final Snackbar snackbar, final int event) {
        if (event != DISMISS_EVENT_ACTION) {
          for (Tab tab : tabs) {
            tabSwitcher.clearSavedState(tab);
          }
        }
      }
    };
  }

  /**
   * Shows a snackbar, which allows to undo the removal of tabs from the activity's tab switcher.
   *
   * @param text  The text of the snackbar as an instance of the type {@link CharSequence}. The text
   *              may not be null
   * @param index The index of the first tab, which has been removed, as an {@link Integer} value
   * @param tabs  An array, which contains the tabs, which have been removed, as an array of the type
   *              {@link Tab}. The array may not be null
   */
  private void showUndoSnackbar(@NonNull final CharSequence text, final int index,
                                @NonNull final Tab... tabs) {
    snackbar = Snackbar.make(tabSwitcher, text, Snackbar.LENGTH_LONG).setActionTextColor(
        ContextCompat.getColor(this, R.color.accent));
    snackbar.setAction(R.string.undo, createUndoSnackbarListener(snackbar, index, tabs));
    snackbar.addCallback(createUndoSnackbarCallback(tabs));
    snackbar.show();
  }

  /**
   * Creates a reveal animation, which can be used to add a tab to the activity's tab switcher.
   *
   * @return The reveal animation, which has been created, as an instance of the class {@link
   * Animation}. The animation may not be null
   */
  @NonNull
  private Animation createRevealAnimation() {
    float x = 0;
    float y = 0;
    View view = getNavigationMenuItem();

    if (view != null) {
      int[] location = new int[2];
      view.getLocationInWindow(location);
      x = location[0] + (view.getWidth() / 2f);
      y = location[1] + (view.getHeight() / 2f);
    }

    return new RevealAnimation.Builder().setX(x).setY(y).create();
  }

  /**
   * Creates a peek animation, which can be used to add a tab to the activity's tab switcher.
   *
   * @return The peek animation, which has been created, as an instance of the class {@link
   * Animation}. The animation may not be null
   */
  @NonNull
  private Animation createPeekAnimation() {
    return new PeekAnimation.Builder().setX(tabSwitcher.getWidth() / 2f).create();
  }

  /**
   * Returns the menu item, which shows the navigation icon of the tab switcher's toolbar.
   *
   * @return The menu item, which shows the navigation icon of the tab switcher's toolbar, as an
   * instance of the class {@link View} or null, if no navigation icon is shown
   */
  @Nullable
  private View getNavigationMenuItem() {
    Toolbar[] toolbars = tabSwitcher.getToolbars();

    if (toolbars != null) {
      Toolbar toolbar = toolbars.length > 1 ? toolbars[1] : toolbars[0];
      int size = toolbar.getChildCount();

      for (int i = 0; i < size; i++) {
        View child = toolbar.getChildAt(i);

        if (child instanceof ImageButton) {
          return child;
        }
      }
    }

    return null;
  }

  /**
   * Creates and returns a tab.
   *
   * @param index The index, the tab should be added at, as an {@link Integer} value
   * @return The tab, which has been created, as an instance of the class {@link Tab}. The tab may
   * not be null
   */
  @NonNull
  private Tab createTab(final int index) {
    CharSequence title = getString(R.string.tab_title, index + 1);
    return new Tab(title);
  }

  @Override
  public final void onSwitcherShown(@NonNull final TabSwitcher tabSwitcher) {
    drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
  }

  @Override
  public final void onSwitcherHidden(@NonNull final TabSwitcher tabSwitcher) {
    if (snackbar != null) {
      snackbar.dismiss();
    }
    drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
  }

  @Override
  public final void onSelectionChanged(@NonNull final TabSwitcher tabSwitcher,
                                       final int selectedTabIndex,
                                       @Nullable final Tab selectedTab) {

  }

  @Override
  public final void onTabAdded(@NonNull final TabSwitcher tabSwitcher, final int index,
                               @NonNull final Tab tab, @NonNull final Animation animation) {
    inflateMenu();
    TabSwitcher.setupWithMenu(tabSwitcher, createTabSwitcherButtonListener());
  }

  @Override
  public final void onTabRemoved(@NonNull final TabSwitcher tabSwitcher, final int index,
                                 @NonNull final Tab tab, @NonNull final Animation animation) {
    CharSequence text = getString(R.string.removed_tab_snackbar, tab.getTitle());
    showUndoSnackbar(text, index, tab);
    inflateMenu();
    TabSwitcher.setupWithMenu(tabSwitcher, createTabSwitcherButtonListener());
  }

  @Override
  public final void onAllTabsRemoved(@NonNull final TabSwitcher tabSwitcher,
                                     @NonNull final Tab[] tabs,
                                     @NonNull final Animation animation) {
    CharSequence text = getString(R.string.cleared_tabs_snackbar, tabs.length);
    showUndoSnackbar(text, 0, tabs);
    inflateMenu();
    TabSwitcher.setupWithMenu(tabSwitcher, createTabSwitcherButtonListener());
  }

  /**
   * The decorator, which is used to inflate and visualize the tabs of the activity's tab
   * switcher.
   */
  private class Decorator extends TabSwitcherDecorator {

    @NonNull
    @Override
    public View onInflateView(@NonNull final LayoutInflater inflater,
                              @Nullable final ViewGroup parent, final int viewType) {
      return inflater.inflate(R.layout.content_main, parent, false);
    }

    @Override
    public void onShowTab(@NonNull final Context context,
                          @NonNull final TabSwitcher tabSwitcher, @NonNull final View view,
                          @NonNull final Tab tab, final int index, final int viewType,
                          @Nullable final Bundle savedInstanceState) {
      toolbar.setVisibility(tabSwitcher.isSwitcherShown() ? View.GONE : View.VISIBLE);
    }
  }
}