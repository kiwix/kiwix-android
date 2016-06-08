package org.kiwix.kiwixmobile.library.entity;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

import java.util.List;
import java.util.Map;

@Root(strict = false, name="metalink")
public class MetaLinkNetworkEntity {

  public List<Url> getUrls() {
    return file.urls;
  }

  public Url getRelevantUrl() {
    return file.urls.get(0);
  }

  @Element
  private FileElement file;

  public FileElement getFile() {
    return file;
  }

  @Root(strict=false)
  public static class FileElement {
    @Attribute
    private String name;

    public String getName() {
      return name;
    }

    @ElementList(inline = true, entry = "url")
    private List<Url> urls;

    @Element
    private int size;

    public int getSize() {
      return size;
    }

    @ElementMap(entry = "hash", key = "type", attribute = true, inline = true)
    private Map<String, String> hashes;

    @Element
    private Pieces pieces;

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
     * @param type Hash type as defined in metalink file
     * @return Hash value or {@code null}
     */
    public String getHash(String type) {
      return hashes.get(type);
    }
  }

  private static class Pieces {
    @Attribute
    private int length;

    @Attribute(name="type")
    private String hashType;

    @ElementList(inline = true, entry="hash")
    private List<String> pieceHashes;
  }

  public static class Url {
    @Attribute
    private String location;

    @Attribute
    private int priority;

    @Text
    private String value;

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