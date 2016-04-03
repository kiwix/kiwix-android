package org.kiwix.kiwixmobile.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.DatabaseUtils;

import java.util.ArrayList;
import java.util.HashMap;


public class DatabaseHelper extends SQLiteOpenHelper {

  public static final String DATABASE_NAME = "Kiwix.db";
  public static final String CONTACTS_TABLE_NAME = "recentsearches";
  public static final String CONTACTS_COLUMN_ID = "id";
  public static final String CONTACTS_COLUMN_SEARCH = "search";

  public DatabaseHelper(Context context) {
    super(context, DATABASE_NAME, null, 1);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    // TODO Auto-generated method stub
    db.execSQL(
        "create table " + CONTACTS_TABLE_NAME +
            " (id integer primary key, search text)"
    );
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // TODO Auto-generated method stub
    db.execSQL("DROP TABLE IF EXISTS " + CONTACTS_TABLE_NAME);
    onCreate(db);
  }

  public boolean insertSearch(String search) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues contentValues = new ContentValues();
    contentValues.put(CONTACTS_COLUMN_SEARCH, search);
    db.insert(CONTACTS_TABLE_NAME, null, contentValues);
    return true;
  }

  public Cursor getData(int id) {
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor res = db.rawQuery("select * from " + CONTACTS_TABLE_NAME + " where id=" + id + "", null);
    return res;
  }

  public int numberOfRows() {
    SQLiteDatabase db = this.getReadableDatabase();
    int numRows = (int) DatabaseUtils.queryNumEntries(db, CONTACTS_TABLE_NAME);
    return numRows;
  }

  public Integer deleteSearches(Integer id) {
    SQLiteDatabase db = this.getWritableDatabase();
    return db.delete(CONTACTS_TABLE_NAME,
        "id = ? ",
        new String[]{Integer.toString(id)});
  }

  public ArrayList<String> getRecentSearches() {
    ArrayList<String> array_list = new ArrayList<String>();

    //hp = new HashMap();
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor res = db.rawQuery("select * from " + CONTACTS_TABLE_NAME, null);
    res.moveToLast();

    while (res.isBeforeFirst() == false) {
      array_list.add(res.getString(res.getColumnIndex(CONTACTS_COLUMN_SEARCH)));
      res.moveToPrevious();
    }
    return array_list;
  }
}

