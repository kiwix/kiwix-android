package org.kiwix.kiwixmobile.library.entity;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class MetaLinkNetworkEntityTest {
  private static Matcher<MetaLinkNetworkEntity.Url> url(
    String location, int priority, String value) {
    return new UrlMatcher(location, priority, value);
  }

  @Test
  public void testDeserialize() throws Exception {
    Serializer serializer = new Persister();
    final MetaLinkNetworkEntity result = serializer.read(
      MetaLinkNetworkEntity.class,
      MetaLinkNetworkEntityTest.class.getResourceAsStream(
        "wikipedia_af_all_nopic_2016-05.zim.meta4"
      ));
    assertThat(result.getUrls().size(), is(5));
    assertThat(result.getUrls(), hasItems(
      url("us", 1, "http://ftpmirror.your.org/pub/kiwix/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim"),
      url("gb", 2, "http://www.mirrorservice.org/sites/download.kiwix.org/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim"),
      url("us", 3, "http://download.wikimedia.org/kiwix/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim"),
      url("de", 4, "http://mirror.netcologne.de/kiwix/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim"),
      url("fr", 5, "http://mirror3.kiwix.org/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim")
    ));

    // Basic file attributes
    assertThat(result.getFile().getName(), is("wikipedia_af_all_nopic_2016-05.zim"));
    assertThat(result.getFile().getSize(), is(63973123L));

    // File hashes
    assertThat(result.getFile().getHash("md5"), is("6f06866b61c4a921b57f28cfd4307220"));
    assertThat(result.getFile().getHash("sha-1"), is("8aac4c7f89e3cdd45b245695e19ecde5aac59593"));
    assertThat(result.getFile().getHash("sha-256"),
      is("83126775538cf588a85edb10db04d6e012321a2025278a08a084b258849b3a5c"));

    // Pieces
    assertThat(result.getFile().getPieceHashType(), is("sha-1"));
    assertThat(result.getFile().getPieceLength(), is(1048576));

    // Check only the first and the last elements of the piece hashes
    assertThat(result.getFile().getPieceHashes().size(), is(62));
    assertThat(result.getFile().getPieceHashes().get(0),
      is("f36815d904d4fd563aaef4ee6ef2600fb1fd70b2"));
    assertThat(result.getFile().getPieceHashes().get(61),
      is("8055e515aa6e78f2810bbb0e0cd07330838b8920"));

  }

  /**
   * Implemented as a matcher only to avoid putting extra code into {@code MetaLinkNetworkEntity}.
   * However in case {@code equals} and {@code hashCode} methods are added to
   * {@code MetaLinkNetworkEntity.Url} class itself, this Matcher should be deleted.
   */
  private static class UrlMatcher extends TypeSafeMatcher<MetaLinkNetworkEntity.Url> {
    private String location;
    private int priority;
    private String value;

    public UrlMatcher(String location, int priority, String value) {
      this.location = location;
      this.priority = priority;
      this.value = value;
    }

    @Override
    protected boolean matchesSafely(MetaLinkNetworkEntity.Url item) {
      return location.equals(item.getLocation()) && priority == item.getPriority()
        && value.equals(item.getValue());
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(String.format(
        "Url (location=%s, priority=%d, value=%s", location, priority, value));
    }
  }
}
