//package org.kiwix.kiwixmobile;
//
//import android.support.v7.widget.RecyclerView;
//import android.view.View;
//import android.widget.TextView;
//
//import com.mikepenz.fastadapter.items.AbstractItem;
//
//import java.util.List;
//
///**
// * Created by EladKeyshawn on 03/02/2017.
// */
//
//public class BookmarkItem extends AbstractItem<BookmarkItem,BookmarkItem.ViewHolder> {
//
//    private String title;
//    private String url;
//    public BookmarkItem(String title,String url) {
//        this.title = title;
//        this.url = url;
//    }
//
//
//    public String getTitle() {
//        return title;
//    }
//
//    public String getUrl() {
//        return url;
//    }
//
//    @Override
//    public int getType() {
//        return 0;
//    }
//
//    @Override
//    public int getLayoutRes() {
//        return R.layout.bookmarks_row;
//    }
//
//    @Override
//    public void bindView(ViewHolder holder) {
//        super.bindView(holder);
//        holder.titleTextView.setText(title);
//    }
//
//    static class ViewHolder extends RecyclerView.ViewHolder {
//
//        TextView titleTextView;
//        public ViewHolder(View view) {
//            super(view);
//            this.titleTextView = (TextView) view.findViewById(R.id.bookmark_title);
//        }
//    }
//
//}
