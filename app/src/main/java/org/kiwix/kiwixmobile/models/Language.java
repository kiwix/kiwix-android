package org.kiwix.kiwixmobile.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Locale;

public class Language implements Parcelable {

  public static final Creator<Language> CREATOR = new Creator<Language>() {
    @Override
    public Language createFromParcel(Parcel in) {
      return new Language(in);
    }

    @Override
    public Language[] newArray(int size) {
      return new Language[size];
    }
  };
  public String language;
  public String languageLocalized;
  public String languageCode;
  public Boolean active;
  public int booksCount;

  public Language(Locale locale, Boolean active) {
    this.language = locale.getDisplayLanguage();
    this.languageLocalized = locale.getDisplayLanguage(locale);
    this.languageCode = locale.getISO3Language();
    this.active = active;
  }

  public Language(String languageCode, Boolean active) {
    this(new Locale(languageCode), active);
  }

  private Language(Parcel in) {
    language = in.readString();
    languageLocalized = in.readString();
    languageCode = in.readString();
    byte tmpActive = in.readByte();
    active = tmpActive == 0 ? null : tmpActive == 1;
    booksCount = in.readInt();
  }

  public boolean equals(Language obj) {
    return obj.language.equals(language) &&
        obj.active.equals(active);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(language);
    dest.writeString(languageLocalized);
    dest.writeString(languageCode);
    dest.writeByte((byte) (active == null ? 0 : active ? 1 : 2));
    dest.writeInt(booksCount);
  }
}
