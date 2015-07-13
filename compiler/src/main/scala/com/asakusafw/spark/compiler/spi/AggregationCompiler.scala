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
package spi

import java.util.ServiceLoader

import scala.collection.mutable
import scala.collection.JavaConversions._

import org.objectweb.asm.Type

import com.asakusafw.lang.compiler.model.graph.{ Operator, UserOperator }

trait AggregationCompiler {

  def of: Class[_]
  def compile(
    operator: UserOperator)(
      implicit context: SparkClientCompiler.Context): Type
}

object AggregationCompiler {

  private def getCompiler(
    operator: Operator)(
      implicit context: SparkClientCompiler.Context): Option[AggregationCompiler] = {
    operator match {
      case op: UserOperator =>
        apply(context.jpContext.getClassLoader)
          .get(op.getAnnotation.resolve(context.jpContext.getClassLoader).annotationType)
      case _ => None
    }
  }

  def support(
    operator: Operator)(
      implicit context: SparkClientCompiler.Context): Boolean = {
    getCompiler(operator).isDefined
  }

  def compile(
    operator: Operator)(
      implicit context: SparkClientCompiler.Context): Type = {
    getCompiler(operator) match {
      case Some(compiler) => compiler.compile(operator.asInstanceOf[UserOperator])
      case _ => throw new AssertionError()
    }
  }

  private[this] val aggregationCompilers: mutable.Map[ClassLoader, Map[Class[_], AggregationCompiler]] = // scalastyle:ignore
    mutable.WeakHashMap.empty

  private[this] def apply(classLoader: ClassLoader): Map[Class[_], AggregationCompiler] = {
    aggregationCompilers.getOrElse(classLoader, reload(classLoader))
  }

  private[this] def reload(classLoader: ClassLoader): Map[Class[_], AggregationCompiler] = {
    val ors = ServiceLoader.load(classOf[AggregationCompiler], classLoader).map {
      resolver => resolver.of -> resolver
    }.toMap[Class[_], AggregationCompiler]
    aggregationCompilers(classLoader) = ors
    ors
  }
}
