package org.kiwix.kiwixmobile.network;

import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.library.entity.MetaLinkNetworkEntity;
import retrofit2.http.GET;
import retrofit2.http.Url;
import rx.Observable;

public interface KiwixService {
  @GET("/library.xml") Observable<LibraryNetworkEntity> getLibrary();

  @GET Observable<MetaLinkNetworkEntity> getMetaLinks(@Url String url);
}
