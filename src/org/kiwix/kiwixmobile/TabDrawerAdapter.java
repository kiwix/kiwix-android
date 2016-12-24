package org.kiwix.kiwixmobile;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import java.util.List;
import org.kiwix.kiwixmobile.views.KiwixWebView;

public class TabDrawerAdapter extends RecyclerView.Adapter<TabDrawerAdapter.ViewHolder> {
  private OnCloseTabClickListener onCloseTabClickListener;
  private OnSelectTabClickListener onSelectTabClickListener;
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
    holder.exit.setOnClickListener(v -> onCloseTabClickListener.onCloseTabClick(v, position));
    holder.itemView.setOnClickListener(v -> {
      onSelectTabClickListener.onSelectTabClick(v, position);
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

  public void setOnCloseTabClickListener(OnCloseTabClickListener listener) {
    this.onCloseTabClickListener = listener;
  }

  public void setOnSelectTabClickListener(OnSelectTabClickListener listener) {
    this.onSelectTabClickListener = listener;
  }

  public interface OnCloseTabClickListener {
    void onCloseTabClick(View view, int position);
  }

  public interface OnSelectTabClickListener {
    void onSelectTabClick(View view, int position);
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