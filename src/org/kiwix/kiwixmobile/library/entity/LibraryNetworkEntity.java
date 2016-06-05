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
package org.kiwix.kiwixmobile.library.entity;

import java.util.List;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

@Root(name = "library")
public class LibraryNetworkEntity {

  @ElementList(name = "book", inline = true, required = false)
  private List<Book> book;

  @Attribute(name = "version", required = false)
  private String version;

  public List<Book> getBooks() {
    return this.book;
  }

  public String getVersion() {
    return this.version;
  }

  public static class Book {

    @Attribute(name = "id", required = false)
    String id;

    @Attribute(name = "title", required = false)
    String title;

    @Attribute(name = "description", required = false)
    String description;

    @Attribute(name = "language", required = false)
    String language;

    @Attribute(name = "creator", required = false)
    String creator;

    @Attribute(name = "publisher", required = false)
    String publisher;

    @Attribute(name = "favicon", required = false)
    String favicon;

    @Attribute(name = "faviconMimeType", required = false)
    String faviconMimeType;

    @Attribute(name = "date", required = false)
    String date;

    @Attribute(name = "url", required = false)
    String url;

    @Attribute(name = "articleCount", required = false)
    String articleCount;

    @Attribute(name = "mediaCount", required = false)
    String mediaCount;

    @Attribute(name = "size", required = false)
    String size;

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

    public String getSize() {
      return this.size;
    }
  }
}