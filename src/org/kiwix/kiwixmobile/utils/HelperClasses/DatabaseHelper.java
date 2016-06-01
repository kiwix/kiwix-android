package org.kiwix.kiwixmobile.utils.HelperClasses;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.kiwix.kiwixmobile.utils.HelperClasses.ShortcutUtils;

import java.util.ArrayList;


public class DatabaseHelper extends SQLiteOpenHelper {

  public static final String DATABASE_NAME = "Kiwix.db";
  public static final String CONTACTS_TABLE_NAME = "recentsearches";
  public static final String CONTACTS_COLUMN_ID = "id";
  public static final String CONTACTS_COLUMN_SEARCH = "search";
  public static final String CONTACTS_COLUMN_ZIM = "zim";
  public String zimFile;

  public DatabaseHelper(Context context, String zimFile) {

    super(context, DATABASE_NAME, null, 2);
    this.zimFile = zimFile;
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    // TODO Auto-generated method stub
    db.execSQL(
        "create table IF NOT EXISTS " + CONTACTS_TABLE_NAME +
            " (id integer primary key, search text unique, zim text)"
    );
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // TODO Auto-generated method stub
    db.execSQL("DROP TABLE IF EXISTS " + CONTACTS_TABLE_NAME);
    onCreate(db);
  }

  public void deleteSearchTable(SQLiteDatabase db) {
    db.execSQL("DROP TABLE IF EXISTS " + CONTACTS_TABLE_NAME);
  }

  public void deleteSpecificSearch(SQLiteDatabase db, String search) {
    db.execSQL("delete from " + CONTACTS_TABLE_NAME + " where " + CONTACTS_COLUMN_SEARCH + "= '" + search + "'");
  }

  public boolean insertSearch(String search) {
    SQLiteDatabase db = this.getWritableDatabase();
    deleteSpecificSearch(db,search);
    ContentValues contentValues = new ContentValues();
    contentValues.put(CONTACTS_COLUMN_SEARCH, search);
    contentValues.put(CONTACTS_COLUMN_ZIM, zimFile);
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
    SQLiteDatabase db = this.getReadableDatabase();
    //hp = new HashMap();
//    Log.d(KiwixMobileActivity.TAG_KIWIX, zimFile);
    Cursor res = db.rawQuery("select * from " + CONTACTS_TABLE_NAME
        + " where " + CONTACTS_COLUMN_ZIM + " = '" + ShortcutUtils.escapeSqlSyntax(zimFile) + "'", null);
    res.moveToLast();

    while (!res.isBeforeFirst()) {
      array_list.add(res.getString(res.getColumnIndex(CONTACTS_COLUMN_SEARCH)));
      res.moveToPrevious();
    }
    res.close();
    return array_list;


  }
}

