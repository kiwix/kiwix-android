package org.kiwix.kiwixmobile.views;

import android.content.Context;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import org.kiwix.kiwixmobile.JNIKiwix;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.io.File;

import org.kiwix.kiwixmobile.ZimContentProvider;

public class AutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {

  private List<String> mData;

  private KiwixFilter mFilter;

  public AutoCompleteAdapter(Context context) {
    super(context, android.R.layout.simple_list_item_1);
    mData = new ArrayList<String>();
    mFilter = new KiwixFilter();
  }

  @Override
  public int getCount() {
    return mData.size();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View row = super.getView(position, convertView, parent);

    TextView tv = (TextView) row.findViewById(android.R.id.text1);
    tv.setText(Html.fromHtml(getItem(position)));

    return row;
  }

  @Override
  public String getItem(int index) {
    String a = mData.get(index);
    if (a.endsWith(".html")) {
      String trim = a.substring(2);
      trim = trim.substring(0, trim.length() - 5);
      return trim.replace("_", " ");
    } else return a;
  }

  public String getItemRaw(int index) {
    return mData.get(index);
  }

  @Override
  public Filter getFilter() {
    return mFilter;
  }

  class KiwixFilter extends Filter {

    private void addToList(List data, String result, String prefix) {
      // highlight by word
      String[] highlight = prefix.split(" ");
      String toAdd = result.substring(0, result.length() - 5).substring(2);
      for (String todo : highlight)
        if (todo.length() > 0)
          toAdd = toAdd.replaceAll("(?i)(" + Pattern.quote(todo) + ")", "<b>$1</b>");
      // add to list
      data.add("A/" + toAdd + ".html");
    }

    public String getDbName(String file){
      String[] names = {file, file};
      if (!names[0].substring(names[0].length() - 3).equals("zim")){
        names[0] = names[0].substring(0, names[0].length() - 2);
      }
      for(String name : names) { // try possible places for index directory
        File f = new File(name + ".idx");
        if (f.exists() && f.isDirectory()) {
          return name + ".idx"; // index in directory <zimfile>.zim.idx or <zimfile>.zimaa.idx
        }
      }
      return file; // index is in zim file itself...
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
      FilterResults filterResults = new FilterResults();
      ArrayList<String> data = new ArrayList<>();
      if (constraint != null) {
        try {
          final String prefix = constraint.toString();
          String qStr = capitalizeQuery(prefix);
          String[] result = JNIKiwix.indexedQuery(getDbName(ZimContentProvider.getZimFile()), qStr).split("\n");

          if (result.length == 1 && result[0].trim().isEmpty()) {
            result = JNIKiwix.indexedQueryPartial(getDbName(ZimContentProvider.getZimFile()), qStr).split("\n");
          }

          if (hasNonEmptyResult(result)) {
            // At least one non empty result
            data.clear();
            List<String> alreadyAdded = new ArrayList<String>();
            String pageUrl = ZimContentProvider.getPageUrlFromTitle(prefix);
            if (pageUrl != null) {
              addToList(data, pageUrl, prefix);
              alreadyAdded.add(pageUrl);
            }
            for (int i = 0; i < 3; i++) {
              String suggestion = ZimContentProvider.getNextSuggestion();
              if (suggestion != null && !suggestion.isEmpty()) {
                pageUrl = ZimContentProvider.getPageUrlFromTitle(suggestion);
                if (!alreadyAdded.contains(pageUrl)) {
                  addToList(data, pageUrl, prefix);
                  alreadyAdded.add(pageUrl);
                }
              }
            }
            for (String res : result) {
              if (!alreadyAdded.contains(res)) {
                addToList(data, res, prefix);
                alreadyAdded.add(pageUrl);
              }
            }
          } else {
            // fallback to legacy search method if index not found
            ZimContentProvider.searchSuggestions(prefix, 200);
            String suggestion;
            data.clear();
            while ((suggestion = ZimContentProvider.getNextSuggestion()) != null) {
              data.add(suggestion);
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
        filterResults.values = data;
        filterResults.count = data.size();
      }
      return filterResults;
    }

    private boolean hasNonEmptyResult(String[] result) {
      return result.length > 0 && !result[0].trim().isEmpty();
    }

    private String capitalizeQuery(String prefix) {
      List<String> rs = new ArrayList<>();
      for (String word : prefix.split(" ")) {
        rs.add((word.length() > 1
            ? Character.toUpperCase(word.charAt(0)) + word.substring(1)
            : word.toUpperCase()));
      }
      String query = TextUtils.join(" ", rs);
      query.replace("us ", "U.S. ");
      return query;
    }

    @Override
    protected void publishResults(CharSequence contraint, FilterResults results) {
      mData = (ArrayList<String>) results.values;
      if (results.count > 0) {
        notifyDataSetChanged();
      } else {
        notifyDataSetInvalidated();
      }
    }
  }
}
