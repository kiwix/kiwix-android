/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kiwix.kiwixmobile.network.converter;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.kiwix.kiwixmobile.library.entity.MetaLinkNetworkEntity;
import retrofit2.Converter;
import retrofit2.Retrofit;

public final class MetaLinkConverterFactory extends Converter.Factory {
  public static MetaLinkConverterFactory create() {
    return new MetaLinkConverterFactory();
  }

  private MetaLinkConverterFactory() {
  }

  @Override public Converter<?, RequestBody> requestBodyConverter(Type type,
      Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
    return null;
  }

  @Override
  public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
      Retrofit retrofit) {
    if (type == MetaLinkNetworkEntity.class) {
      return MetaLinkConverter.INSTANCE;
    }
    return null;
  }

  static final class MetaLinkConverter implements Converter<ResponseBody, MetaLinkNetworkEntity> {
    static final MetaLinkConverter INSTANCE = new MetaLinkConverter();
    private static final Pattern pattern = Pattern.compile("<url.*?>(.*?)</url>");

    @Override public MetaLinkNetworkEntity convert(ResponseBody value) throws IOException {
      List<String> urls = new ArrayList<>();

      Matcher matcher = pattern.matcher(value.string());
      while (matcher.find()) {
        urls.add(matcher.group(1));
      }
      if (!urls.isEmpty() && urls.size() >= 2) urls = urls.subList(1, urls.size());

      MetaLinkNetworkEntity metaLinkNetworkEntity = new MetaLinkNetworkEntity();
      metaLinkNetworkEntity.setUrls(urls);

      return metaLinkNetworkEntity;
    }
  }
}
