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

package com.intellij.vcs.log.facade.utils.impl;

import com.intellij.util.BooleanFunction;
import com.intellij.vcs.log.facade.utils.Flags;
import com.intellij.vcs.log.facade.utils.IntDataList;
import com.intellij.vcs.log.facade.utils.IntToIntMap;
import org.jetbrains.annotations.NotNull;

public class CompressedIntDataList implements IntDataList {

  private static final int BYTE1_MASK = 0xff;
  private static final int BYTE2_MASK = 0xffff;
  private static final int BYTE3_MASK = 0xffffff;
  private static final int BYTE4_MASK = 0xffffffff;

  private final int[] myControlValues;

  private final byte[] myCompressedDeltas;

  @NotNull
  private final Flags myStartedBytes;

  @NotNull
  private final IntToIntMap myDeltaStartIndexes;

  public CompressedIntDataList(int[] controlValues, byte[] compressedDeltas, @NotNull Flags startedBytes) {
    myControlValues = controlValues;
    myCompressedDeltas = compressedDeltas;
    myStartedBytes = startedBytes;
    myDeltaStartIndexes = ListIntToIntMap.newInstance(new BooleanFunction<Integer>() {
      @Override
      public boolean fun(Integer integer) {
        return myStartedBytes.get(integer);
      }
    }, myStartedBytes.size());
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public int get(int index) {
    return 0;
  }


  private static class IntCompressor {

    public static final int OFFSET_1 = 8 * 1 - 1;
    public static final int OFFSET_2 = 8 * 2 - 1;
    public static final int OFFSET_3 = 8 * 3 - 1;

    public static final int OR_MASK_1 = 1 << OFFSET_1;
    public static final int OR_MASK_2 = 1 << OFFSET_2;
    public static final int OR_MASK_3 = 1 << OFFSET_3;

    // return count of byte after compression, value must be more or equals that 0
    public static int sizeOf(int value) {
      if (value >> OFFSET_1 == 0)
        return 1;
      if (value >> OFFSET_2 == 0)
        return 2;
      if (value >> OFFSET_3 == 0)
        return 3;
      return 4;
    }

    // value must be more or equals that 0, sizeOf = sizeOf(value)
    public static int addNegativeBit(int value, int sizeOf) {
      if (sizeOf == 1)
        return value | OR_MASK_1;
      if (sizeOf == 2)
        return value | OR_MASK_2;
      if (sizeOf == 3)
        return value | OR_MASK_3;
      return value;
    }

    public static byte split(int value, int sizeOf, int byteIndex) {
      return 0;
    }
  }
}
