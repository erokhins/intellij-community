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
package com.intellij.vcs.log.graph.impl.print;

import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcs.log.graph.GraphColorManager;
import com.intellij.vcs.log.graph.api.LinearGraphWithCommitInfo;
import com.intellij.vcs.log.graph.api.elements.GraphElement;
import com.intellij.vcs.log.graph.impl.visible.FilterFragmentGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

public class CollapsedFilterPrintElementsManager<CommitId> extends AbstractPrintElementsManager<CommitId> {

  @NotNull
  private final FilterFragmentGenerator myFragmentGenerator;

  public CollapsedFilterPrintElementsManager(@NotNull LinearGraphWithCommitInfo<CommitId> printedLinearGraph,
                                                @NotNull GraphColorManager<CommitId> colorManager,
                                                @NotNull FilterFragmentGenerator fragmentGenerator) {
    super(printedLinearGraph, colorManager);
    myFragmentGenerator = fragmentGenerator;
  }

  @NotNull
  @Override
  protected Set<Integer> getSelectedNodes(@NotNull GraphElement graphElement) {
    FilterFragmentGenerator.FilterFragment fragmentForCollapse = myFragmentGenerator.getFragmentForCollapse(graphElement);
    if (fragmentForCollapse == null)
      return Collections.emptySet();

    final Set<Integer> selectedNodes = myFragmentGenerator.getIntermediateNodes(fragmentForCollapse);
    selectedNodes.add(fragmentForCollapse.upNodeIndex);
    selectedNodes.add(fragmentForCollapse.downNodeIndex);

    return selectedNodes;
  }
}
