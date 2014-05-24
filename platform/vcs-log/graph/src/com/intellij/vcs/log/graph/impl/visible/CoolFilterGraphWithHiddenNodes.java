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
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.vcs.log.graph.api.LinearGraphWithHiddenNodes;
import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import com.intellij.vcs.log.graph.api.elements.GraphNode;
import com.intellij.vcs.log.graph.utils.Flags;
import com.intellij.vcs.log.graph.utils.ListenerController;
import com.intellij.vcs.log.graph.utils.impl.BitSetFlags;
import com.intellij.vcs.log.graph.utils.impl.SetListenerController;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CoolFilterGraphWithHiddenNodes implements LinearGraphWithHiddenNodes {
  @NotNull
  private final LinearGraphWithHiddenNodes myDelegateGraph;

  @NotNull
  private final Condition<Integer> myIsFilterNode;
  @NotNull
  private final Flags myVisibleNodes;

  @NotNull
  private final MultiMap<Integer, Integer> myAdditionalEdges = new MultiMap<Integer, Integer>();

  @NotNull
  private final SetListenerController<LinearGraphWithHiddenNodes.UpdateListener>
    myListenerController = new SetListenerController<LinearGraphWithHiddenNodes.UpdateListener>();

  public CoolFilterGraphWithHiddenNodes(@NotNull LinearGraphWithHiddenNodes delegateGraph, @NotNull Condition<Integer> isFilterNode ) {
    myDelegateGraph = delegateGraph;
    myIsFilterNode = isFilterNode;

    myVisibleNodes = new BitSetFlags(myDelegateGraph.nodesCount());
    for (int i = 0; i < myDelegateGraph.nodesCount(); i++)
      myVisibleNodes.set(i, myDelegateGraph.nodeIsVisible(i) && isFilterNode.value(i));

    updateHideEdges();
    addUpdateListener();
  }

  private void addUpdateListener() {
    myDelegateGraph.getListenerController().addListener(new LinearGraphWithHiddenNodes.UpdateListener() {
      @Override
      public void update(final int upNodeIndex, final int downNodeIndex) {
        myListenerController.callListeners(new Consumer<LinearGraphWithHiddenNodes.UpdateListener>() {
          @Override
          public void consume(LinearGraphWithHiddenNodes.UpdateListener updateListener) {
            updateListener.update(upNodeIndex, downNodeIndex);
          }
        });
      }
    });
  }

  private void updateHideEdges() {
    myAdditionalEdges.clear();
    Condition<Integer> condition = new Condition<Integer>() {
      @Override
      public boolean value(Integer integer) {
        return nodeIsVisible(integer);
      }
    };
    FilterEdgesCreator filterEdgesCreator = new FilterEdgesCreator(myDelegateGraph, condition, new Consumer<Pair<Integer, Integer>>() {
      @Override
      public void consume(Pair<Integer, Integer> edge) {
        int upNode = edge.first;
        int downNode = edge.second;
        myAdditionalEdges.putValue(upNode, downNode);
        myAdditionalEdges.putValue(downNode, upNode);
      }
    });

    long l = System.currentTimeMillis();
    filterEdgesCreator.createEdges();
    System.out.println(System.currentTimeMillis() - l + ":" + nodesCount());
  }

  @Override
  public boolean nodeIsVisible(int nodeIndex) {
    return myVisibleNodes.get(nodeIndex);
  }

  @NotNull
  @Override
  public ListenerController<UpdateListener> getListenerController() {
    return myListenerController;
  }

  @NotNull
  @Override
  public GraphNode.Type getNodeType(int nodeIndex) {
    if (myIsFilterNode.value(nodeIndex))
      return GraphNode.Type.USUAL;
    else
      return GraphNode.Type.GRAY;
  }

  @NotNull
  @Override
  public GraphEdge.Type getEdgeType(int upNodeIndex, int downNodeIndex) {
    if (!myDelegateGraph.getDownNodes(upNodeIndex).contains(downNodeIndex))
      return GraphEdge.Type.HIDE;
    else
      return GraphEdge.Type.USUAL;
  }

  @Override
  public int nodesCount() {
    return myDelegateGraph.nodesCount();
  }

  @NotNull
  @Override
  public List<Integer> getUpNodes(int nodeIndex) {
    List<Integer> result = ContainerUtil.newSmartList();
    for (int upNode : myDelegateGraph.getUpNodes(nodeIndex)) {
      if (myVisibleNodes.get(upNode))
        result.add(upNode);
    }

    for (int node : myAdditionalEdges.get(nodeIndex)) {
      if (node < nodeIndex && nodeIsVisible(node) && !result.contains(node)) // todo nodeIsVisible
        result.add(node);
    }

    return result;
  }

  @NotNull
  @Override
  public List<Integer> getDownNodes(int nodeIndex) {
    List<Integer> result = ContainerUtil.newSmartList();
    for (int downNode : myDelegateGraph.getDownNodes(nodeIndex)) {
      if (myVisibleNodes.get(downNode))
        result.add(downNode);
    }

    for (int node : myAdditionalEdges.get(nodeIndex)) {
      if (node > nodeIndex && nodeIsVisible(node) && !result.contains(node)) // todo nodeIsVisible
        result.add(node);
    }

    return result;
  }

  public void setNodeVisibility(int nodeIndex, boolean visible) {
    myVisibleNodes.set(nodeIndex, visible);
  }

  @NotNull
  private static Collection<Integer> removeFromMultiMap(MultiMap<Integer, Integer> map, Integer removeIndex) {
    Collection<Integer> lastCollection = map.remove(removeIndex);
    if (lastCollection != null)
      return lastCollection;
    else
      return Collections.emptyList();
  }

  private void clearEdgesInInterval(int upNode, int downNode) {
    //for (int i = upNode; i <= downNode; i++) {
    //  for (int up : removeFromMultiMap(myUpperCollapsedEdges, i)) {
    //    myLowerCollapsedEdges.remove(up, i);
    //  }
    //  for (int down : removeFromMultiMap(myLowerCollapsedEdges, i)) {
    //    myUpperCollapsedEdges.remove(down, i);
    //  }
    //}
  }

  public void updateCollapsedEdges(final int upNodeIndex, final int downNodeIndex) {
    //clearEdgesInInterval(upNodeIndex, downNodeIndex);
    updateHideEdges();

    // TODO:
    myListenerController.callListeners(new Consumer<UpdateListener>() {
      @Override
      public void consume(UpdateListener updateListener) {
        updateListener.update(upNodeIndex, downNodeIndex);
      }
    });
  }

  public void expand(int upNode, int downNode) {
    FilterFragmentGenerator.doForIntermediateNodes(myDelegateGraph, upNode, downNode, new Consumer<Integer>() {
      @Override
      public void consume(Integer integer) {
        setNodeVisibility(integer, true);
      }
    });
    updateCollapsedEdges(upNode, downNode);
  }
}
