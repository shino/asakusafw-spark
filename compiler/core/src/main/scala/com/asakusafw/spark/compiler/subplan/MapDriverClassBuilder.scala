package com.asakusafw.spark.compiler.subplan

import java.util.concurrent.atomic.AtomicLong

import scala.reflect.ClassTag

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.objectweb.asm._
import org.objectweb.asm.signature.SignatureVisitor

import com.asakusafw.spark.runtime.driver.MapDriver
import com.asakusafw.spark.tools.asm._
import com.asakusafw.spark.tools.asm.MethodBuilder._

abstract class MapDriverClassBuilder(
  flowId: String,
  val dataModelType: Type,
  val branchKeyType: Type)
    extends ClassBuilder(
      Type.getType(s"L${classOf[MapDriver[_, _]].asType.getInternalName}$$${flowId}$$${MapDriverClassBuilder.nextId};"),
      Option(MapDriverClassBuilder.signature(dataModelType, branchKeyType)),
      classOf[MapDriver[_, _]].asType)
    with Branching {

  override def defConstructors(ctorDef: ConstructorDef): Unit = {
    super.defConstructors(ctorDef)

    ctorDef.newInit(Seq(classOf[SparkContext].asType, classOf[RDD[_]].asType)) { mb =>
      import mb._
      val scVar = `var`(classOf[SparkContext].asType, thisVar.nextLocal)
      val prevVar = `var`(classOf[RDD[_]].asType, scVar.nextLocal)
      thisVar.push().invokeInit(superType, scVar.push(), prevVar.push(),
        getStatic(ClassTag.getClass.asType, "MODULE$", ClassTag.getClass.asType)
          .invokeV("apply", classOf[ClassTag[_]].asType, ldc(dataModelType).asType(classOf[Class[_]].asType)))
    }
  }
}

object MapDriverClassBuilder {

  private[this] val curId: AtomicLong = new AtomicLong(0L)

  def nextId: Long = curId.getAndIncrement

  def signature(dataModelType: Type, branchKeyType: Type): String = {
    new ClassSignatureBuilder()
      .newSuperclass {
        _.newClassType(classOf[MapDriver[_, _]].asType) {
          _
            .newTypeArgument(SignatureVisitor.INSTANCEOF, dataModelType)
            .newTypeArgument(SignatureVisitor.INSTANCEOF, branchKeyType)
        }
      }
      .build()
  }
}
