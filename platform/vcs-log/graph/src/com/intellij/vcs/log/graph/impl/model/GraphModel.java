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

import com.intellij.vcs.log.graph.permanent.LinearGraph;
import com.intellij.vcs.log.graph.permanent.elements.GraphEdgeType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface GraphModel {

  @NotNull
  LinearGraph getDelegateGraph();
  @NotNull
  LinearGraph getCompiledGraph();

  // delegate node ids is non negative
  void setVisibilityDelegateNodes(@NotNull Collection<Integer> ids, boolean visibility);

  int getNodeId(int nodeIndex);

  // return -1 if node hide, or non exist
  int getNodeIndex(int nodeId);

  // insertNodeId < -1. If you need insert node before all another, use prevNodeId = -1
  void addNode(int prevNodeId, int insertNodeId);
  // nodeId < -1
  void removeNode(int nodeId);

  void addEdge(int nodeId1, int nodeId2, GraphEdgeType edgeType);
  void removeEdge(int nodeId1, int nodeId2, GraphEdgeType edgeType);
}
