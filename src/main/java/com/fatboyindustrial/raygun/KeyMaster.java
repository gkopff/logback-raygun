/*
 * Copyright 2014 Greg Kopff
 *  All rights reserved.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package com.fatboyindustrial.raygun;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;

import java.util.Optional;

/**
 * Supplies API keys back to the appender. <p>
 *
 * Two modes of operation are supported: <ul>
 *   <li> the same single API key is always used, and
 *   <li> an API is conditionally supplied based on the hostname.
 * </ul> <p>
 */
public class KeyMaster
{
  /** Marker value for matching any host. */
  private static final String ANY_HOST = "__ANY_HOST";

  /** The host to API key mapping. */
  private final ImmutableMap<String, String> keys;

  /**
   * Constructor.
   * @param keys The host to API key mapping.
   */
  private KeyMaster(final ImmutableMap<String, String> keys)
  {
    this.keys = keys;
  }

  /**
   * Parse the configuration string and use it as the basis of the key master.
   * @param config The value of {@code <apiKey>}.
   * @return The configured key master instance.
   */
  public static KeyMaster fromConfigString(final String config)
  {
    if (config.indexOf(' ') != -1)
    {
      return new KeyMaster(parseNamed(config));
    }
    else
    {
      return new KeyMaster(ImmutableMap.of(ANY_HOST, config));
    }
  }

  /**
   * Gets the API key for the given host, or absent if there is no key defined. <p>
   *
   * If the key master is configured for a single API key (rather than named keys) then this method
   * ignores {@code host}.
   *
   * @param host The host to get the API key for.
   * @return The API key, or absent if no match was found.
   */
  public Optional<String> getApiKey(final String host)
  {
    if (this.keys.containsKey(ANY_HOST))
    {
      return Optional.of(this.keys.get(ANY_HOST));
    }
    else
    {
      return Optional.ofNullable(this.keys.get(host));
    }
  }

  /**
   * Parses the given string with the encoded hostname/API key pairings.
   * @param encoded The input string with values in {@code hostname[KEY]hostname[KEY]} format.
   * @return The mapping of hostname to API keys.
   * @throws IllegalArgumentException If the string is not in the required format.
   */
  @VisibleForTesting
  protected static ImmutableMap<String, String> parseNamed(final String encoded) throws IllegalArgumentException
  {
    final ImmutableMap.Builder<String, String> map = ImmutableMap.builder();

    for (final String str : Splitter.on(' ').split(encoded))
    {
      final int pivot = str.indexOf(':');
      if (pivot == -1)
      {
        throw new IllegalArgumentException("invalid format: " + str);
      }

      map.put(str.substring(0, pivot), str.substring(pivot + 1));
    }

    return map.build();
  }
}
