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

import com.intellij.vcs.log.facade.utils.IntDataList;
import org.jetbrains.annotations.NotNull;

public class CompressedIntDataList implements IntDataList {
  public static final int DEFAULT_BLOCK_SIZE = 20;

  @NotNull
  public static CompressedIntDataList newInstance(final int[] delegateArray) {
    return newInstance(delegateArray, DEFAULT_BLOCK_SIZE);
  }

  @NotNull
  public static CompressedIntDataList newInstance(final int[] delegateArray, int blockSize) {
    long ts = System.currentTimeMillis();

    IntDeltaCompressor intDeltaCompressor = IntDeltaCompressor.newInstance(new IntDataList() {
      @Override
      public int size() {
        return delegateArray.length - 1;
      }

      @Override
      public int get(int index) {
        return delegateArray[index + 1] - delegateArray[index];
      }
    });

    int[] strongValues = new int[(delegateArray.length - 1) / blockSize + 1];
    for (int i = 0; i < strongValues.length; i++)
      strongValues[i] = delegateArray[i * blockSize];

    CompressedIntDataList compressedIntDataList = new CompressedIntDataList(blockSize, strongValues, intDeltaCompressor);

    System.out.println(System.currentTimeMillis() - ts);
    return compressedIntDataList;
  }

  private final int myBlockSize;

  @NotNull
  private final int[] myStrongValues;

  @NotNull
  private final IntDeltaCompressor myCompressedDeltas;

  private CompressedIntDataList(int blockSize, @NotNull int[] strongValues, @NotNull final IntDeltaCompressor compressedDeltas) {
    myBlockSize = blockSize;
    myStrongValues = strongValues;
    myCompressedDeltas = compressedDeltas;
  }

  @Override
  public int size() {
    return myCompressedDeltas.size() + 1;
  }

  @Override
  public int get(int index) {
    int strongIndex = index / myBlockSize;
    return myStrongValues[strongIndex] + myCompressedDeltas.getSumOfInterval(strongIndex * myBlockSize, index);
  }

}
