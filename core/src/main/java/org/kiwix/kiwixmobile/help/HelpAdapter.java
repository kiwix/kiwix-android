package org.kiwix.kiwixmobile.help;

import android.animation.ObjectAnimator;
import android.graphics.Color;
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

import static org.kiwix.kiwixmobile.utils.AnimationUtils.collapse;
import static org.kiwix.kiwixmobile.utils.AnimationUtils.expand;

class HelpAdapter extends RecyclerView.Adapter<HelpAdapter.Item> {
  private final String[] titles;
  private final String[] descriptions;
  private final boolean nightMode;

  HelpAdapter(HashMap<String, String> titleDescriptionMap, boolean nightMode) {
    this.titles = titleDescriptionMap.keySet().toArray(new String[0]);
    this.descriptions = titleDescriptionMap.values().toArray(new String[0]);
    this.nightMode = nightMode;
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
    if (nightMode) {
      holder.toggleDescriptionVisibility.setColorFilter(Color.argb(255, 255, 255, 255));
    }
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
