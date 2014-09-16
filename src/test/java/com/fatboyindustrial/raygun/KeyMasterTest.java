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

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link KeyMaster}.
 */
public class KeyMasterTest
{
  /**
   * Tests that the hostname/API key mappings can be extracted.
   */
  @Test
  public void testParseNamed()
  {
    final String input = "vogon.local:OpjzfzXvHooFqjyhT5311g== desiato:DYkfAazxL/HxiwnhtRkAvA==";
    final ImmutableMap<String, String> expected = ImmutableMap.of(
        "vogon.local", "OpjzfzXvHooFqjyhT5311g==",
        "desiato", "DYkfAazxL/HxiwnhtRkAvA==");

    assertThat(KeyMaster.parseNamed(input).size(), is(expected.size()));
    assertThat(KeyMaster.parseNamed(input), is(expected));
  }

  /**
   * Tests that getting an API key when using single key mode works.
   */
  @Test
  public void testSingleKeyMode()
  {
    final String key = "OpjzfzXvHooFqjyhT5311g==";
    final KeyMaster keyMaster = KeyMaster.fromConfigString(key);

    assertThat(keyMaster.getApiKey("anything"), is(Optional.of(key)));
  }

  /**
   * Tests that getting an API key when using the named key mode works.
   */
  @Test
  public void testNamedKeyMode()
  {
    final String input = "vogon.local:OpjzfzXvHooFqjyhT5311g== desiato:DYkfAazxL/HxiwnhtRkAvA==";
    final KeyMaster keyMaster = KeyMaster.fromConfigString(input);

    assertThat(keyMaster.getApiKey("anything"), is(Optional.empty()));
    assertThat(keyMaster.getApiKey("vogon.local"), is(Optional.of("OpjzfzXvHooFqjyhT5311g==")));
    assertThat(keyMaster.getApiKey("desiato"), is(Optional.of("DYkfAazxL/HxiwnhtRkAvA==")));
  }
}