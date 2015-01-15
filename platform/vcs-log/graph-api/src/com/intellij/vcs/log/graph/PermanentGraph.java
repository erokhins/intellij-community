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
package com.intellij.vcs.log.graph;

import com.intellij.openapi.util.Condition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * PermanentGraph is created once per repository, and forever until the log is refreshed. <br/>
 * An instance can be achieved by {@link PermanentGraphBuilder}. <br/>
 * This graph contains all commits in the log and may occupy a lot.
 *
 * @see VisibleGraph
 */
public interface PermanentGraph<CommitId> {

  @NotNull
  VisibleGraph<CommitId> createVisibleGraph(@NotNull SortType sortType,
                                            @Nullable Set<CommitId> headsOfVisibleBranches,
                                            @Nullable Condition<CommitId> filter); // todo Set

  @NotNull
  List<GraphCommit<CommitId>> getAllCommits();

  @NotNull
  List<CommitId> getChildren(@NotNull CommitId commit);

  @NotNull
  Set<CommitId> getContainingBranches(@NotNull CommitId commit);

  enum SortType{
    Normal,
    Bek
  }
}
