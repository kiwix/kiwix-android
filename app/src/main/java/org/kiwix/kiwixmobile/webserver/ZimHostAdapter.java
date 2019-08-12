package org.kiwix.kiwixmobile.webserver;

import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem;
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.base.AdapterDelegate;
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.base.AdapterDelegateManager;
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.base.BaseDelegateAdapter;

import java.util.Arrays;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;

public final class ZimHostAdapter extends BaseDelegateAdapter {
  private static final AdapterDelegateManager<BaseDelegateAdapter> delegateManager =
      new AdapterDelegateManager<>();

  public long getIdFor(@NotNull BooksOnDiskListItem item) {
    Intrinsics.checkParameterIsNotNull(item, "item");
    return item.getId();
  }

  public long getIdFor(Object var1) {
    return this.getIdFor((BooksOnDiskListItem) var1);
  }

  public ZimHostAdapter(@NotNull AdapterDelegate... delegates) {
    super(Arrays.copyOf(delegates, delegates.length), delegateManager);
    Intrinsics.checkParameterIsNotNull(delegates, "delegates");
  }
}
