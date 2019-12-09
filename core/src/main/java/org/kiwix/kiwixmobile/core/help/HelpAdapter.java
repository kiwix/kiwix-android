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

package org.kiwix.kiwixmobile.core.help;

import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import java.util.HashMap;
import org.kiwix.kiwixmobile.core.R;
import org.kiwix.kiwixmobile.core.R2;

import static org.kiwix.kiwixmobile.core.utils.AnimationUtils.collapse;
import static org.kiwix.kiwixmobile.core.utils.AnimationUtils.expand;

class HelpAdapter extends RecyclerView.Adapter<HelpAdapter.Item> {
  private final String[] titles;
  private final String[] descriptions;

  HelpAdapter(HashMap<String, String> titleDescriptionMap) {
    this.titles = titleDescriptionMap.keySet().toArray(new String[0]);
    this.descriptions = titleDescriptionMap.values().toArray(new String[0]);
  }

  @NonNull
  @Override
  public Item onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new Item(LayoutInflater.from(parent.getContext())
      .inflate(R.layout.item_help, parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull Item holder, int position) {
    holder.title.setText(titles[position]);
    holder.description.setText(descriptions[position]);
  }

  @Override
  public int getItemCount() {
    return titles.length;
  }

  class Item extends RecyclerView.ViewHolder {
    @BindView(R2.id.item_help_title)
    TextView title;
    @BindView(R2.id.item_help_description)
    TextView description;
    @BindView(R2.id.item_help_toggle_expand)
    ImageView toggleDescriptionVisibility;

    Item(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    @OnClick({ R2.id.item_help_title, R2.id.item_help_toggle_expand })
    void toggleDescriptionVisibility() {
      if (description.getVisibility() == View.GONE) {
        ObjectAnimator.ofFloat(toggleDescriptionVisibility, "rotation", 0, 180).start();
        expand(description);
      } else {
        collapse(description);
        ObjectAnimator.ofFloat(toggleDescriptionVisibility, "rotation", 180, 360).start();
      }
    }
  }
}
