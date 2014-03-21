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

package com.intellij.vcs.log.facade.graph;

import com.intellij.vcs.log.facade.api.graph.LinearGraphWithCommitInfo;
import com.intellij.vcs.log.facade.api.graph.LinearGraphWithHiddenNodes;
import com.intellij.vcs.log.facade.graph.permanent.PermanentCommitsInfo;
import com.intellij.vcs.log.newgraph.PermanentGraphLayout;
import org.jetbrains.annotations.NotNull;

public class GraphWithHiddenNodesAsGraphWithCommitInfo extends GraphWithHiddenNodesAsPrintedGraph implements LinearGraphWithCommitInfo {

  @NotNull
  private final PermanentCommitsInfo myPermanentCommitsInfo;

  public GraphWithHiddenNodesAsGraphWithCommitInfo(@NotNull LinearGraphWithHiddenNodes delegateGraph,
                                                   @NotNull PermanentGraphLayout graphLayout,
                                                   @NotNull PermanentCommitsInfo permanentCommitsInfo) {
    super(delegateGraph, graphLayout);
    myPermanentCommitsInfo = permanentCommitsInfo;
  }

  @Override
  public int getHashIndex(int nodeIndex) {
    return myPermanentCommitsInfo.getHashIndex(getIndexInPermanentGraph(nodeIndex));
  }

  @Override
  public long getTimestamp(int nodeIndex) {
    return myPermanentCommitsInfo.getTimestamp(getIndexInPermanentGraph(nodeIndex));
  }
}
