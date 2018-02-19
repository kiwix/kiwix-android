package org.kiwix.kiwixmobile.common.views.bottom_tab;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.common.views.web.KiwixWebView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TabDrawerAdapter extends RecyclerView.Adapter<TabDrawerAdapter.ViewHolder> {
  private TabClickListener listener;
  private List<KiwixWebView> webViews;

  private int selectedPosition = 0;

  public TabDrawerAdapter(List<KiwixWebView> webViews) {
    this.webViews = webViews;
  }

  @Override
  public TabDrawerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.tabs_list, parent, false);
    return new ViewHolder(v);
  }

  @Override
  public void onBindViewHolder(ViewHolder holder, int position) {
    KiwixWebView webView = webViews.get(position);
    holder.title.setText(webView.getTitle());
    holder.exit.setOnClickListener(v -> listener.onCloseTab(v, position));
    holder.itemView.setOnClickListener(v -> {
      listener.onSelectTab(v, position);
      selectedPosition = holder.getAdapterPosition();
      notifyDataSetChanged();
      holder.itemView.setActivated(true);
    });
    holder.itemView.setActivated(holder.getAdapterPosition() == selectedPosition);
  }

  @Override
  public int getItemCount() {
    return webViews.size();
  }

  public void setSelected(int position) {
    this.selectedPosition = position;
  }

  public int getSelectedPosition() {
    return selectedPosition;
  }

  public void setTabClickListener(TabClickListener listener) {
    this.listener = listener;
  }

  public interface TabClickListener {
    void onSelectTab(View view, int position);

    void onCloseTab(View view, int position);
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    public @BindView(R.id.titleText) TextView title;
    public @BindView(R.id.exitButton) ImageView exit;

    public ViewHolder(View v) {
      super(v);
      ButterKnife.bind(this, v);
    }
  }
}