package com.asakusafw.spark.compiler.subplan

import com.asakusafw.lang.compiler.model.graph.Operator
import com.asakusafw.spark.tools.asm._

trait DriverLabel extends ClassBuilder {

  def label: String

  override def defMethods(methodDef: MethodDef): Unit = {
    super.defMethods(methodDef)

    methodDef.newMethod("label", classOf[String].asType, Seq.empty) { mb =>
      import mb._
      `return`(ldc(label))
    }
  }
}