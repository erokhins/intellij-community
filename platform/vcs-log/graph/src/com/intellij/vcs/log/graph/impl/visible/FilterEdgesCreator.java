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
import com.intellij.openapi.util.Pair;
import com.intellij.util.Consumer;
import com.intellij.vcs.log.graph.api.LinearGraphWithHiddenNodes;
import org.jetbrains.annotations.NotNull;


public class FilterEdgesCreator {
  @NotNull
  private final LinearGraphWithHiddenNodes myGraph;

  @NotNull
  private final Condition<Integer> myNodeIsVisible;

  @NotNull
  private final Consumer<Pair<Integer, Integer>> myEdgesCreator;

  private int[] myNumbers;

  public FilterEdgesCreator(@NotNull LinearGraphWithHiddenNodes graph, @NotNull Condition<Integer> nodeIsVisible, @NotNull Consumer<Pair<Integer, Integer>> edgesCreator) {
    myGraph = graph;
    myNodeIsVisible = nodeIsVisible;
    myEdgesCreator = edgesCreator;
  }

  public void createEdges() {
    myNumbers = new int[myGraph.nodesCount()];
    downWalk();
    upWalk();
  }

  private void createEdge(int upNode, int downNode) {
    myEdgesCreator.consume(new Pair<Integer, Integer>(upNode, downNode));
  }


  private void downWalk() {
    for (int  node = 0; node < myGraph.nodesCount(); node++ ) {
      if (!myGraph.nodeIsVisible(node))
        continue;

      if (myNodeIsVisible.value(node)) {
        myNumbers[node] = node;
        for (int upNode : myGraph.getUpNodes(node)) {
          int myUpNumber = myNumbers[upNode];
          if (myUpNumber >= 0)
            createEdge(myUpNumber, node);
          else {
            createEdge(-myUpNumber, node);
          }
        }
      } else {
        int nearUpNode = Integer.MIN_VALUE;
        for (int upNode : myGraph.getUpNodes(node)) {
          nearUpNode = Math.max(nearUpNode, myNumbers[upNode]);
        }
        if (nearUpNode != Integer.MIN_VALUE)
          myNumbers[node] = nearUpNode;
        else
          myNumbers[node] = -node; // todo branch start
      }
    }
  }

  private void upWalk() {
    for (int node = myGraph.nodesCount() - 1; node >= 0; node--) {
      if (!myGraph.nodeIsVisible(node))
        continue;

      if (myNodeIsVisible.value(node)) {
        myNumbers[node] = node;
        for (int downNode : myGraph.getDownNodes(node)) {
          if (downNode == Integer.MAX_VALUE)
            continue;
          int myDownNumber = myNumbers[downNode];
          if (myDownNumber != Integer.MAX_VALUE) {
            createEdge(node, myDownNumber);
          }
        }
      } else {
        int nearDownNode = Integer.MAX_VALUE;
        for (int downNode : myGraph.getDownNodes(node)) {
          if (downNode == Integer.MAX_VALUE)
            continue;
          nearDownNode = Math.min(nearDownNode, myNumbers[downNode]);
        }
        if (nearDownNode != Integer.MAX_VALUE)
          myNumbers[node] = nearDownNode;
        else
          myNumbers[node] = Integer.MAX_VALUE; //todo
      }
    }
  }
}
