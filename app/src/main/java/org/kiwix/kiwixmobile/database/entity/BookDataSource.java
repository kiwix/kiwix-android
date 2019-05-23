/*
 * Copyright 2016
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU  General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.kiwix.kiwixmobile.database.entity;

import com.yahoo.squidb.annotations.TableModelSpec;

@TableModelSpec(className = "BookDatabaseEntity", tableName = "book")
public class BookDataSource {

  public String bookId;

  public String title;

  public String description;

  public String language;

  public String bookCreator;

  public String publisher;

  public String date;

  public String url;

  public String remoteUrl;

  public String articleCount;

  public String mediaCount;

  public String size;

  public String favicon;

  public String name;

  public boolean downloaded;

}