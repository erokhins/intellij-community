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
package com.intellij.vcs.log.graph.collapsing;

import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcs.log.graph.api.LiteLinearGraph;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FragmentGenerator {

  public static class GreenFragment {
    @Nullable private final Integer myUpRedNode;
    @Nullable private final Integer myDownRedNode;
    @NotNull  private final Set<Integer> myMiddleGreenNodes;

    private GreenFragment(@Nullable Integer upRedNode, @Nullable Integer downRedNode, @NotNull Set<Integer> middleGreenNodes) {
      myUpRedNode = upRedNode;
      myDownRedNode = downRedNode;
      myMiddleGreenNodes = middleGreenNodes;
    }

    @Nullable
    public Integer getUpRedNode() {
      return myUpRedNode;
    }

    @Nullable
    public Integer getDownRedNode() {
      return myDownRedNode;
    }

    @NotNull
    public Collection<Integer> getMiddleGreenNodes() {
      return myMiddleGreenNodes;
    }
  }

  @NotNull
  private final LiteLinearGraph myGraph;
  @NotNull
  private final Condition<Integer> myRedNodes;

  public FragmentGenerator(@NotNull LiteLinearGraph graph, @NotNull Condition<Integer> redNodes) {
    myGraph = graph;
    myRedNodes = redNodes;
  }

  @NotNull
  public Set<Integer> getMiddleNodes(final int upNode, final int downNode) {
    Set<Integer> downWalk = getWalkNodes(upNode, false, new Condition<Integer>() {
      @Override
      public boolean value(Integer integer) {
        return integer > downNode;
      }
    });
    Set<Integer> upWalk = getWalkNodes(downNode, true, new Condition<Integer>() {
      @Override
      public boolean value(Integer integer) {
        return integer < upNode;
      }
    });

    downWalk.retainAll(upWalk);
    return downWalk;
  }

  @Nullable
  public Integer getNearRedNode(int startNode, int maxWalkSize, boolean toUp) {
    if (myRedNodes.value(startNode)) return startNode;

    Walker walker = new Walker(startNode, toUp);
    while (walker.notEmpty()) {
      Integer next = walker.doNext();

      if (myRedNodes.value(next)) return next;

      if (maxWalkSize < 0) return null;
      maxWalkSize--;

      walker.addAll(getNodes(next, toUp));
    }

    return null;
  }

  @NotNull
  public GreenFragment getGreenFragmentForCollapse(int startNode, int maxWalkSize) {
    if (myRedNodes.value(startNode)) return new GreenFragment(null, null, Collections.<Integer>emptySet());
    Integer upRedNode = getNearRedNode(startNode, maxWalkSize, true);
    Integer downRedNode = getNearRedNode(startNode, maxWalkSize, false);

    Set<Integer> upPart = upRedNode != null ?
                          getMiddleNodes(upRedNode, startNode) :
                          getWalkNodes(startNode, true, createStopFunction(maxWalkSize));

    Set<Integer> downPart = downRedNode != null ?
                            getMiddleNodes(startNode, downRedNode) :
                            getWalkNodes(startNode, false, createStopFunction(maxWalkSize));

    Set<Integer> middleNodes = ContainerUtil.union(upPart, downPart);
    if (upRedNode != null) middleNodes.remove(upRedNode);
    if (downRedNode != null) middleNodes.remove(downRedNode);

    return new GreenFragment(upRedNode, downRedNode, middleNodes);
  }

  @NotNull
  private Set<Integer> getWalkNodes(int startNode, boolean toUp, Condition<Integer> stopFunction) {
    Set<Integer> walkNodes = new HashSet<Integer>();

    Walker walker = new Walker(startNode, toUp);
    while (walker.notEmpty()) {
      Integer next = walker.doNext();
      if (!stopFunction.value(next)) {
        walkNodes.add(next);
        walker.addAll(getNodes(next, toUp));
      }
    }

    return walkNodes;
  }

  @NotNull
  private List<Integer> getNodes(int nodeIndex, boolean toUp) {
    return myGraph.getNodes(nodeIndex, LiteLinearGraph.NodeFilter.filter(toUp));
  }

  @NotNull
  private static Condition<Integer> createStopFunction(final int maxNodeCount) {
    return new Condition<Integer>() {
      private int count = maxNodeCount;
      @Override
      public boolean value(Integer integer) {
        count--;
        return count < 0;
      }
    };
  }

  private static class Walker {
    private final SortedSet<Integer> myWalkNodes;

    Walker(int startNode, final boolean toUp) {
      myWalkNodes = new TreeSet<Integer>(new Comparator<Integer>() {
        @Override
        public int compare(@NotNull Integer o1, @NotNull Integer o2) {
          if (toUp)
            return o2 - o1;
          return o1 - o2;
        }
      });
      myWalkNodes.add(startNode);
    }

    public Integer doNext() {
      Integer next = myWalkNodes.first();
      myWalkNodes.remove(next);
      return next;
    }

    public boolean notEmpty() {
      return !myWalkNodes.isEmpty();
    }

    public void addAll(List<Integer> nodes) {
      myWalkNodes.addAll(nodes);
    }
  }

}
