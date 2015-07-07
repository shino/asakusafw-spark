/*
 * Copyright 2011-2015 Asakusa Framework Team.
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
package operator
package user
package join

import java.util.{ List => JList }

import scala.collection.JavaConversions

import org.objectweb.asm.Type

import com.asakusafw.lang.compiler.model.graph.OperatorOutput
import com.asakusafw.spark.runtime.operator.DefaultMasterSelection
import com.asakusafw.spark.tools.asm._
import com.asakusafw.spark.tools.asm.MethodBuilder._

abstract class JoinOperatorFragmentClassBuilder(
  flowId: String,
  dataModelType: Type,
  operatorType: Type,
  operatorOutputs: Seq[OperatorOutput])
    extends UserOperatorFragmentClassBuilder(
      flowId, dataModelType, operatorType, operatorOutputs) {

  def masterType: Type
  def txType: Type
  def masterSelection: Option[(String, Type)]

  def join(mb: MethodBuilder, masterVar: Var, txVar: Var): Unit
}