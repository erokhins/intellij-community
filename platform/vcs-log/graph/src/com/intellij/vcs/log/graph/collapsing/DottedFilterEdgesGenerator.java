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

import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcs.log.graph.api.LiteLinearGraph;
import com.intellij.vcs.log.graph.api.LiteLinearGraph.NodeFilter;
import com.intellij.vcs.log.graph.utils.LinearGraphUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.vcs.log.graph.api.elements.GraphEdgeType.*;
import static com.intellij.vcs.log.graph.collapsing.GraphAdditionalEdges.NULL_ID;

public class DottedFilterEdgesGenerator {
  public void update(@NotNull CollapsedGraph collapsedGraph, int upDelegateNodeIndex, int downDelegateNodeIndex) {
    new DottedFilterEdgesGenerator(collapsedGraph, upDelegateNodeIndex, downDelegateNodeIndex).update();
  }

  @NotNull private final CollapsedGraph myCollapsedGraph;

  @NotNull private final LiteLinearGraph myLiteDelegateGraph;

  private final int myUpIndex;
  private final int myDownIndex;
  @NotNull private final int[] myNumbers;

  private DottedFilterEdgesGenerator(@NotNull CollapsedGraph collapsedGraph, int upIndex, int downIndex) {
    myCollapsedGraph = collapsedGraph;
    myLiteDelegateGraph = LinearGraphUtils.asLiteLinearGraph(collapsedGraph.getDelegateGraph());
    myUpIndex = upIndex;
    myDownIndex = downIndex;
    myNumbers = new int[downIndex - upIndex + 1];
  }

  private int getNodeId(int nodeIndex) {
    return myCollapsedGraph.getDelegateGraph().getGraphNode(nodeIndex).getNodeId();
  }

  private void addDottedEdge(int nodeIndex1, int nodeIndex2) {
    myCollapsedGraph.getGraphAdditionalEdges().createEdge(getNodeId(nodeIndex1), getNodeId(nodeIndex2), DOTTED);
  }

  private void addDottedArrow(int nodeIndex, boolean toUp) {
    myCollapsedGraph.getGraphAdditionalEdges().createEdge(getNodeId(nodeIndex), NULL_ID, toUp ? DOTTED_ARROW_UP : DOTTED_ARROW_DOWN);
  }

  // update specified range
  private void update() {
    computeEdges(new DataDispatcher(false));
    computeEdges(new DataDispatcher(true));
  }

  private void computeEdges(DataDispatcher dispatcher) {
    for (int shiftIndex = 0; shiftIndex < myNumbers.length; shiftIndex++) {
      if (dispatcher.nodeIsVisible(shiftIndex)) {
        myNumbers[shiftIndex] = computeVisibleNodeNumber(shiftIndex, dispatcher);
      }
      else {
        myNumbers[shiftIndex] = computeInvisibleNodeNumber(shiftIndex, dispatcher);
      }
    }
  }

  private int computeVisibleNodeNumber(int shiftIndex, DataDispatcher dispatcher) {
    int nearlyVisibleNode = Integer.MIN_VALUE;
    int maxAdjacentNumber = Integer.MIN_VALUE;
    for (int prevShiftIndex : dispatcher.getPrevNodes(shiftIndex)) {
      if (prevShiftIndex < 0) {

      }
      if (dispatcher.nodeIsVisible(prevShiftIndex)) {
        maxAdjacentNumber = Math.max(maxAdjacentNumber, myNumbers[prevShiftIndex]);
      } else {
        nearlyVisibleNode = Math.max(nearlyVisibleNode, myNumbers[prevShiftIndex]);
      }
    }

    if (nearlyVisibleNode != maxAdjacentNumber && nearlyVisibleNode == Integer.MIN_VALUE) {
      addDottedEdge(dispatcher.getNodeIndex(shiftIndex), dispatcher.getNodeIndex(nearlyVisibleNode));
    }

    return nearlyVisibleNode;
  }

  private int computeInvisibleNodeNumber(int nodeIndex, DataDispatcher dispatcher) {
    //todo
    return 0;
  }

  class DataDispatcher {
    private final boolean myToUp;

    DataDispatcher(boolean toUp) {
      this.myToUp = toUp;
    }

    private int getShiftIndex(int nodeIndex) {
      return myToUp ? myDownIndex - nodeIndex : nodeIndex - myUpIndex;
    }

    private int getNodeIndex(int shiftIndex) {
      return myToUp ? myDownIndex - shiftIndex : myUpIndex + shiftIndex;
    }

    private List<Integer> getPrevNodes(int shiftIndex) {
      return ContainerUtil.map(myLiteDelegateGraph.getNodes(getNodeIndex(shiftIndex), NodeFilter.filter(!myToUp)), new Function<Integer, Integer>() {
        @Override
        public Integer fun(Integer nodeIndex) {
          return getShiftIndex(nodeIndex);
        }
      });
    }

    private boolean nodeIsVisible(int shiftNodeIndex) {
      int nodeIndex = getNodeIndex(shiftNodeIndex);
      return myCollapsedGraph.getNodeVisibility(getNodeId(nodeIndex));
    }

  }

}
