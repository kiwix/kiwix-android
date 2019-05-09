/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.database.entity;

import com.yahoo.squidb.annotations.TableModelSpec;

@TableModelSpec(className = "DownloadDatabaseEntity", tableName = "downloads")
public class DownloadsSpec {
  public Long downloadId;
  public String bookId;
  public String favIcon;
  public String title;
  public String description;
  public String language;
  public String bookCreator;
  public String publisher;
  public String date;
  public String url;
  public String articleCount;
  public String mediaCount;
  public String size;
  public String name;
}
