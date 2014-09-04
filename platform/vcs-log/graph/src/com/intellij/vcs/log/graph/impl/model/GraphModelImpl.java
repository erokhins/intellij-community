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
package com.intellij.vcs.log.graph.impl.model;

import com.intellij.util.SmartList;
import com.intellij.vcs.log.graph.permanent.LinearGraph;
import com.intellij.vcs.log.graph.permanent.elements.GraphEdge;
import com.intellij.vcs.log.graph.permanent.elements.GraphEdgeType;
import com.intellij.vcs.log.graph.utils.Flags;
import com.intellij.vcs.log.graph.utils.IntIntMultiMap;
import com.intellij.vcs.log.graph.utils.UpdatableIntToIntMap;
import com.intellij.vcs.log.graph.utils.impl.BitSetFlags;
import com.intellij.vcs.log.graph.utils.impl.ListIntToIntMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import static com.intellij.vcs.log.graph.permanent.elements.GraphEdgeType.*;
import static com.intellij.vcs.log.graph.impl.model.EdgeTypeConverter.*;

public class GraphModelImpl implements GraphModel {
  @NotNull
  private final LinearGraph myDelegateGraph;
  @NotNull
  private final NegativeNodeManager myNegativeNodeManager = new NegativeNodeManager();
  @NotNull
  private final Flags myVisibleDelegateNodes;
  @NotNull
  private final UpdatableIntToIntMap myDelegateIntToIntMap;
  @NotNull
  private final IntIntMultiMap myAdditionEdges = new IntIntMultiMap();
  @NotNull
  private final CompiledGraph myCompiledGraph = new CompiledGraph();


  public GraphModelImpl(@NotNull LinearGraph delegateGraph) {
    myDelegateGraph = delegateGraph;
    myVisibleDelegateNodes = new BitSetFlags(delegateGraph.nodesCount());
    myDelegateIntToIntMap = ListIntToIntMap.newInstance(myVisibleDelegateNodes);
  }

  @NotNull
  @Override
  public LinearGraph getDelegateGraph() {
    return myDelegateGraph;
  }

  @NotNull
  @Override
  public LinearGraph getCompiledGraph() {
    return myCompiledGraph;
  }

  @Override
  public void setVisibilityDelegateNodes(@NotNull Collection<Integer> ids, boolean visibility) {
    int minId = Integer.MAX_VALUE;
    int maxId = Integer.MIN_VALUE;
    for (int id : ids) {
      checkPositiveId(id);
      minId = Math.min(minId, id);
      maxId = Math.max(maxId, id);
      myVisibleDelegateNodes.set(id, visibility);
    }
    update(minId, maxId);
  }

  @Override
  public int getNodeId(int nodeIndex) {
    int delegateIndex = nodeIndex;
    if (nodeIndex >= myDelegateGraph.nodesCount())
      nodeIndex = myDelegateGraph.nodesCount() - 1;
    while (delegateIndex >= 0 && delegateIndex + myNegativeNodeManager.countNegativeNodesBefore(delegateIndex) > nodeIndex)
      delegateIndex--;

    if (delegateIndex == -1)
      return myNegativeNodeManager.getNegativeNodeId(new NegativeNodeManager.NegativeNodeInfo(-1, nodeIndex));

    int delta = nodeIndex - delegateIndex - myNegativeNodeManager.countNegativeNodesBefore(delegateIndex);
    if (delta == 0)
      return delegateIndex;
    else
      return myNegativeNodeManager.getNegativeNodeId(new NegativeNodeManager.NegativeNodeInfo(delegateIndex, delta - 1));
  }

  @Override
  public int getNodeIndex(int nodeId) {
    if (nodeId >= 0) {
      if (!myVisibleDelegateNodes.get(nodeId))
        return -1;
      int nodeIndex = myDelegateIntToIntMap.getShortIndex(nodeId);
      return myNegativeNodeManager.countNegativeNodesBefore(nodeId) + nodeIndex;
    } else {
      NegativeNodeManager.NegativeNodeInfo negativeNodeInfo = myNegativeNodeManager.getNegativeNodeInfo(nodeId);
      if (negativeNodeInfo == null)
        return -1;

      int positiveNodeBefore = negativeNodeInfo.getPositiveNodeBefore();
      if (positiveNodeBefore == -1)
        return negativeNodeInfo.getIndex();

      int nodeIndex = myDelegateIntToIntMap.getShortIndex(positiveNodeBefore);
      nodeIndex += myNegativeNodeManager.countNegativeNodesBefore(positiveNodeBefore);
      return nodeIndex + negativeNodeInfo.getIndex() + 1;
    }
  }

