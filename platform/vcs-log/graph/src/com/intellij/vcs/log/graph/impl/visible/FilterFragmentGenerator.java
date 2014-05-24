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

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.util.BooleanFunction;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcs.log.graph.api.LinearGraph;
import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import com.intellij.vcs.log.graph.api.elements.GraphElement;
import com.intellij.vcs.log.graph.api.elements.GraphNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class FilterFragmentGenerator {

  public static void doForIntermediateNodes(@NotNull LinearGraph linearGraph, int upNode, int downNode, @NotNull Consumer<Integer> doIt) {
    FilterFragmentGenerator filterFragmentGenerator =
      new FilterFragmentGenerator(linearGraph, Conditions.<Integer>alwaysFalse(), Conditions.<Integer>alwaysFalse());
    FilterFragment filterFragment = new FilterFragment(upNode, downNode, true);
    for (int node: filterFragmentGenerator.getIntermediateNodes(filterFragment)) {
      doIt.consume(node);
    }
  }

  @NotNull
  private final LinearGraph myLinearGraph;

  @NotNull
  private final LinearGraphWalker myGraphWalker;

  @NotNull
  private final Condition<Integer> myThisIsFilerNode;

  @NotNull
  private final FragmentGenerator myGrayLinearGenerator;

  public FilterFragmentGenerator(@NotNull LinearGraph linearGraph,
                                 @NotNull Condition<Integer> thisIsFilerNode,
                                 @NotNull Condition<Integer> thisIsBranchNode) {
    myLinearGraph = linearGraph;
    myThisIsFilerNode = thisIsFilerNode;
    myGraphWalker = new LinearGraphWalker(linearGraph);
    myGrayLinearGenerator = new FragmentGenerator(linearGraph, Conditions.or(thisIsBranchNode, thisIsFilerNode));
  }

  @Nullable
  public FilterFragment getFragmentForCollapse(@NotNull GraphElement graphElement) {
    FragmentGenerator.GraphFragment relativeFragment = myGrayLinearGenerator.getLongFragment(graphElement);
    if (relativeFragment != null)
      return new FilterFragment(relativeFragment.upNodeIndex, relativeFragment.downNodeIndex, false);

    final int[] fragment = new int[2];
    if (graphElement instanceof GraphNode) {
      fragment[0] = ((GraphNode)graphElement).getNodeIndex();
      fragment[1] = fragment[0];
    } else {
      GraphEdge edge = (GraphEdge)graphElement;
      fragment[0] = edge.getUpNodeIndex();
      fragment[1] = edge.getDownNodeIndex();
    }

    myGraphWalker.upWalk(fragment[0], new BooleanFunction<Integer>() {
      @Override
      public boolean fun(Integer node) {
        fragment[0] = node;
        if (myThisIsFilerNode.value(node)) {
          return true;
        }
        return false;
      }
    });

    myGraphWalker.downWalk(fragment[1], new BooleanFunction<Integer>() {
      @Override
      public boolean fun(Integer node) {
        fragment[1] = node;
        if (myThisIsFilerNode.value(node)) {
          return true;
        }
        return false;
      }
    });

    if (fragment[0] == fragment[1] || myLinearGraph.getDownNodes(fragment[0]).contains(fragment[1]))
      return null;

    return new FilterFragment(fragment[0], fragment[1], true);
  }

  public Set<Integer> getIntermediateNodes(@NotNull final FilterFragment filterFragment) {
    final Set<Integer> intermediateNodes = ContainerUtil.newHashSet();

    final Set<Integer> downWalk = ContainerUtil.newHashSet();
    myGraphWalker.downWalk(filterFragment.upNodeIndex, new BooleanFunction<Integer>() {
      @Override
      public boolean fun(Integer node) {
        if (node >= filterFragment.downNodeIndex)
          return true;

        downWalk.add(node);
        return false;
      }
    });

    myGraphWalker.upWalk(filterFragment.downNodeIndex, new BooleanFunction<Integer>() {
      @Override
      public boolean fun(Integer node) {
        if (node <= filterFragment.upNodeIndex)
          return true;
        if (downWalk.contains(node))
          intermediateNodes.add(node);
        return false;
      }
    });
    return intermediateNodes;
  }

  public static class FilterFragment {
    public final int upNodeIndex;
    public final int downNodeIndex;
    public final boolean isFilterFragment;

    FilterFragment(int upNodeIndex, int downNodeIndex, boolean isFilterFragment) {
      this.upNodeIndex = upNodeIndex;
      this.downNodeIndex = downNodeIndex;
      this.isFilterFragment = isFilterFragment;
    }
  }
}
