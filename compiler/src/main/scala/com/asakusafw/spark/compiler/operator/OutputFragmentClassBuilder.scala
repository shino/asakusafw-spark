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

import java.util.concurrent.atomic.AtomicLong

import scala.collection.mutable
import scala.reflect.ClassTag

import org.objectweb.asm.Type
import org.objectweb.asm.signature.SignatureVisitor

import com.asakusafw.lang.compiler.api.JobflowProcessor.{ Context => JPContext }
import com.asakusafw.runtime.model.DataModel
import com.asakusafw.spark.compiler.operator.OutputFragmentClassBuilder._
import com.asakusafw.spark.runtime.fragment.OutputFragment
import com.asakusafw.spark.tools.asm._

class OutputFragmentClassBuilder(
  flowId: String,
  dataModelType: Type)
  extends ClassBuilder(
    Type.getType(
      s"L${GeneratedClassPackageInternalName}/${flowId}/fragment/OutputFragment$$${nextId};"),
    Some(OutputFragmentClassBuilder.signature(dataModelType)),
    classOf[OutputFragment[_]].asType) {

  override def defConstructors(ctorDef: ConstructorDef): Unit = {
    ctorDef.newInit(Seq.empty) { mb =>
      import mb._ // scalastyle:ignore
      thisVar.push().invokeInit(superType,
        getStatic(ClassTag.getClass.asType, "MODULE$", ClassTag.getClass.asType)
          .invokeV("apply", classOf[ClassTag[_]].asType,
            ldc(dataModelType).asType(classOf[Class[_]].asType)))
    }
  }

  override def defMethods(methodDef: MethodDef): Unit = {
    super.defMethods(methodDef)

    methodDef.newMethod("newDataModel", dataModelType, Seq.empty) { mb =>
      import mb._ // scalastyle:ignore
      `return`(pushNew0(dataModelType))
    }

    methodDef.newMethod("newDataModel", classOf[DataModel[_]].asType, Seq.empty) { mb =>
      import mb._ // scalastyle:ignore
      `return`(thisVar.push().invokeV("newDataModel", dataModelType))
    }
  }
}

object OutputFragmentClassBuilder {

  private[this] val curId: AtomicLong = new AtomicLong(0L)

  def nextId: Long = curId.getAndIncrement

  def signature(dataModelType: Type): String = {
    new ClassSignatureBuilder()
      .newSuperclass {
        _.newClassType(classOf[OutputFragment[_]].asType) {
          _.newTypeArgument(SignatureVisitor.INSTANCEOF, dataModelType)
        }
      }
      .build()
  }

  private[this] val cache: mutable.Map[JPContext, mutable.Map[(String, Type), Type]] =
    mutable.WeakHashMap.empty

  def getOrCompile(
    flowId: String,
    dataModelType: Type,
    jpContext: JPContext): Type = {
    cache.getOrElseUpdate(jpContext, mutable.Map.empty).getOrElseUpdate(
      (flowId, dataModelType),
      jpContext.addClass(new OutputFragmentClassBuilder(flowId, dataModelType)))
  }
}