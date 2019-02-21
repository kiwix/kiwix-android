package org.kiwix.kiwixmobile.bookmark;

import android.content.Context;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewEmptySupport extends RecyclerView {
  private View emptyView;

  private RecyclerView.AdapterDataObserver emptyObserver = new AdapterDataObserver() {

    @Override
    public void onChanged() {
      Adapter<?> adapter = getAdapter();
      if (adapter != null && emptyView != null) {
        if (adapter.getItemCount() == 0) {
          emptyView.setVisibility(View.VISIBLE);
          RecyclerViewEmptySupport.this.setVisibility(View.GONE);
        } else {
          emptyView.setVisibility(View.GONE);
          RecyclerViewEmptySupport.this.setVisibility(View.VISIBLE);
        }
      }
    }
  };

  public RecyclerViewEmptySupport(Context context, View emptyView) {
    super(context);
    this.emptyView = emptyView;
  }

  @Override
  public void setAdapter(Adapter adapter) {
    super.setAdapter(adapter);
    if (adapter != null) {
      adapter.registerAdapterDataObserver(emptyObserver);
    }

    emptyObserver.onChanged();
  }

  public void checkIfEmpty() {
    emptyObserver.onChanged();
  }
}
