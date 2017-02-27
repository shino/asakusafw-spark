/*
 * Copyright 2011-2017 Asakusa Framework Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.asakusafw.spark.compiler
package graph

import com.asakusafw.lang.compiler.model.graph.OperatorOutput
import com.asakusafw.lang.compiler.planning.SubPlan
import com.asakusafw.spark.compiler.planning.{PartitionGroupInfo, SubPlanInfo, SubPlanInputInfo}
import com.asakusafw.spark.compiler.util.NumPartitions._
import com.asakusafw.spark.compiler.util.SparkIdioms._
import com.asakusafw.spark.tools.asm.MethodBuilder._
import com.asakusafw.spark.tools.asm._
import com.asakusafw.spark.tools.asm4s._
import org.objectweb.asm.Type

import scala.collection.JavaConversions._

object CoGroupInstantiator extends Instantiator {

  override def newInstance(
    nodeType: Type,
    subplan: SubPlan,
    subplanToIdx: Map[SubPlan, Int])(
      vars: Instantiator.Vars)(
        implicit mb: MethodBuilder,
        context: Instantiator.Context): Var = {

    val primaryOperator = subplan.getAttribute(classOf[SubPlanInfo]).getPrimaryOperator

    val properties = primaryOperator.inputs.map { input =>
      input.dataModelRef.groupingTypes(input.getGroup.getGrouping)
    }.toSet
    assert(properties.size == 1,
      s"The grouping of all inputs should be the same: ${
        properties.map(_.mkString("(", ",", ")")).mkString("(", ",", ")")
      } [${subplan}]")
    val cogroup = pushNew(nodeType)
    cogroup.dup().invokeInit(
      buildSeq { builder =>
        for {
          input <- primaryOperator.getInputs
        } {
          builder +=
            tuple2(
              buildSeq { builder =>
                for {
                  opposite <- input.getOpposites.toSet[OperatorOutput]
                  subPlanInput <- Option(subplan.findInput(opposite.getOwner))
                  inputInfo <- Option(subPlanInput.getAttribute(classOf[SubPlanInputInfo]))
                  if inputInfo.getInputType == SubPlanInputInfo.InputType.PARTITIONED
                  prevSubPlanOutput <- subPlanInput.getOpposites
                } {
                  val prevSubPlan = prevSubPlanOutput.getOwner
                  val marker = prevSubPlanOutput.getOperator
                  builder +=
                    tuple2(
                      vars.nodes.push().aload(ldc(subplanToIdx(prevSubPlan))),
                      context.branchKeys.getField(marker))
                }
              },
              option(
                sortOrdering(
                  input.dataModelRef.groupingTypes(input.getGroup.getGrouping),
                  input.dataModelRef.orderingTypes(input.getGroup.getOrdering))))
        }
      },
      groupingOrdering(properties.head),
      if (properties.head.isEmpty) {
        partitioner(ldc(1))
      } else {
        // TODO: refactoring may ne nice
        val portWithMaxDataSize = subplan.getInputs.maxBy(port =>
          Option(port.getAttribute(classOf[PartitionGroupInfo])).map(_.getDataSize).
            getOrElse(PartitionGroupInfo.DataSize.REGULAR))
//        println(s"********* portWithMaxDataSize: ${portWithMaxDataSize}")
//        println(s"${portWithMaxDataSize.getOperator.getOutput.getOpposites.head.getOwner}")
        partitioner(
          numPartitions(vars.jobContext.push())(portWithMaxDataSize))
      },
      vars.broadcasts.push(),
      vars.jobContext.push())
    cogroup.store()
  }
}
