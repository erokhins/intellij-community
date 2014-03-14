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

import java.util.HashMap;
import java.util.Map;

public class ShortTimestampGetter implements TimestampGetter {

  public static final int DEFAULT_BLOCK_SIZE = 30;

  private static final long MAX_DELTA = Short.MAX_VALUE - 10;
  private static final short BROKEN_DELTA = Short.MAX_VALUE;

  @NotNull
  public static ShortTimestampGetter newInstance(@NotNull TimestampGetter delegateGetter) {
    return newInstance(delegateGetter, DEFAULT_BLOCK_SIZE);
  }

  @NotNull
  public static ShortTimestampGetter newInstance(@NotNull TimestampGetter delegateGetter, int blockSize) {
    if (delegateGetter.size() == 0)
      throw new IllegalArgumentException("Empty TimestampGetter not supported");

    long[] saveTimestamps = new long[(delegateGetter.size() - 1) / blockSize + 1];
    for (int i = 0; i < saveTimestamps.length; i++)
      saveTimestamps[i] = delegateGetter.getTimestamp(blockSize * i);

    Map<Integer, Long> brokenDeltas = new HashMap<Integer, Long>();
    short[] deltas = new short[delegateGetter.size() - 1];

    for (int i = 0; i < delegateGetter.size() - 1; i++) {
      long delta = delegateGetter.getTimestamp(i + 1) - delegateGetter.getTimestamp(i);
      short shortDelta = deltaToShort(delta);
      deltas[i] = shortDelta;
      if (shortDelta == BROKEN_DELTA)
        brokenDeltas.put(i, delta);
    }

    return new ShortTimestampGetter(deltas, blockSize, saveTimestamps, brokenDeltas);
  }

  private static short deltaToShort(long delta) {
    if (delta >= 0 && delta <= MAX_DELTA)
      return (short)delta;

    if (delta < 0 && -delta <= MAX_DELTA)
      return (short)delta;

    return BROKEN_DELTA;
  }

  // myDeltas[i] = getTimestamp(i + 1) - getTimestamp(i)
  private final short[] myDeltas;

  @NotNull
  private final Map<Integer, Long> myBrokenDeltas;

  private final int myBlockSize;

  // saved 0, blockSize, 2 * blockSize, etc.
  private final long[] mySaveTimestamps;

  public ShortTimestampGetter(short[] deltas, int blockSize, long[] saveTimestamps, @NotNull Map<Integer, Long> brokenDeltas) {
    myDeltas = deltas;
    myBlockSize = blockSize;
    mySaveTimestamps = saveTimestamps;
    this.myBrokenDeltas = brokenDeltas;
  }

  @Override
  public int size() {
    return myDeltas.length + 1;
  }

  @Override
  public long getTimestamp(int index) {
    int relativeSaveIndex = index / myBlockSize;
    long timestamp = mySaveTimestamps[relativeSaveIndex];
    for (int i = myBlockSize * relativeSaveIndex; i < index; i++) {
      timestamp += getDelta(i);
    }

    return timestamp;
  }

  private long getDelta(int index) {
    short delta = myDeltas[index];
    if (delta != BROKEN_DELTA)
      return delta;

    return myBrokenDeltas.get(index);
  }
}
