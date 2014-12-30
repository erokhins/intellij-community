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
package com.intellij.vcs.log.graph.impl

import com.intellij.vcs.log.graph.api.LinearGraph
import com.intellij.vcs.log.graph.TestGraphBuilder
import com.intellij.vcs.log.graph.graph
import com.intellij.vcs.log.graph.impl.facade.bek.BekSorter
import com.intellij.vcs.log.graph.TestPermanentGraphInfo
import com.intellij.vcs.log.graph.impl.facade.BekBaseLinearGraphController
import com.intellij.vcs.log.graph.asString
import org.junit.Assert.*
import org.junit.Test
import com.intellij.vcs.log.graph.asTestGraphString

class BekTest {

  fun LinearGraph.assert(vararg order: Int = IntArray(0), builder: TestGraphBuilder.() -> Unit) {
    val result = graph(builder)
    val graphInfo = TestPermanentGraphInfo(this, *order)

    val bekIntMap = BekSorter.createBekMap(this, graphInfo.graphLayout, graphInfo.timestampGetter)
    val actualResult = BekBaseLinearGraphController(graphInfo, this, bekIntMap).getCompiledGraph()

    assertEquals(result.asTestGraphString(), actualResult.asTestGraphString())
  }

  val Int.big: Int get() = this * 100000000

  Test fun simple() = graph {
    1(3, 2)
    2(4)
    3(5)
    4(5)
    5()
  }.assert {
    1(3, 2)
    2(4)
    4(5)
    3(5)
    5()
  }

  Test fun simple2() = graph {
    4.big(9.big)
    1.big(3.big, 5.big)
    3.big(9.big)
    5.big(7.big)
    7.big(9.big)
    9.big()
  }.assert {
    1.big(3.big, 5.big)
    5.big(7.big)
    7.big(9.big)
    3.big(9.big)
    4.big(9.big)
    9.big()
  }

  Test fun s22() = graph {
    0(2)
    1(4, 2)
    4(10.big)
    2(3)
    10.big(11.big)
    3(6)
    11.big(6)
    6()
  }.assert(0, 1) {

  }

}
