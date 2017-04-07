package org.kiwix.kiwixmobile.new_bookmarks.lists;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.mikepenz.fastadapter.items.AbstractItem;

import org.kiwix.kiwixmobile.R;

import java.util.List;


public class ReadingListItem extends AbstractItem<ReadingListItem,ReadingListItem.ViewHolder> {
    private String title;
    private String articleCount;


    public String getTitle() {
        return title;
    }

    public String getArticleCount() {
        return articleCount;
    }

    public ReadingListItem(String title) {
        this.title = title;
    }

    //The unique ID for this type of item
    @Override
    public int getType() {
        return 0;
    }

    //The layout to be used for this type of item
    @Override
    public int getLayoutRes() {
        return R.layout.item_reading_list_folder;
    }

    //The logic to bind your data to the view
    @Override
    public void bindView(ViewHolder viewHolder, List<Object> payloads) {
        //call super so the selection is already handled for you
        super.bindView(viewHolder, payloads);

        //bind our data
        //set the text for the name
        viewHolder.name.setText(title);
        //set the text for the description or hide
        viewHolder.articlesCount.setText(articleCount);
    }

    //reset the view here (this is an optional method, but recommended)
    @Override
    public void unbindView(ViewHolder holder) {
        super.unbindView(holder);
        holder.name.setText(null);
        holder.articlesCount.setText(null);
    }

    //The viewHolder used for this item. This viewHolder is always reused by the RecyclerView so scrolling is blazing fast
    protected static class ViewHolder extends RecyclerView.ViewHolder {
        protected TextView name;
        protected TextView articlesCount;

        public ViewHolder(View view) {
            super(view);
            this.name = (TextView) view.findViewById(R.id.readinglist_item_title);
            this.articlesCount = (TextView) view.findViewById(R.id.readinglist_item_article_count);
        }
    }
}