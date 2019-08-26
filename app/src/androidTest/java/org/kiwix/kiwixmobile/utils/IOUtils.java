package org.kiwix.kiwixmobile.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
public class IOUtils {
  private IOUtils() {
    //utility class
  }

  private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
  private static final int EOF = -1;

  public static byte[] toByteArray(final InputStream input) throws IOException {
    try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      copy(input, output);
      return output.toByteArray();
    }
  }
  private static int copy(final InputStream input, final OutputStream output) throws IOException {
    final long count = copyLarge(input, output);
    if (count > Integer.MAX_VALUE) {
      return -1;
    }
    return (int) count;

  }
  private static long copyLarge(final InputStream input, final OutputStream output)      throws IOException {
    final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    long count = 0;
    int n;
    while (EOF != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }
}
