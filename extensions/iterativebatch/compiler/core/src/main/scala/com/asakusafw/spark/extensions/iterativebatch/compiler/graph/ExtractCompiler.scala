/*
 * Copyright 2011-2016 Asakusa Framework Team.
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
package com.asakusafw.spark.extensions.iterativebatch.compiler
package graph

import scala.collection.JavaConversions._

import org.objectweb.asm.Type

import com.asakusafw.lang.compiler.planning.SubPlan
import com.asakusafw.spark.compiler.graph.{
  CacheOnce,
  ExtractClassBuilder,
  ExtractInstantiator,
  Instantiator
}
import com.asakusafw.spark.compiler.planning.{ IterativeInfo, SubPlanInfo }
import com.asakusafw.spark.compiler.spi.NodeCompiler

import com.asakusafw.spark.extensions.iterativebatch.compiler.spi.RoundAwareNodeCompiler

class ExtractCompiler extends RoundAwareNodeCompiler {

  override def support(
    subplan: SubPlan)(
      implicit context: NodeCompiler.Context): Boolean = {
    subplan.getAttribute(classOf[SubPlanInfo]).getDriverType == SubPlanInfo.DriverType.EXTRACT
  }

  override def instantiator: Instantiator = ExtractInstantiator

  override def compile(
    subplan: SubPlan)(
      implicit context: NodeCompiler.Context): Type = {
    assert(support(subplan), s"The subplan is not supported: ${subplan}")

    val subPlanInfo = subplan.getAttribute(classOf[SubPlanInfo])
    val primaryInputs = subPlanInfo.getPrimaryInputs.toSet[SubPlan.Input]
    assert(primaryInputs.size == 1,
      s"The size of primary inputs should be 1: ${primaryInputs.size} [${subplan}]")

    val marker = primaryInputs.head.getOperator

    val iterativeInfo = IterativeInfo.get(subplan)

    val builder =
      iterativeInfo.getRecomputeKind match {
        case IterativeInfo.RecomputeKind.ALWAYS =>
          new ExtractClassBuilder(
            marker)(
            subPlanInfo.getLabel,
            subplan.getOutputs.toSeq) with CacheAlways
        case IterativeInfo.RecomputeKind.PARAMETER =>
          new ExtractClassBuilder(
            marker)(
            subPlanInfo.getLabel,
            subplan.getOutputs.toSeq) with CacheByParameter {

            override val parameters: Set[String] = iterativeInfo.getParameters.toSet
          }
        case IterativeInfo.RecomputeKind.NEVER =>
          new ExtractClassBuilder(
            marker)(
            subPlanInfo.getLabel,
            subplan.getOutputs.toSeq) with CacheOnce
      }

    context.addClass(builder)
  }
}
