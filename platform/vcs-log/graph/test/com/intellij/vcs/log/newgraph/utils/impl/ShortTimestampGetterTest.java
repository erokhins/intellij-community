/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.vcs.log.newgraph.utils.impl;

import com.intellij.vcs.log.newgraph.utils.TimestampGetter;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

public class ShortTimestampGetterTest {

  private final static long SHORT_MAX = Short.MAX_VALUE;
  private final static int BLOCK_SIZE = 3;

  private static TimestampGetter create(final long... timestamp) {
    return new TimestampGetter() {
      @Override
      public int size() {
        return timestamp.length;
      }

      @Override
      public long getTimestamp(int index) {
        return timestamp[index];
      }
    };
  }

  private static String toStr(@NotNull TimestampGetter timestampGetter) {
    StringBuilder s = new StringBuilder();
    for (int i = 0; i < timestampGetter.size(); i++) {
      if (i != 0)
        s.append(", ");
      s.append(timestampGetter.getTimestamp(i));
    }
    return s.toString();
  }

  protected void runTest(long ... timestamp) {
    TimestampGetter timestampGetter = create(timestamp);
    ShortTimestampGetter shortTimestampGetter = ShortTimestampGetter.newInstance(timestampGetter, BLOCK_SIZE);
    assertEquals(toStr(timestampGetter), toStr(shortTimestampGetter));
  }

  @Test
  public void simple() {
    runTest(1, 4, 2, -4, 200);
  }

  @Test
  public void checkEmpty() {
    try {
      runTest();
    } catch (IllegalArgumentException e) {
      return;
    }

    fail();
  }

  @Test
  public void checkBlockSize() {
    runTest(1);
    runTest(1, 2);
    runTest(1, 2, 3);
    runTest(1, 2, 3, 4);
    runTest(1, 2, 3, 4, 5);
    runTest(1, 2, 3, 4, 5, 6);
    runTest(1, 2, 3, 4, 5, 6, 7);
  }

  @Test
  public void oneTime() {
    runTest(1);
    runTest(-SHORT_MAX);
    runTest(SHORT_MAX);

    runTest(SHORT_MAX - 100);
    runTest(SHORT_MAX - 1000);
  }

  @Test
  public void overflow() {
    runTest(1000, 1000 +  SHORT_MAX, SHORT_MAX, -SHORT_MAX, 100 * SHORT_MAX);
  }
}
