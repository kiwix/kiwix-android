package org.kiwix.kiwixmobile.utils.files;

import android.content.Context;
import android.content.SharedPreferences;


public class RateAppCounter {

  String MASTER_NAME = "visitCounter";
  String NOTHANKS_CLICKED = "clickedNoThanks";
  SharedPreferences visitCounter;

  public RateAppCounter(Context context) {
    visitCounter = context.getSharedPreferences(MASTER_NAME, 0);
    visitCounter = context.getSharedPreferences(NOTHANKS_CLICKED,0);
  }

  public boolean getNoThanksState(){
    return visitCounter.getBoolean(NOTHANKS_CLICKED, false);
  }
  public void setNoThanksState(boolean val){
    SharedPreferences.Editor CounterEditor = visitCounter.edit();
    CounterEditor.putBoolean(NOTHANKS_CLICKED, val);
    CounterEditor.commit();
  }
  public void setCount(int count) {
    SharedPreferences.Editor CounterEditor = visitCounter.edit();
    CounterEditor.putInt("count", count);
    CounterEditor.commit();

  }
  public SharedPreferences.Editor getEditor(){
    return visitCounter.edit();
  }

  public int getCount() {
    return visitCounter.getInt("count", 0);
  }
}
