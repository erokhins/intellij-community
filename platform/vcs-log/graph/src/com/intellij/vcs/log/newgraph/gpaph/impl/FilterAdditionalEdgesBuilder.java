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

package com.intellij.vcs.log.newgraph.gpaph.impl;

import com.intellij.util.containers.MultiMap;
import com.intellij.vcs.log.facade.utils.Flags;
import com.intellij.vcs.log.newgraph.PermanentGraph;
import com.intellij.vcs.log.newgraph.SomeGraph;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class FilterAdditionalEdgesBuilder {

  public static final Integer NOT_LOAD_COMMIT = SomeGraph.NOT_LOAD_COMMIT;
  @NotNull
  private final PermanentGraph myGraph;

  @NotNull
  private final Flags myVisibleNodes;

  @NotNull
  private final SetMultiMap<Integer, Integer> resultMap = new SetMultiMap<Integer, Integer>();


  @NotNull
  private final Map<Integer, Set<Integer>> myCurrentStrongVisibleNodes = new HashMap<Integer, Set<Integer>>();
  @NotNull
  private final SetMultiMap<Integer, Integer> myNodesToUpVisibleNodes = new SetMultiMap<Integer, Integer>();
  @NotNull
  private final SetMultiMap<Integer, Integer> myStrongRelationNodes = new SetMultiMap<Integer, Integer>();

  public FilterAdditionalEdgesBuilder(@NotNull PermanentGraph graph, @NotNull Flags visibleNodes) {
    myGraph = graph;
    myVisibleNodes = visibleNodes;
  }

  public MultiMap<Integer, Integer> build() {
    long ms = System.currentTimeMillis();

    runBuild();

    System.out.println(System.currentTimeMillis() - ms);
    return resultMap;
  }

  private void someClear() {
    Set<Map.Entry<Integer, Set<Integer>>> entries = new HashSet<Map.Entry<Integer, Set<Integer>>>(myCurrentStrongVisibleNodes.entrySet());
    for (Map.Entry<Integer, Set<Integer>> strong : entries) {
      if (strong.getValue() == null || strong.getValue().isEmpty()) {
        myCurrentStrongVisibleNodes.remove(strong.getKey());
        myStrongRelationNodes.remove(strong.getKey());
      }
    }
  }

  private void runBuild() {
    for (int i = 0; i < myGraph.nodesCount(); i++) {
      if (myVisibleNodes.get(i)) {
        visibleRun(i);
        someClear();
      } else {
        invisibleRun(i);
      }
    }
  }

  private void visibleRun(int index) {
    Collection<Integer> upVisibleNodes = myNodesToUpVisibleNodes.remove(index);
    removeDuplicateUpVisibleNodes(index, upVisibleNodes);

    HashSet<Integer> relations = new HashSet<Integer>(upVisibleNodes);
    for (int upVisibleNode : upVisibleNodes) {
      addEdge(upVisibleNode, index);
      removeTempEdge(upVisibleNode, index);

      for (int someVisibleNode : myStrongRelationNodes.get(upVisibleNode)) {
        if (!relations.contains(someVisibleNode) && myCurrentStrongVisibleNodes.containsKey(someVisibleNode)) {
          relations.add(someVisibleNode);
        }
      }
    }
    myStrongRelationNodes.put(index, relations);

    Set<Integer> downNodes = new HashSet<Integer>(getCorrectDownNodes(index));
    myCurrentStrongVisibleNodes.put(index, downNodes);

    for (int downNode : downNodes)
      myNodesToUpVisibleNodes.putValue(downNode, index);
  }

  private void addEdge(int upNode, int downNode) {
    boolean isRealEdge = myGraph.getDownNodes(upNode).contains(downNode);
    if (!isRealEdge) {
      resultMap.putValue(upNode, downNode);
      resultMap.putValue(downNode, upNode);
    }
  }

  /////////////////////////////////////////////////

  private void removeDuplicateUpVisibleNodes(int currentIndex, Collection<Integer> upVisibleNodes) {
    Set<Integer> duplicates = new HashSet<Integer>();
    for (int upVisibleNode : upVisibleNodes) {
      for (int duplicate : myStrongRelationNodes.get(upVisibleNode)) {
        if (upVisibleNodes.contains(duplicate))
          duplicates.add(duplicate);
      }
    }

    for (int duplicate : duplicates) {
      removeTempEdge(duplicate, currentIndex);
    }

    upVisibleNodes.removeAll(duplicates);
  }

  private void removeTempEdge(int visibleNodeIndex, int currentIndex) {
    Set<Integer> downNodes = myCurrentStrongVisibleNodes.get(visibleNodeIndex);
    if (downNodes != null)
      downNodes.remove(currentIndex);
  }

  private List<Integer> getCorrectDownNodes(int index) {
    List<Integer> downNodes = myGraph.getDownNodes(index);
    if (downNodes.contains(NOT_LOAD_COMMIT)) {
      downNodes = new ArrayList<Integer>(downNodes);
      downNodes.remove(NOT_LOAD_COMMIT);
    }
    return downNodes;
  }

  private void invisibleRun(int index) {
    Collection<Integer> upVisibleNodes = myNodesToUpVisibleNodes.remove(index);
    removeDuplicateUpVisibleNodes(index, upVisibleNodes);

    List<Integer> newDownNodes = getCorrectDownNodes(index);

    for (int upVisibleNode : upVisibleNodes) {
      Set<Integer> someDownNodes = myCurrentStrongVisibleNodes.get(upVisibleNode);
      if (someDownNodes != null) {
        someDownNodes.remove(index);
        someDownNodes.addAll(newDownNodes);
      }
    }
    for (int downNode : newDownNodes) {
      myNodesToUpVisibleNodes.putValues(downNode, upVisibleNodes);
    }
  }




  private static class SetMultiMap<K, V> extends MultiMap<K, V> {
    @Override
    protected Collection<V> createCollection() {
      return new HashSet<V>();
    }

    @NotNull
    @Override
    public Collection<V> remove(K key) {
      Collection<V> remove = super.remove(key);
      return remove == null ? Collections.<V>emptySet() : remove;
    }
  }
}
