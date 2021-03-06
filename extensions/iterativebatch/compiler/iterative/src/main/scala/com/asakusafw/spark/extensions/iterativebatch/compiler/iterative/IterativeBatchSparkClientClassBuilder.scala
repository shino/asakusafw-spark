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
package com.asakusafw.spark.extensions.iterativebatch.compiler
package iterative

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext

import org.objectweb.asm.Type

import com.asakusafw.lang.compiler.analyzer.util.OperatorUtil
import com.asakusafw.lang.compiler.model.description.TypeDescription
import com.asakusafw.lang.compiler.model.graph.Operator
import com.asakusafw.lang.compiler.planning.{ Plan, SubPlan }
import com.asakusafw.spark.compiler.`package`._
import com.asakusafw.spark.compiler.serializer.{
  BranchKeySerializerClassBuilder,
  BroadcastIdSerializerClassBuilder,
  KryoRegistratorCompiler
}
import com.asakusafw.spark.runtime.JobContext
import com.asakusafw.spark.tools.asm._
import com.asakusafw.spark.tools.asm.MethodBuilder._

import com.asakusafw.spark.extensions.iterativebatch.compiler.graph.IterativeJobCompiler
import com.asakusafw.spark.extensions.iterativebatch.runtime.graph.IterativeJob
import com.asakusafw.spark.extensions.iterativebatch.runtime.iterative.IterativeBatchSparkClient

class IterativeBatchSparkClientClassBuilder(
  plan: Plan)(
    implicit context: IterativeBatchExtensionCompiler.Context)
  extends ClassBuilder(
    Type.getType(s"L${GeneratedClassPackageInternalName}/${context.flowId}/IterativeBatchSparkClient;"), // scalastyle:ignore
    classOf[IterativeBatchSparkClient].asType) {

  override def defMethods(methodDef: MethodDef): Unit = {
    super.defMethods(methodDef)

    methodDef.newMethod(
      "newJob",
      classOf[IterativeJob].asType,
      Seq(classOf[JobContext].asType)) { implicit mb =>

        val thisVar :: jobContextVar :: _ = mb.argVars

        val t = IterativeJobCompiler.compile(plan)(context.iterativeJobCompilerContext)
        val job = pushNew(t)
        job.dup().invokeInit(jobContextVar.push())
        `return`(job)
      }

    val branchKeysType = context.addClass(context.branchKeys)
    val broadcastIdsType = context.addClass(context.broadcastIds)

    val registrator = KryoRegistratorCompiler.compile(
      OperatorUtil.collectDataTypes(
        plan.getElements.toSet[SubPlan].flatMap(_.getOperators.toSet[Operator]))
        .toSet[TypeDescription]
        .map(_.asType),
      context.addClass(new BranchKeySerializerClassBuilder(branchKeysType)),
      context.addClass(new BroadcastIdSerializerClassBuilder(broadcastIdsType)))

    methodDef.newMethod("kryoRegistrator", classOf[String].asType, Seq.empty) { implicit mb =>
      `return`(ldc(registrator.getClassName))
    }
  }
}
