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
package com.intellij.vcs.log.graph.impl.visible;

import com.intellij.util.BooleanFunction;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcs.log.graph.api.LinearGraph;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

public class LinearGraphWalker {
  @NotNull
  private final LinearGraph myLinearGraph;

  public LinearGraphWalker(@NotNull LinearGraph linearGraph) {
    myLinearGraph = linearGraph;
  }

  public void downWalk(int startNode, @NotNull BooleanFunction<Integer> stopFunction) {
    Set<Integer> nextNodes = ContainerUtil.newHashSet();
    nextNodes.add(startNode);
    while (!nextNodes.isEmpty()) {
      Integer nextNode = Collections.min(nextNodes);
      nextNodes.remove(nextNode);
      if (stopFunction.fun(nextNode))
        break;

      nextNodes.addAll(myLinearGraph.getDownNodes(nextNode));
    }
  }

  public void upWalk(int startNode, @NotNull BooleanFunction<Integer> stopFunction) {
    Set<Integer> nextNodes = ContainerUtil.newHashSet();
    nextNodes.add(startNode);
    while (!nextNodes.isEmpty()) {
      Integer nextNode = Collections.max(nextNodes);
      nextNodes.remove(nextNode);
      if (stopFunction.fun(nextNode))
        break;

      nextNodes.addAll(myLinearGraph.getUpNodes(nextNode));
    }
  }
}
