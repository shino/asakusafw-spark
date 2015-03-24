package com.asakusafw.spark.compiler
package operator
package user

import scala.reflect.ClassTag

import org.objectweb.asm.Type

import com.asakusafw.lang.compiler.model.graph.UserOperator
import com.asakusafw.spark.compiler.spi.UserOperatorCompiler
import com.asakusafw.spark.tools.asm._
import com.asakusafw.spark.tools.asm.MethodBuilder._
import com.asakusafw.vocabulary.operator.Convert

class ConvertOperatorCompiler extends UserOperatorCompiler {

  override def of: Class[_] = classOf[Convert]

  override def compile(operator: UserOperator)(implicit context: Context): Type = {

    val operatorInfo = new OperatorInfo(operator)(context.jpContext)

    assert(operatorInfo.annotationDesc.getDeclaringClass.resolve(context.jpContext.getClassLoader) == of)
    assert(operatorInfo.inputs.size == 1) // FIXME to take multiple inputs for side data?
    assert(operatorInfo.outputs.size == 2)

    assert(operatorInfo.methodType.getArgumentTypes.toSeq ==
      operatorInfo.inputDataModelTypes(Convert.ID_INPUT)
      +: operatorInfo.argumentTypes)

    val builder = new UserOperatorFragmentClassBuilder(
      context.flowId,
      operatorInfo.inputDataModelTypes(Convert.ID_INPUT),
      operatorInfo.implementationClassType,
      operatorInfo.outputs) {

      override def defAddMethod(mb: MethodBuilder, dataModelVar: Var): Unit = {
        import mb._
        getOutputField(mb, operatorInfo.outputs(Convert.ID_OUTPUT_ORIGINAL))
          .invokeV("add", dataModelVar.push().asType(classOf[AnyRef].asType))
        getOutputField(mb, operatorInfo.outputs(Convert.ID_OUTPUT_CONVERTED))
          .invokeV("add",
            getOperatorField(mb)
              .invokeV(
                operatorInfo.methodDesc.getName,
                operatorInfo.outputDataModelTypes(Convert.ID_OUTPUT_CONVERTED),
                dataModelVar.push()
                  +: operatorInfo.arguments.map { argument =>
                    ldc(argument.getValue.resolve(context.jpContext.getClassLoader))(
                      ClassTag(argument.getValue.getValueType.resolve(context.jpContext.getClassLoader)))
                  }: _*)
              .asType(classOf[AnyRef].asType))
        `return`()
      }
    }

    context.jpContext.addClass(builder)
  }
}
