package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.IntDef;

import androidx.annotation.NonNull;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.FileItem.FileStatus.*;

/**
 * Helper class, part of the local file sharing module.
 *
 * Defines a file-item to represent the files being transferred.
 */
public class FileItem implements Parcelable {

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({ TO_BE_SENT, SENDING, SENT, ERROR })
  public @interface FileStatus {
    int TO_BE_SENT = -1;  // File yet to be sent
    int SENDING = 0;      // Being sent
    int SENT = +1;        // Successfully sent
    int ERROR = +2;       // Error encountered while transferring the file
  }

  private Uri fileUri;
  private String fileName;
  private int fileStatus;

  public FileItem(@NonNull Uri fileUri) { // For sender devices
    this.fileUri = fileUri;
    this.fileName = WifiDirectManager.getFileName(fileUri);
    this.fileStatus = TO_BE_SENT;
  }

  public FileItem(@NonNull String fileName) { // For receiver devices
    this.fileUri = null;
    this.fileName = fileName;
    this.fileStatus = TO_BE_SENT;
  }

  // For making FileItem a Parcelable
  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(@NonNull Parcel dest, int flags) {
    dest.writeString(fileName);
    dest.writeInt(fileStatus);
  }

  public static final Parcelable.Creator<FileItem> CREATOR
      = new Parcelable.Creator<FileItem>() {

    @Override
    public FileItem createFromParcel(Parcel source) {
      return (new FileItem(source));
    }

    @Override
    public FileItem[] newArray(int size) {
      return new FileItem[size];
    }
  };

  public FileItem(@NonNull Parcel parcel) {
    this.fileName = parcel.readString();
    this.fileStatus = parcel.readInt();
  }

  // Helper methods
  public void setFileStatus(@FileStatus int fileStatus) {
    this.fileStatus = fileStatus;
  }

  public @NonNull Uri getFileUri() {
    return fileUri;
  }

  public @NonNull String getFileName() {
    return fileName;
  }

  @FileStatus
  public int getFileStatus() {
    return fileStatus;
  }
}
