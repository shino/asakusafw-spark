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
package operator

import java.util.concurrent.atomic.AtomicLong

import scala.collection.mutable

import org.objectweb.asm.Type
import org.objectweb.asm.signature.SignatureVisitor

import com.asakusafw.runtime.model.DataModel
import com.asakusafw.spark.compiler.operator.EdgeFragmentClassBuilder._
import com.asakusafw.spark.runtime.fragment.{ EdgeFragment, Fragment }
import com.asakusafw.spark.tools.asm._
import com.asakusafw.spark.tools.asm.MethodBuilder._

class EdgeFragmentClassBuilder(
  dataModelType: Type)(
    implicit context: CompilerContext)
  extends ClassBuilder(
    Type.getType(
      s"L${GeneratedClassPackageInternalName}/${context.flowId}/fragment/EdgeFragment$$${nextId};"),
    new ClassSignatureBuilder()
      .newSuperclass {
        _.newClassType(classOf[EdgeFragment[_]].asType) {
          _.newTypeArgument(SignatureVisitor.INSTANCEOF) {
            _.newClassType(dataModelType)
          }
        }
      },
    classOf[EdgeFragment[_]].asType) {

  override def defConstructors(ctorDef: ConstructorDef): Unit = {
    ctorDef.newInit(Seq(classOf[Array[Fragment[_]]].asType)) { implicit mb =>
      val thisVar :: childrenVar :: _ = mb.argVars
      thisVar.push().invokeInit(superType, childrenVar.push())
    }
  }

  override def defMethods(methodDef: MethodDef): Unit = {
    super.defMethods(methodDef)

    methodDef.newMethod("newDataModel", dataModelType, Seq.empty) { implicit mb =>
      `return`(pushNew0(dataModelType))
    }

    methodDef.newMethod("newDataModel", classOf[DataModel[_]].asType, Seq.empty) { implicit mb =>
      val thisVar :: _ = mb.argVars
      `return`(thisVar.push().invokeV("newDataModel", dataModelType))
    }
  }
}

object EdgeFragmentClassBuilder {

  private[this] val curIds: mutable.Map[CompilerContext, AtomicLong] =
    mutable.WeakHashMap.empty

  def nextId(implicit context: CompilerContext): Long =
    curIds.getOrElseUpdate(context, new AtomicLong(0L)).getAndIncrement()

  private[this] val cache: mutable.Map[CompilerContext, mutable.Map[Type, Type]] = // scalastyle:ignore
    mutable.WeakHashMap.empty

  def getOrCompile(
    dataModelType: Type)(
      implicit context: CompilerContext): Type = {
    cache.getOrElseUpdate(context, mutable.Map.empty)
      .getOrElseUpdate(
        dataModelType,
        context.addClass(new EdgeFragmentClassBuilder(dataModelType)))
  }
}
