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

package com.intellij.vcs.log.graph.permanent;

import com.intellij.util.SmartList;
import com.intellij.vcs.log.graph.permanent.elements.GraphEdge;
import com.intellij.vcs.log.graph.permanent.elements.GraphEdgeType;
import com.intellij.vcs.log.graph.permanent.elements.GraphNode;
import com.intellij.vcs.log.graph.utils.Flags;
import com.intellij.vcs.log.graph.utils.IntList;
import com.intellij.vcs.log.graph.utils.impl.CompressedIntList;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import static com.intellij.vcs.log.graph.permanent.elements.GraphEdgeType.USUAL;

public class PermanentLinearGraphImpl implements LinearGraph {
  private final Flags mySimpleNodes;

  // myNodeToEdgeIndex.length = nodesCount() + 1.
  private final IntList myNodeToEdgeIndex;
  private final IntList myLongEdges;

  /*package*/ PermanentLinearGraphImpl(Flags simpleNodes, int[] nodeToEdgeIndex, int[] longEdges) {
    mySimpleNodes = simpleNodes;
    myNodeToEdgeIndex = CompressedIntList.newInstance(nodeToEdgeIndex);
    myLongEdges = CompressedIntList.newInstance(longEdges);
  }

  @Override
  public int nodesCount() {
    return mySimpleNodes.size();
  }

  @NotNull
  @Override
  public List<Integer> getUpNodes(int nodeIndex) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public List<Integer> getDownNodes(int nodeIndex) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public List<GraphEdge> getUpEdges(int nodeIndex) {
    List<GraphEdge> result = new SmartList<GraphEdge>();
    if (nodeIndex != 0 && mySimpleNodes.get(nodeIndex - 1)) {
      result.add(new GraphEdge(nodeIndex - 1, nodeIndex, USUAL));
    }

    for (int i = myNodeToEdgeIndex.get(nodeIndex); i < myNodeToEdgeIndex.get(nodeIndex + 1); i++) {
      int node = myLongEdges.get(i);
      if (node >= 0 && node < nodeIndex)
        result.add(new GraphEdge(node, nodeIndex, USUAL));
    }

    return result;
  }

  @NotNull
  @Override
  public List<GraphEdge> getDownEdges(int nodeIndex) {
    if (mySimpleNodes.get(nodeIndex)) {
      return Collections.singletonList(new GraphEdge(nodeIndex, nodeIndex + 1, USUAL));
    }

    List<GraphEdge> result = new SmartList<GraphEdge>();
    for (int i = myNodeToEdgeIndex.get(nodeIndex); i < myNodeToEdgeIndex.get(nodeIndex + 1); i++) {
      int node = myLongEdges.get(i);
      if (node < 0)
        result.add(new GraphEdge(nodeIndex, null, node, GraphEdgeType.NOT_LOAD_COMMIT));
      else if (nodeIndex < node)
        result.add(new GraphEdge(nodeIndex, node, USUAL));
    }

    return result;
  }

  @NotNull
  @Override
  public GraphNode getGraphNode(int nodeIndex) {
    return new GraphNode(nodeIndex);
  }

  @Override
  public int getNodeIndexById(int nodeId) {
    if (nodeId >= 0 && nodeId < nodesCount())
      return nodeId;
    else
      return -1;
  }
}
