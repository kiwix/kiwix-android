/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.kiwix.kiwixmobile.core.main;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import java.util.ArrayList;
import java.util.List;
import org.kiwix.kiwixmobile.core.R;
import org.kiwix.kiwixmobile.core.R2;

public class TableDrawerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private TableClickListener listener;
  private String title;
  private List<DocumentSection> sections;
  private int primary;
  private int secondary;

  private int selectedPosition = 0;

  TableDrawerAdapter() {
    sections = new ArrayList<>();
  }

  @Override
  public int getItemViewType(int position) {
    if (position == 0) return 0;
    return 1;
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    int resource = R.layout.section_list;
    Context context = parent.getContext();
    View v = LayoutInflater.from(context).inflate(resource, parent, false);

    TypedValue typedValue = new TypedValue();
    Resources.Theme theme = context.getTheme();
    theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
    primary = typedValue.data;
    theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
    secondary = typedValue.data;

    if (viewType == 0) return new HeaderViewHolder(v);
    return new SectionViewHolder(v);
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    ViewHolder vh = (ViewHolder) holder;

    Context context = holder.itemView.getContext();
    holder.itemView.setActivated(holder.getAdapterPosition() == selectedPosition);

    if (position == 0) {
      vh.title.setTypeface(Typeface.DEFAULT_BOLD);
      vh.title.setTextColor(primary);
      if (title != null && !title.isEmpty()) {
        vh.title.setText(title);
      } else {
        String empty = context.getString(R.string.no_section_info);
        if (context instanceof CoreMainActivity) {
          empty = ((CoreMainActivity) context).getCurrentWebView().getTitle();
        }
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
    vh.title.setTextColor((documentSection.level) % 2 == 0 ? primary : secondary);
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

  void setTableClickListener(TableClickListener listener) {
    this.listener = listener;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  void setSections(List<DocumentSection> sections) {
    this.selectedPosition = 0;
    this.sections = sections;
  }

  public interface TableClickListener {
    void onHeaderClick(View view);

    void onSectionClick(View view, int position);
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    public @BindView(R2.id.titleText)
    TextView title;

    public ViewHolder(View v) {
      super(v);
      ButterKnife.bind(this, v);
    }
  }

  public static class HeaderViewHolder extends ViewHolder {

    HeaderViewHolder(View v) {
      super(v);
    }
  }

  public static class SectionViewHolder extends ViewHolder {

    SectionViewHolder(View v) {
      super(v);
    }
  }

  public static class DocumentSection {
    public String title;
    public String id;
    int level;
  }
}
