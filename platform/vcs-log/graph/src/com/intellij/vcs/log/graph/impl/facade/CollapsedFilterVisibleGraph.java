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
package com.intellij.vcs.log.graph.impl.facade;

import com.intellij.openapi.util.Condition;
import com.intellij.util.Consumer;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcs.log.graph.actions.GraphAnswer;
import com.intellij.vcs.log.graph.api.elements.GraphEdge;
import com.intellij.vcs.log.graph.api.elements.GraphElement;
import com.intellij.vcs.log.graph.api.permanent.PermanentGraphInfo;
import com.intellij.vcs.log.graph.api.printer.PrintElementsManager;
import com.intellij.vcs.log.graph.impl.print.AbstractPrintElementsManager;
import com.intellij.vcs.log.graph.impl.print.CollapsedFilterPrintElementsManager;
import com.intellij.vcs.log.graph.impl.visible.CoolFilterGraphWithHiddenNodes;
import com.intellij.vcs.log.graph.impl.visible.FilterFragmentGenerator;
import com.intellij.vcs.log.graph.impl.visible.adapters.GraphWithHiddenNodesAsGraphWithCommitInfo;
import com.intellij.vcs.log.graph.impl.visible.adapters.LinearGraphAsGraphWithHiddenNodes;
import com.intellij.vcs.log.graph.utils.IntToIntMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class CollapsedFilterVisibleGraph<CommitId> extends AbstractVisibleGraph<CommitId> {

  @NotNull
  public static <CommitId> CollapsedFilterVisibleGraph<CommitId> newInstance(@NotNull final PermanentGraphInfo<CommitId> permanentGraph,
                                                                       @Nullable Set<CommitId> heads,
                                                                       @NotNull final Condition<CommitId> filter) {
    LinearGraphAsGraphWithHiddenNodes branchesGraph = createBranchesGraph(permanentGraph, heads);
    final Condition<Integer> isFilterNode = new Condition<Integer>() {
      @Override
      public boolean value(Integer integer) {
        CommitId commitId = permanentGraph.getPermanentCommitsInfo().getCommitId(integer);
        return filter.value(commitId);
      }
    };

    CoolFilterGraphWithHiddenNodes collapsedGraph = new CoolFilterGraphWithHiddenNodes(branchesGraph, isFilterNode);

    final GraphWithHiddenNodesAsGraphWithCommitInfo<CommitId> graphWithCommitInfo =
      new GraphWithHiddenNodesAsGraphWithCommitInfo<CommitId>(collapsedGraph, permanentGraph.getPermanentGraphLayout(),
                                                              permanentGraph.getPermanentCommitsInfo());

    final Condition<Integer> notCollapsedNodes = permanentGraph.getNotCollapsedNodes();
    FilterFragmentGenerator fragmentGeneratorForPrinterGraph = new FilterFragmentGenerator(graphWithCommitInfo, new Condition<Integer>() {
      @Override
      public boolean value(Integer integer) {
        int longIndex = graphWithCommitInfo.getIntToIntMap().getLongIndex(integer);
        return isFilterNode.value(longIndex);
      }
    }, new Condition<Integer>() {
      @Override
      public boolean value(Integer integer) {
        int longIndex = graphWithCommitInfo.getIntToIntMap().getLongIndex(integer);
        return notCollapsedNodes.value(longIndex);
      }
    });

    CollapsedFilterPrintElementsManager<CommitId> printElementsManager = new CollapsedFilterPrintElementsManager<CommitId>(graphWithCommitInfo,
                                                                                           permanentGraph.getGraphColorManager(),
                                                                                           fragmentGeneratorForPrinterGraph);
    return new CollapsedFilterVisibleGraph<CommitId>(graphWithCommitInfo, collapsedGraph, printElementsManager, fragmentGeneratorForPrinterGraph, permanentGraph);
  }

  @NotNull
  private final CoolFilterGraphWithHiddenNodes myCoolFilterGraph;

  @NotNull
  private final FilterFragmentGenerator myFragmentGeneratorForPrinterGraph;

  @NotNull
  private final IntToIntMap myIntToIntMap;

  @NotNull
  private final PermanentGraphInfo<CommitId> myPermanentGraph;

  private CollapsedFilterVisibleGraph(@NotNull GraphWithHiddenNodesAsGraphWithCommitInfo<CommitId> linearGraphWithCommitInfo,
                                      @NotNull CoolFilterGraphWithHiddenNodes coolFilterGraph,
                                      @NotNull PrintElementsManager printElementsManager,
                                      @NotNull FilterFragmentGenerator fragmentGeneratorForPrinterGraph,
                                      @NotNull PermanentGraphInfo<CommitId> permanentGraph) {
    super(linearGraphWithCommitInfo, permanentGraph.getCommitsWithNotLoadParent(), printElementsManager);
    myCoolFilterGraph = coolFilterGraph;
    myFragmentGeneratorForPrinterGraph = fragmentGeneratorForPrinterGraph;
    myPermanentGraph = permanentGraph;
    myIntToIntMap = linearGraphWithCommitInfo.getIntToIntMap();
  }

  @Override
  protected void setLinearBranchesExpansion(boolean collapse) {
    // todo
  }

  @NotNull
  @Override
  protected GraphAnswer<CommitId> clickByElement(@NotNull GraphElement graphElement) {
    GraphEdge graphEdge = AbstractPrintElementsManager.containedCollapsedEdge(graphElement, myLinearGraphWithCommitInfo);
    if (graphEdge != null) {
      int upLongIndex = myIntToIntMap.getLongIndex(graphEdge.getUpNodeIndex());
      int downLongIndex = myIntToIntMap.getLongIndex(graphEdge.getDownNodeIndex());

      myCoolFilterGraph.expand(upLongIndex, downLongIndex);
      return createJumpAnswer(graphEdge.getUpNodeIndex());
    }

    FilterFragmentGenerator.FilterFragment relativeFragment = myFragmentGeneratorForPrinterGraph.getFragmentForCollapse(graphElement);
    if (relativeFragment != null) {
      int upLongIndex = myIntToIntMap.getLongIndex(relativeFragment.upNodeIndex);
      int downLongIndex = myIntToIntMap.getLongIndex(relativeFragment.downNodeIndex);

      Set<Integer> intermediateNodes = myFragmentGeneratorForPrinterGraph.getIntermediateNodes(relativeFragment);
      List<Integer> intermediateLongIndex = ContainerUtil.map(intermediateNodes, new Function<Integer, Integer>() {
        @Override
        public Integer fun(Integer integer) {
          return myIntToIntMap.getLongIndex(integer);
        }
      });
      for (int node : intermediateLongIndex) {
        myCoolFilterGraph.setNodeVisibility(node, false);
      }
      myCoolFilterGraph.updateCollapsedEdges(upLongIndex, downLongIndex);
      return createJumpAnswer(relativeFragment.upNodeIndex);
    }

    return COMMIT_ID_GRAPH_ANSWER;
  }
}