  @Override
  public void addNode(int afterIndex, int insertNodeId) {
    checkNegativeId(insertNodeId);
    myNegativeNodeManager.addNegativeNode(insertNodeId, afterIndex);
  }

  @Override
  public void removeNode(int nodeId) {
    checkNegativeId(nodeId);
    myNegativeNodeManager.removeNegativeNode(nodeId);
  }

  @Override
  public void addEdge(int nodeId1, int nodeId2, GraphEdgeType edgeType) {
    checkAdditionEdgeType(edgeType);
    myAdditionEdges.putValue(nodeId1, compactEdge(nodeId2, edgeType));
    myAdditionEdges.putValue(nodeId2, compactEdge(nodeId1, edgeType));
  }

  @Override
  public void removeEdge(int nodeId1, int nodeId2, GraphEdgeType edgeType) {
    checkAdditionEdgeType(edgeType);
    myAdditionEdges.remove(nodeId1, compactEdge(nodeId2, edgeType));
    myAdditionEdges.remove(nodeId2, compactEdge(nodeId1, edgeType));
  }

  private void update(int startDelegateId, int endDelegateId) {
    myDelegateIntToIntMap.update(startDelegateId, endDelegateId);
  }

  private static void checkAdditionEdgeType(GraphEdgeType edgeType) {
    assert !edgeType.isPermanentEdge();
  }
  private static void checkPositiveId(int id) {
    assert id >= 0;
  }
  private static void checkNegativeId(int id) {
    assert id < -1;
  }

  private class CompiledGraph implements LinearGraph {

    @Override
    public int nodesCount() {
      return myNegativeNodeManager.countNegativeNodes() + myDelegateIntToIntMap.shortSize();
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
      int nodeId = getNodeId(nodeIndex);
      if (nodeId >= 0) { // edges from delegate graph
        for (GraphEdge edge : myDelegateGraph.getUpEdges(nodeId)) {
          Integer upNodeId = edge.getUpNodeIndex();
          Integer upNodeIndex = null;
          if (upNodeId != null)
            upNodeIndex = getNodeIndex(upNodeId);

          result.add(new GraphEdge(upNodeIndex, nodeIndex, edge.getAdditionInfo(), edge.getType()));
        }
      }

      addToResultAdditionEdges(result, nodeIndex, nodeId, false);
      return result;
    }

    @NotNull
    @Override
    public List<GraphEdge> getDownEdges(int nodeIndex) {
      List<GraphEdge> result = new SmartList<GraphEdge>();
      int nodeId = getNodeId(nodeIndex);
      if (nodeId >= 0) { // edges from delegate graph
        for (GraphEdge edge : myDelegateGraph.getDownEdges(nodeId)) {
          Integer downNodeId = edge.getDownNodeIndex();
          Integer downNodeIndex = null;
          if (downNodeId != null)
            downNodeIndex = getNodeIndex(downNodeId);

          result.add(new GraphEdge(nodeIndex, downNodeIndex, edge.getAdditionInfo(), edge.getType()));
        }
      }

      addToResultAdditionEdges(result, nodeIndex, nodeId, true);
      return result;
    }

    private void addToResultAdditionEdges(List<GraphEdge> result, int nodeIndex, int nodeId, boolean toDown) {
      for (int compactEdge : myAdditionEdges.get(nodeId)) {
        GraphEdge edge = createEdge(nodeIndex, compactEdge, toDown);
        if (edge != null)
          result.add(edge);
      }
    }

    @Nullable
    private GraphEdge createEdge(int nodeIndex, int compactEdge, boolean toDown) {
      GraphEdgeType edgeType = retrievedType(compactEdge);
      int retrievedId = retrievedNodeIndex(compactEdge);
      switch (edgeType) {
        case DOTTED:
          int anotherNodeIndex = getNodeIndex(retrievedId);
          if (anotherNodeIndex == -1)
            return null;
          if (toDown && nodeIndex < anotherNodeIndex)
            return new GraphEdge(nodeIndex, anotherNodeIndex, DOTTED);
          if (!toDown && nodeIndex > anotherNodeIndex)
            return new GraphEdge(anotherNodeIndex, nodeIndex, DOTTED);
          return null;

        case DOTTED_ARROW_DOWN:
          if (toDown)
            return new GraphEdge(nodeIndex, null, retrievedId, DOTTED_ARROW_DOWN);
          else
            return null;

        case DOTTED_ARROW_UP:
          if (!toDown)
            return new GraphEdge(null, nodeIndex, retrievedId, DOTTED_ARROW_UP);
          else
            return null;

        default:
          return null;
      }
    }

  }
}
