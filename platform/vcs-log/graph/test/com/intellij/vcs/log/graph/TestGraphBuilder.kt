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
package com.intellij.vcs.log.graph

import com.intellij.vcs.log.graph.api.elements.GraphEdgeType
import java.util.ArrayList
import com.intellij.vcs.log.graph.api.LinearGraph
import com.intellij.vcs.log.graph.api.elements.GraphEdge
import com.intellij.vcs.log.graph.api.elements.GraphNode
import com.intellij.vcs.log.graph.utils.LinearGraphUtils
import com.intellij.util.containers.MultiMap
import com.intellij.util.containers.HashMap
import com.intellij.vcs.log.graph.api.elements.GraphNodeType
import com.intellij.vcs.log.graph.BaseTestGraphBuilder.SimpleEdge
import com.intellij.vcs.log.graph.BaseTestGraphBuilder.SimpleNode
import com.intellij.vcs.log.graph.api.permanent.PermanentGraphInfo
import com.intellij.vcs.log.graph.api.permanent.PermanentCommitsInfo
import com.intellij.vcs.log.graph.api.GraphLayout
import com.intellij.openapi.util.Condition
import com.intellij.util.containers.ContainerUtil
import com.intellij.vcs.log.graph.utils.TimestampGetter
import com.intellij.vcs.log.graph.impl.permanent.GraphLayoutBuilder
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertNull

trait BaseTestGraphBuilder {
  val Int.U: SimpleNode get() = SimpleNode(this, GraphNodeType.USUAL)
  val Int.G: SimpleNode get() = SimpleNode(this, GraphNodeType.GRAY)
  val Int.NOT_LOAD: SimpleNode get() = SimpleNode(this, GraphNodeType.NOT_LOAD_COMMIT)

  val Int.u: SimpleEdge get() = SimpleEdge(this, GraphEdgeType.USUAL)
  val Int.dot: SimpleEdge get() = SimpleEdge(this, GraphEdgeType.DOTTED)
  val Int?.up_dot: SimpleEdge get() = SimpleEdge(this, GraphEdgeType.DOTTED_ARROW_UP)
  val Int?.down_dot: SimpleEdge get() = SimpleEdge(this, GraphEdgeType.DOTTED_ARROW_DOWN)
  val Int?.not_load: SimpleEdge get() = SimpleEdge(this, GraphEdgeType.NOT_LOAD_COMMIT)

  class SimpleEdge(val toNode: Int?, val type: GraphEdgeType = GraphEdgeType.USUAL)
  class SimpleNode(val nodeId: Int, val type: GraphNodeType = GraphNodeType.USUAL)
}

class TestGraphBuilder: BaseTestGraphBuilder {
  private val nodes = ArrayList<NodeWithEdges>()

  fun done(): LinearGraph = TestLinearGraph(nodes)

  fun Int.invoke() = newNode(asSimpleNode())
  fun Int.invoke(vararg edge: Int) = newNode(asSimpleNode(), edge.asSimpleEdges())
  fun Int.invoke(vararg edge: SimpleEdge) = newNode(asSimpleNode(), edge.toList())
  fun SimpleNode.invoke() = newNode(this)
  fun SimpleNode.invoke(vararg edge: Int) = newNode(this, edge.asSimpleEdges())
  fun SimpleNode.invoke(vararg edge: SimpleEdge) = newNode(this, edge.toList())

  private class NodeWithEdges(val nodeId: Int, val edges: List<SimpleEdge>, val type: GraphNodeType = GraphNodeType.USUAL)

  private fun IntArray.asSimpleEdges() = map { SimpleEdge(it) }
  private fun Int.asSimpleNode() = SimpleNode(this)

  private fun newNode(node: SimpleNode, edges: List<SimpleEdge> = listOf()) {
    nodes add NodeWithEdges(node.nodeId, edges, node.type)
  }
  fun node(id: Int, vararg edge: Int) {
    nodes add NodeWithEdges(id, edge.map {
      SimpleEdge(it, GraphEdgeType.USUAL)
    })
  }

  fun node(id: Int, vararg edge: SimpleEdge) {
    nodes add NodeWithEdges(id, edge.toList())
  }

  private class TestLinearGraph(buildNodes: List<NodeWithEdges>): LinearGraph {
    private val nodes: List<GraphNode>
    private val reverseMap: Map<Int, Int>
    private val edges = MultiMap<Int, GraphEdge>()

    val SimpleEdge.toIndex: Int?  get() = toNode?.let{reverseMap[it]}

    ;{
      val idsMap = HashMap<Int, Int>()
      nodes = buildNodes.map2 { (index, it) ->
        idsMap[it.nodeId] = index
        GraphNode(it.nodeId, index, it.type)
      }
      reverseMap = idsMap

      // create edges
      for (node in buildNodes) {
        val nodeIndex = reverseMap[node.nodeId]!!
        for (simpleEdge in node.edges) {
          val edgeType = simpleEdge.type

          if (edgeType.isNormalEdge()) {
            val anotherNodeIndex = simpleEdge.toIndex
            assert(anotherNodeIndex != null, "Graph is incorrect. Node ${node.nodeId} has ${edgeType} edge to not existed node: ${simpleEdge.toNode}")

            val graphEdge = GraphEdge.createNormalEdge(anotherNodeIndex!!, nodeIndex, edgeType)
            edges.putValue(nodeIndex, graphEdge)
            edges.putValue(anotherNodeIndex, graphEdge)
          } else {
            edges.putValue(nodeIndex, GraphEdge.createEdgeWithAdditionInfo(nodeIndex, simpleEdge.toNode, edgeType))
          }
        }
      }

    }

