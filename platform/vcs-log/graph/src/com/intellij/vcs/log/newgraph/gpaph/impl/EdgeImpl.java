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
package com.intellij.vcs.log.newgraph.gpaph.impl;

import com.intellij.vcs.log.newgraph.gpaph.Edge;
import org.jetbrains.annotations.NotNull;

public class EdgeImpl implements Edge {
  private final int myUpVisibleIndex;
  private final int myDownVisibleIndex;

  @NotNull
  private final Type myType;

  private final int upLayoutIndex;
  private final int downLayoutIndex;

  public EdgeImpl(int upVisibleIndex, int downVisibleIndex, @NotNull Type type, int upLayoutIndex, int downLayoutIndex) {
    myUpVisibleIndex = upVisibleIndex;
    myDownVisibleIndex = downVisibleIndex;
    myType = type;
    this.upLayoutIndex = upLayoutIndex;
    this.downLayoutIndex = downLayoutIndex;
  }

  @Override
  public int getUpNodeVisibleIndex() {
    return myUpVisibleIndex;
  }

  @Override
  public int getDownNodeVisibleIndex() {
    return myDownVisibleIndex;
  }

  @NotNull
  @Override
  public Type getType() {
    return myType;
  }

  @Override
  public int upLayoutIndex() {
    return upLayoutIndex;
  }

  @Override
  public int downLayoutIndex() {
    return downLayoutIndex;
  }

  @Override
  public int getLayoutIndex() {
    return Math.max(upLayoutIndex, downLayoutIndex);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof EdgeImpl)) return false;

    EdgeImpl edge = (EdgeImpl)o;

    if (myDownVisibleIndex != edge.myDownVisibleIndex) return false;
    if (myUpVisibleIndex != edge.myUpVisibleIndex) return false;
    if (myType != edge.myType) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myUpVisibleIndex;
    result = 31 * result + myDownVisibleIndex;
    result = 31 * result + myType.hashCode();
    return result;
  }
}
