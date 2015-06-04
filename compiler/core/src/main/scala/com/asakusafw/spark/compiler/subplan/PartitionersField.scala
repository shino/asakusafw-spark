package com.asakusafw.spark.compiler
package subplan

import scala.collection.mutable
import scala.collection.JavaConversions._
import scala.reflect.NameTransformer

import org.apache.spark.{ HashPartitioner, Partitioner, SparkConf, SparkContext }
import org.objectweb.asm.{ Opcodes, Type }
import org.objectweb.asm.signature.SignatureVisitor

import com.asakusafw.lang.compiler.api.JobflowProcessor.{ Context => JPContext }
import com.asakusafw.lang.compiler.planning.{ PlanMarker, SubPlan }
import com.asakusafw.spark.compiler.planning.SubPlanOutputInfo
import com.asakusafw.spark.runtime.rdd.BranchKey
import com.asakusafw.spark.tools.asm._
import com.asakusafw.spark.tools.asm.MethodBuilder._

trait PartitionersField extends ClassBuilder with NumPartitions {

  def flowId: String

  def jpContext: JPContext

  def branchKeys: BranchKeys

  def subplanOutputs: Seq[SubPlan.Output]

  override def defFields(fieldDef: FieldDef): Unit = {
    super.defFields(fieldDef)

    fieldDef.newField(
      Opcodes.ACC_PRIVATE | Opcodes.ACC_TRANSIENT,
      "partitioners",
      classOf[Map[_, _]].asType,
      new TypeSignatureBuilder()
        .newClassType(classOf[Map[_, _]].asType) {
          _.newTypeArgument(SignatureVisitor.INSTANCEOF, classOf[BranchKey].asType)
            .newTypeArgument(SignatureVisitor.INSTANCEOF, classOf[Partitioner].asType)
        }
        .build())
  }

  override def defMethods(methodDef: MethodDef): Unit = {
    super.defMethods(methodDef)

    methodDef.newMethod("partitioners", classOf[Map[_, _]].asType, Seq.empty,
      new MethodSignatureBuilder()
        .newReturnType {
          _.newClassType(classOf[Map[_, _]].asType) {
            _.newTypeArgument(SignatureVisitor.INSTANCEOF, classOf[BranchKey].asType)
              .newTypeArgument(SignatureVisitor.INSTANCEOF, classOf[Partitioner].asType)
          }
        }
        build ()) { mb =>
        import mb._
        thisVar.push().getField("partitioners", classOf[Map[_, _]].asType).unlessNotNull {
          thisVar.push().putField("partitioners", classOf[Map[_, _]].asType, initPartitioners(mb))
        }
        `return`(thisVar.push().getField("partitioners", classOf[Map[_, _]].asType))
      }
  }

  def getPartitionersField(mb: MethodBuilder): Stack = {
    import mb._
    thisVar.push().invokeV("partitioners", classOf[Map[_, _]].asType)
  }

  private def initPartitioners(mb: MethodBuilder): Stack = {
    import mb._
    val builder = getStatic(Map.getClass.asType, "MODULE$", Map.getClass.asType)
      .invokeV("newBuilder", classOf[mutable.Builder[_, _]].asType)
    for {
      output <- subplanOutputs.sortBy(_.getOperator.getSerialNumber)
      outputInfo <- Option(output.getAttribute(classOf[SubPlanOutputInfo]))
    } {
      outputInfo.getOutputType match {
        case SubPlanOutputInfo.OutputType.AGGREGATED | SubPlanOutputInfo.OutputType.PARTITIONED =>
          builder.invokeI(
            NameTransformer.encode("+="),
            classOf[mutable.Builder[_, _]].asType,
            getStatic(Tuple2.getClass.asType, "MODULE$", Tuple2.getClass.asType).
              invokeV("apply", classOf[(_, _)].asType,
                branchKeys.getField(mb, output.getOperator).asType(classOf[AnyRef].asType), {
                  val partitioner = pushNew(classOf[HashPartitioner].asType)
                  partitioner.dup().invokeInit(
                    numPartitions(mb, thisVar.push().invokeV("sc", classOf[SparkContext].asType))(output))
                  partitioner.asType(classOf[AnyRef].asType)
                })
              .asType(classOf[AnyRef].asType))
        case SubPlanOutputInfo.OutputType.BROADCAST =>
          builder.invokeI(
            NameTransformer.encode("+="),
            classOf[mutable.Builder[_, _]].asType,
            getStatic(Tuple2.getClass.asType, "MODULE$", Tuple2.getClass.asType).
              invokeV("apply", classOf[(_, _)].asType,
                branchKeys.getField(mb, output.getOperator).asType(classOf[AnyRef].asType), {
                  val partitioner = pushNew(classOf[HashPartitioner].asType)
                  partitioner.dup().invokeInit(ldc(1))
                  partitioner.asType(classOf[AnyRef].asType)
                })
              .asType(classOf[AnyRef].asType))
        case _ =>
      }
    }
    builder.invokeI("result", classOf[AnyRef].asType).cast(classOf[Map[_, _]].asType)
  }
}