    override fun nodesCount() = nodes.size

    override fun getUpNodes(nodeIndex: Int) = LinearGraphUtils.getUpNodes(this, nodeIndex)
    override fun getDownNodes(nodeIndex: Int) = LinearGraphUtils.getDownNodes(this, nodeIndex)

    override fun getAdjacentEdges(nodeIndex: Int) = edges[nodeIndex].toList()

    override fun getGraphNode(nodeIndex: Int) = nodes[nodeIndex]

    override fun getNodeIndexById(nodeId: Int) = reverseMap[nodeId]

  }
}

private fun LinearGraph.assertEdge(nodeIndex: Int, edge: GraphEdge) {
  if (edge.getType().isNormalEdge()) {
    if (nodeIndex == edge.getUpNodeIndex()) {
      assertTrue(getAdjacentEdges(edge.getDownNodeIndex()).contains(edge))
    }
    else {
      assertTrue(nodeIndex == edge.getDownNodeIndex())
      assertTrue(getAdjacentEdges(edge.getUpNodeIndex()).contains(edge))
    }
  } else {
    when (edge.getType()) {
      GraphEdgeType.NOT_LOAD_COMMIT, GraphEdgeType.DOTTED_ARROW_DOWN -> {
        assertTrue(nodeIndex == edge.getUpNodeIndex())
        assertNull(edge.getDownNodeIndex())
      }
      GraphEdgeType.DOTTED_ARROW_UP -> {
        assertTrue(nodeIndex == edge.getDownNodeIndex())
        assertNull(edge.getUpNodeIndex())
      }
    }
  }
}

fun LinearGraph.asTestGraphString(): String = StringBuilder {
  for(nodeIndex in 0..nodesCount() - 1) {
    val node = getGraphNode(nodeIndex)
    append(node.getNodeId())
    assertEquals(nodeIndex, node.getNodeIndex(),
      "nodeIndex: $nodeIndex, but for node with this index(nodeId: ${node.getNodeId()}) nodeIndex: ${node.getNodeIndex()}"
    )
    when (node.getType()) {
      GraphNodeType.GRAY -> append(".G")
      GraphNodeType.NOT_LOAD_COMMIT -> append(".NOT_LOAD")
    }

    // edges
    append("(")
    getAdjacentEdges(nodeIndex).map {
      assertEdge(nodeIndex, it)
      if (it.getUpNodeIndex() == nodeIndex) {
        val startId = if (it.getType().isNormalEdge()) {
          getGraphNode(it.getDownNodeIndex()).getNodeId().toString()
        }
        else if (it.getAdditionInfo() != null) {
          it.getAdditionInfo().toString()
        }
        else {
          "null"
        }

        when (it.getType()!!) {
          GraphEdgeType.USUAL -> startId
          GraphEdgeType.DOTTED -> "$startId.dot"
          GraphEdgeType.DOTTED_ARROW_UP -> "$startId.up_dot"
          GraphEdgeType.DOTTED_ARROW_DOWN -> "$startId.down_dot"
          GraphEdgeType.NOT_LOAD_COMMIT -> "$startId.not_load"
        }
      } else {
        null
      }
    }.mapNotNull { it }.joinTo(this, separator = ", ")

    append(")")
    append("\n")
  }
}.toString()

fun graph(f: TestGraphBuilder.() -> Unit): LinearGraph {
  val builder = TestGraphBuilder()
  builder.f()
  return builder.done()
}

private fun <T, R> Iterable<T>.map2(transform: (Int, T) -> R): List<R> {
  val result = ArrayList<R>()
  var index = 0
  for (element in this) {
    result.add(transform(index, element))
    index++
  }
  return result
}

class TestPermanentGraphInfo(
    val graph: LinearGraph,
    vararg val headsOrder: Int = IntArray(0),
    val branchNodes: Set<Int> = emptySet()
) : PermanentGraphInfo<Int> {

  val commitInfo = object : PermanentCommitsInfo<Int> {
    override fun getCommitId(nodeId: Int) = nodeId
    override fun getTimestamp(nodeId: Int) = nodeId.toLong()
    override fun getNodeId(commitId: Int) = commitId
    override fun convertToNodeIds(heads: MutableCollection<Int>) = ContainerUtil.newHashSet(heads)
  }

  val timestampGetter = object : TimestampGetter {
    override fun size() = graph.nodesCount()
    override fun getTimestamp(index: Int) = commitInfo.getTimestamp(graph.getGraphNode(index).getNodeId())
  }

  val graphLayout = GraphLayoutBuilder.build(graph) { (x, y) ->
    if (headsOrder.isEmpty()) {
      graph.getGraphNode(x).getNodeId() - graph.getGraphNode(y).getNodeId()
    }
    else {
      val t = if (headsOrder.indexOf(x) == -1) x else if(headsOrder.indexOf(y) == -1) y else -1
      if (t != -1) throw IllegalStateException("Not found headsOrder for $t node by id")
      headsOrder.indexOf(x) - headsOrder.indexOf(y)
    }
  }

  override fun getPermanentCommitsInfo() = commitInfo
  override fun getPermanentLinearGraph() = graph
  override fun getPermanentGraphLayout() = graphLayout

  override fun getNotCollapsedNodes() = object : Condition<Int> {
    override fun value(nodeId: Int) = branchNodes.contains(nodeId)
  }

  // todo fix it, if you needed of
  override fun getGraphColorManager() = throw UnsupportedOperationException()

}