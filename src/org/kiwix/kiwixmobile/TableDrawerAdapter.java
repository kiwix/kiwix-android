package org.kiwix.kiwixmobile;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import java.util.ArrayList;
import java.util.List;

public class TableDrawerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private TableClickListener listener;
  private String title;
  private List<DocumentSection> sections;

  private int selectedPosition = 0;

  public TableDrawerAdapter() {
    sections = new ArrayList<>();
  }

  @Override public int getItemViewType(int position) {
    if (position == 0) return 0;
    return 1;
  }

  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    int resource = R.layout.section_list;
    Context context = parent.getContext();
    View v = LayoutInflater.from(context).inflate(resource, parent, false);

    if (viewType == 0) return new HeaderViewHolder(v);
    return new SectionViewHolder(v);
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    ViewHolder vh = (ViewHolder) holder;

    Context context = holder.itemView.getContext();
    holder.itemView.setActivated(holder.getAdapterPosition() == selectedPosition);

    if (position == 0) {
      vh.title.setTypeface(Typeface.DEFAULT_BOLD);
      vh.title.setTextColor(Color.BLACK);
      if (title != null) {
        vh.title.setText(title);
      } else {
        String empty = context.getString(R.string.no_section_info);
        vh.title.setText(empty);
      }
      vh.itemView.setOnClickListener(v -> {
        updateSelection(holder.getAdapterPosition());
        listener.onHeaderClick(v);
      });
      return;
    }

    final int sectionPosition = position - 1;
    DocumentSection documentSection = sections.get(sectionPosition);
    vh.title.setText(documentSection.title);
    float density = context.getResources().getDisplayMetrics().density;
    int padding = (int) (((documentSection.level - 1) * 16) * density);
    vh.title.setPadding(padding, 0, 0, 0);
    vh.title.setTextColor((documentSection.level) % 2 == 0 ? Color.BLACK : Color.GRAY);
    vh.title.setText(sections.get(sectionPosition).title);
    vh.itemView.setOnClickListener(v -> {
      updateSelection(vh.getAdapterPosition());
      listener.onSectionClick(v, sectionPosition);
    });
  }

  private void updateSelection(int position) {
    if (selectedPosition == position) return;
    int oldPosition = selectedPosition;
    selectedPosition = position;

    notifyItemChanged(selectedPosition);
    notifyItemChanged(oldPosition);
  }

  @Override
  public int getItemCount() {
    return sections.size() + 1;
  }

  public void setTableClickListener(TableClickListener listener) {
    this.listener = listener;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setSections(List<DocumentSection> sections) {
    this.selectedPosition = 0;
    this.sections = sections;
  }

  public interface TableClickListener {
    void onHeaderClick(View view);

    void onSectionClick(View view, int position);
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    public @BindView(R.id.titleText) TextView title;

    public ViewHolder(View v) {
      super(v);
      ButterKnife.bind(this, v);
    }
  }

  public static class HeaderViewHolder extends ViewHolder {

    public HeaderViewHolder(View v) {
      super(v);
    }
  }

  public static class SectionViewHolder extends ViewHolder {

    public SectionViewHolder(View v) {
      super(v);
    }
  }

  public static class DocumentSection {
    public int level;
    public String title;
    public String id;
  }
}