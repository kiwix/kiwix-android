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
package org.kiwix.kiwixmobile.library.entity;

import java.util.List;
import java.util.Map;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

@Root(strict = false, name = "metalink")
public class MetaLinkNetworkEntity {

  @Element
  public FileElement file;

  public List<Url> getUrls() {
    return file.urls;
  }

  public Url getRelevantUrl() {
    return file.urls.get(0);
  }

  public FileElement getFile() {
    return file;
  }

  @Root(strict = false)
  public static class FileElement {
    @Attribute
    public String name;
    @ElementList(inline = true, entry = "url")
    public List<Url> urls;
    @Element
    private long size;
    @ElementMap(entry = "hash", key = "type", attribute = true, inline = true)
    public Map<String, String> hashes;
    @Element
    public Pieces pieces;

    public String getName() {
      return name;
    }

    public long getSize() {
      return size;
    }

    public int getPieceLength() {
      return pieces.length;
    }

    public String getPieceHashType() {
      return pieces.hashType;
    }

    public List<String> getPieceHashes() {
      return pieces.pieceHashes;
    }

    /**
     * Get file hash
     *
     * @param type Hash type as defined in metalink file
     * @return Hash value or {@code null}
     */
    public String getHash(String type) {
      return hashes.get(type);
    }
  }

  public static class Pieces {
    @Attribute
    private int length;

    @Attribute(name = "type")
    public String hashType;

    @ElementList(inline = true, entry = "hash")
    public List<String> pieceHashes;
  }

  public static class Url {
    @Attribute
    public String location;

    @Attribute
    private int priority;

    @Text
    public String value;

    public String getValue() {
      return value;
    }

    public String getLocation() {
      return location;
    }

    public int getPriority() {
      return priority;
    }
  }
}
