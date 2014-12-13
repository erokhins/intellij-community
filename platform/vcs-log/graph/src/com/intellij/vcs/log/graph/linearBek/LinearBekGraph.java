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
package com.intellij.vcs.log.graph.linearBek;

import com.intellij.vcs.log.graph.api.LinearGraph;
import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import com.intellij.vcs.log.graph.api.elements.GraphNode;
import com.intellij.vcs.log.graph.collapsing.GraphAdditionalEdges;
import com.intellij.vcs.log.graph.utils.LinearGraphUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LinearBekGraph implements LinearGraph {
  @NotNull private final LinearGraph myGraph;
  @NotNull private final GraphAdditionalEdges myHiddenEdges;
  @NotNull private final GraphAdditionalEdges myNewEdges;

  public LinearBekGraph(@NotNull LinearGraph graph, @NotNull GraphAdditionalEdges hiddenEdges, @NotNull GraphAdditionalEdges newEdges) {
    myGraph = graph;
    myHiddenEdges = hiddenEdges;
    myNewEdges = newEdges;
  }

  @Override
  public int nodesCount() {
    return myGraph.nodesCount();
  }

  @NotNull
  @Override
  public List<Integer> getUpNodes(int nodeIndex) {
    return LinearGraphUtils.getUpNodes(this, nodeIndex);
  }

  @NotNull
  @Override
  public List<Integer> getDownNodes(int nodeIndex) {
    return LinearGraphUtils.getDownNodes(this, nodeIndex);
  }

  @NotNull
  @Override
  public List<GraphEdge> getAdjacentEdges(int nodeIndex) {
    List<GraphEdge> result = new ArrayList<GraphEdge>();
    result.addAll(myGraph.getAdjacentEdges(nodeIndex));
    myHiddenEdges.removeAdditionalEdges(result, nodeIndex);
    myNewEdges.appendAdditionalEdges(result, nodeIndex);

    Collections.sort(result, new Comparator<GraphEdge>() {
      @Override
      public int compare(GraphEdge o1, GraphEdge o2) {
        return o1.getUpNodeIndex().compareTo(o2.getUpNodeIndex());
      }
    });

    return result;
  }

  @NotNull
  @Override
  public GraphNode getGraphNode(int nodeIndex) {
    return myGraph.getGraphNode(nodeIndex);
  }

  @Nullable
  @Override
  public Integer getNodeIndexById(int nodeId) {
    return myGraph.getNodeIndexById(nodeId);
  }
}
