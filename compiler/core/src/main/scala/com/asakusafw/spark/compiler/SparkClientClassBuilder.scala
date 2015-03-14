package com.asakusafw.spark.compiler

import org.objectweb.asm.Type

import com.asakusafw.spark.runtime.SparkClient
import com.asakusafw.spark.tools.asm._

class SparkClientClassBuilder(flowId: String)
    extends ClassBuilder(
      Type.getType(s"L${GeneratedClassPackageInternalName}/${flowId}/SparkClient;"),
      classOf[SparkClient].asType) {

}
