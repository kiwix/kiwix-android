/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
 *
 */
package org.kiwix.kiwixmobile.core.entity;

import java.io.File;
import java.io.Serializable;
import java.util.LinkedList;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

@Root(name = "library", strict = false)
public class LibraryNetworkEntity {

  @ElementList(name = "book", inline = true, required = false)
  public LinkedList<Book> book;

  @Attribute(name = "version", required = false)
  private String version;

  public LinkedList<Book> getBooks() {
    return this.book;
  }

  public String getVersion() {
    return this.version;
  }

  @Root(name = "book", strict = false)
  public static class Book implements Serializable {

    @Attribute(name = "id", required = false)
    public String id;

    @Attribute(name = "title", required = false)
    public String title;

    @Attribute(name = "description", required = false)
    public String description;

    @Attribute(name = "language", required = false)
    public String language;

    @Attribute(name = "creator", required = false)
    public String creator;

    @Attribute(name = "publisher", required = false)
    public String publisher;

    @Attribute(name = "favicon", required = false)
    public String favicon;

    @Attribute(name = "faviconMimeType", required = false)
    public String faviconMimeType;

    @Attribute(name = "date", required = false)
    public String date;

    @Attribute(name = "url", required = false)
    public String url;

    @Attribute(name = "articleCount", required = false)
    public String articleCount;

    @Attribute(name = "mediaCount", required = false)
    public String mediaCount;

    @Attribute(name = "size", required = false)
    public String size;

    @Attribute(name = "name", required = false)
    public String bookName;

    @Attribute(name = "tags", required = false)
    public String tags;

    public int searchMatches = 0;
    @Deprecated
    public File file;
    @Deprecated
    public String remoteUrl;

    public String getId() {
      return this.id;
    }

    public String getTitle() {
      return this.title;
    }

    public String getDescription() {
      return this.description;
    }

    public String getLanguage() {
      return this.language;
    }

    public String getCreator() {
      return this.creator;
    }

    public String getPublisher() {
      return this.publisher;
    }

    public String getFavicon() {
      return this.favicon;
    }

    public String getFaviconMimeType() {
      return this.faviconMimeType;
    }

    public String getDate() {
      return this.date;
    }

    public String getUrl() {
      return this.url;
    }

    public String getArticleCount() {
      return this.articleCount;
    }

    public String getMediaCount() {
      return this.mediaCount;
    }

    public String getName() {
      return this.bookName;
    }

    public String getSize() {
      return this.size;
    }

    // Two books are equal if their ids match
    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Book) {
        if (((Book) obj).getId() != null && ((Book) obj).getId().equals(getId())) {
          return true;
        }
      }
      return false;
    }

    // Only use the book's id to generate a hash code
    @Override
    public int hashCode() {
      return getId().hashCode();
    }
  }
}
