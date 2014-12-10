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

import com.intellij.vcs.log.graph.api.LiteLinearGraph;
import com.intellij.vcs.log.graph.api.elements.GraphEdgeType;
import com.intellij.vcs.log.graph.utils.LinearGraphUtils;
import org.jetbrains.annotations.NotNull;

public class AdditionEdgesGenerator {
  public void update(@NotNull CollapsedGraph collapsedGraph, int upDelegateNodeIndex, int downDelegateNodeIndex) {
    new AdditionEdgesGenerator(collapsedGraph).update(upDelegateNodeIndex, downDelegateNodeIndex);
  }

  @NotNull
  private final CollapsedGraph myCollapsedGraph;

  @NotNull
  private final LiteLinearGraph myLiteDelegateGraph;

  private AdditionEdgesGenerator(@NotNull CollapsedGraph collapsedGraph) {
    myCollapsedGraph = collapsedGraph;
    myLiteDelegateGraph = LinearGraphUtils.asLiteLinearGraph(collapsedGraph.getDelegateGraph());
  }

  private boolean nodeIsVisible(int nodeIndex) {
    return myCollapsedGraph.getNodeVisibility(getNodeId(nodeIndex));
  }

  private int getNodeId(int nodeIndex) {
    return myCollapsedGraph.getDelegateGraph().getGraphNode(nodeIndex).getNodeId();
  }

  private void addDottedEdge(int nodeIndex1, int nodeIndex2) {
    myCollapsedGraph.getGraphAdditionEdges().createEdge(getNodeId(nodeIndex1), getNodeId(nodeIndex2), GraphEdgeType.DOTTED);
  }

  private void addDottedArrow(int nodeIndex, boolean toUp) {

  }

  // update specified range
  private void update(int upNodeIndex, int downNodeIndex) {

  }
}
