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

import com.intellij.util.containers.IntIntHashMap;
import gnu.trove.TIntArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class NegativeNodeManager {
  private static final int NONEXISTENT_NODE = Integer.MIN_VALUE;

  private final TIntArrayList myNegativeNodeIds = new TIntArrayList();
  private final TIntArrayList myPrevPositiveNode = new TIntArrayList();
  private final IntIntHashMap myNegativeNodeToPositiveNodeBefore = new IntIntHashMap(4, NONEXISTENT_NODE);

  public int countNegativeNodesBefore(int positiveNodeId) {
    assert positiveNodeId >= 0;
    int orderNumber = myPrevPositiveNode.binarySearch(positiveNodeId - 1);
    if (orderNumber < 0)
      return -orderNumber - 1;

    orderNumber = toUp(orderNumber);

    return orderNumber + 1;
  }

  private int toUp(int nodeOrderNumber) {
    for (int i = nodeOrderNumber; i < myPrevPositiveNode.size() - 1; i++)
      if (myPrevPositiveNode.get(nodeOrderNumber) != myPrevPositiveNode.get(i))
        return i - 1;
    return myPrevPositiveNode.size();
  }

  private int toDown(int nodeOrderNumber) {
    for (int i = nodeOrderNumber; i > 0; i--)
      if (myPrevPositiveNode.get(nodeOrderNumber) != myPrevPositiveNode.get(i))
        return i + 1;
    return 0;
  }

  @Nullable
  private Integer getOrderNumber(int negativeNodeId) {
    int positiveNodeBefore = myNegativeNodeToPositiveNodeBefore.get(negativeNodeId);
    if (positiveNodeBefore == NONEXISTENT_NODE)
      return null;

    int orderNumber = myPrevPositiveNode.binarySearch(positiveNodeBefore);
    assert orderNumber >= 0;
    orderNumber = myNegativeNodeIds.indexOf(toDown(orderNumber), negativeNodeId);
    assert orderNumber >= 0;
    return orderNumber;
  }

  // null if negativeNodeId not existed
  @Nullable
  public NegativeNodeInfo getNegativeNodeInfo(int negativeNodeId) {
    Integer orderNumber = getOrderNumber(negativeNodeId);
    if (orderNumber == null)
      return null;
    return new NegativeNodeInfo(myPrevPositiveNode.get(orderNumber), orderNumber - toDown(orderNumber));
  }

  public int getNegativeNodeId(@NotNull NegativeNodeInfo nodeInfo) {
    int index = myPrevPositiveNode.binarySearch(nodeInfo.getPositiveNodeBefore());
    assert index >= 0;

    index = toDown(index);
    int orderNumber = index + nodeInfo.getIndex();
    assert myPrevPositiveNode.get(index) == myPrevPositiveNode.get(orderNumber);

    return myNegativeNodeIds.get(orderNumber);
  }

  public void removeNegativeNode(int negativeNodeId) {
    Integer orderNumber = getOrderNumber(negativeNodeId);
    if (orderNumber == null) return;

    myNegativeNodeToPositiveNodeBefore.remove(negativeNodeId);
    myPrevPositiveNode.remove(orderNumber);
    myNegativeNodeIds.remove(orderNumber);
  }

  public void addNegativeNode(int negativeNodeId, int prevNodeId) {
    assert negativeNodeId < -1;
    if (prevNodeId < -1) {
      Integer prevOrderNumber = getOrderNumber(negativeNodeId);
      assert prevOrderNumber != null;
      insertNode(negativeNodeId, prevOrderNumber + 1, myPrevPositiveNode.get(prevOrderNumber));
    } else {
      int orderNumber = myPrevPositiveNode.binarySearch(prevNodeId);
      if (orderNumber < 0)
        insertNode(negativeNodeId, -orderNumber - 1, prevNodeId);
      else
        insertNode(negativeNodeId, toDown(orderNumber), prevNodeId);
    }
  }

  private void insertNode(int nodeId, int orderNumber, int prevPositiveNumber) {
    assert orderNumber >= myNegativeNodeIds.size() || myNegativeNodeIds.get(orderNumber) != nodeId;
    myNegativeNodeIds.insert(orderNumber, nodeId);
    myPrevPositiveNode.insert(orderNumber, prevPositiveNumber);
    myNegativeNodeToPositiveNodeBefore.put(nodeId, prevPositiveNumber);
  }

  public int countNegativeNodes() {
    return myNegativeNodeIds.size();
  }

  public static class NegativeNodeInfo {
    private final int myPositiveNodeBefore;
    private final int myIndex; // count nodes between this and positiveNodeBefore

    public NegativeNodeInfo(int positiveNodeBefore, int index) {
      myPositiveNodeBefore = positiveNodeBefore;
      myIndex = index;
    }

    public int getPositiveNodeBefore() {
      return myPositiveNodeBefore;
    }

    public int getIndex() {
      return myIndex;
    }
  }
}
