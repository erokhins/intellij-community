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

import com.intellij.openapi.util.Pair;
import com.intellij.util.Function;
import com.intellij.vcs.log.graph.permanent.elements.GraphEdge;
import com.intellij.vcs.log.graph.permanent.elements.GraphEdgeType;
import com.intellij.vcs.log.graph.utils.IntIntMultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.intellij.vcs.log.graph.permanent.elements.GraphEdgeType.*;

public class GraphAdditionEdges {
  @NotNull
  private final Function<Integer, Integer> myGetNodeIndexById;
  @NotNull
  private final Function<Integer, Integer> myGetNodeIdByIndex;
  @NotNull
  private final IntIntMultiMap myAdditionEdges = new IntIntMultiMap();

  public GraphAdditionEdges(@NotNull Function<Integer, Integer> getNodeIndexById, @NotNull Function<Integer, Integer> getNodeIdByIndex) {
    myGetNodeIndexById = getNodeIndexById;
    myGetNodeIdByIndex = getNodeIdByIndex;
  }


  public void createEdge(@NotNull GraphEdge graphEdge) {
    Pair<Integer, Integer> nodeIds = getNodeIds(graphEdge);
    createEdge(nodeIds.first, nodeIds.second, graphEdge.getType());
  }

  public void createEdge(int mainNodeId, int additionId, GraphEdgeType edgeType) {
    if (edgeType.isEdge()) {
      myAdditionEdges.putValue(mainNodeId, compactEdge(additionId, edgeType));
      myAdditionEdges.putValue(additionId, compactEdge(mainNodeId, edgeType));
    } else {
      myAdditionEdges.putValue(mainNodeId, compactEdge(additionId, edgeType));
    }
  }

  public void removeEdge(@NotNull GraphEdge graphEdge) {
    Pair<Integer, Integer> nodeIds = getNodeIds(graphEdge);
    removeEdge(nodeIds.first, nodeIds.second, graphEdge.getType());
  }

  public void removeEdge(int mainNodeId, int additionId, GraphEdgeType edgeType) {
    if (edgeType.isEdge()) {
      myAdditionEdges.remove(mainNodeId, compactEdge(additionId, edgeType));
      myAdditionEdges.remove(additionId, compactEdge(mainNodeId, edgeType));
    } else {
      myAdditionEdges.remove(mainNodeId, compactEdge(additionId, edgeType));
    }
  }

  public void addToResultAdditionEdges(List<GraphEdge> result, int nodeIndex, boolean toDown) {
    for (int compactEdge : myAdditionEdges.get(myGetNodeIdByIndex.fun(nodeIndex))) {
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
        int anotherNodeIndex = myGetNodeIndexById.fun(retrievedId);
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

  @NotNull
  private Pair<Integer, Integer> getNodeIds(@NotNull GraphEdge graphEdge) {
    if (graphEdge.getUpNodeIndex() != null) {
      Integer mainId = myGetNodeIdByIndex.fun(graphEdge.getUpNodeIndex());
      if (graphEdge.getDownNodeIndex() != null) {
        return Pair.create(mainId, myGetNodeIdByIndex.fun(graphEdge.getDownNodeIndex()));
      } else {
        assert graphEdge.getAdditionInfo() != null;
        return Pair.create(mainId, graphEdge.getAdditionInfo());
      }
    } else {
      assert graphEdge.getDownNodeIndex() != null && graphEdge.getAdditionInfo() != null;
      return Pair.create(myGetNodeIdByIndex.fun(graphEdge.getDownNodeIndex()), graphEdge.getAdditionInfo());
    }
  }

  private static int compactEdge(int nodeIndex, GraphEdgeType edgeType) {
    assert nodeIndex < 0xffffff || nodeIndex > 0x80ffffff;
    byte type = edgeType.getType();
    return (type << 24) | (0xffffff & nodeIndex);
  }

  @NotNull
  private static GraphEdgeType retrievedType(int compactEdge) {
    int type = compactEdge >> 24; // not important >> or >>>
    return GraphEdgeType.getByType((byte) type);
  }

  private static int retrievedNodeIndex(int compactEdge) {
    return (compactEdge << 8) >> 8;
  }

}
