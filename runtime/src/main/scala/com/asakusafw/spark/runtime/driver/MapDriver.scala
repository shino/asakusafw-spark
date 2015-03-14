package com.asakusafw.spark.runtime.driver

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.reflect.{ classTag, ClassTag }

import org.apache.hadoop.io.NullWritable
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.Job
import org.apache.spark._
import org.apache.spark.rdd.RDD

import com.asakusafw.runtime.model.DataModel
import com.asakusafw.runtime.stage.input.TemporaryInputFormat
import com.asakusafw.spark.runtime.fragment._
import com.asakusafw.spark.runtime.rdd._

abstract class MapDriver[T <: DataModel[T]: ClassTag, B](
  @transient val sc: SparkContext,
  @transient prev: RDD[(_, T)])
    extends SubPlanDriver[B] with Branch[B] {

  override def execute(): Map[B, RDD[(_, _)]] = {
    prev.branch[B, Any, Any](branchKeys, { iter =>
      val (fragment, outputs) = fragments
      assert(outputs.keys.toSet == branchKeys)
      iter.flatMap {
        case (_, dm) =>
          fragment.reset()
          fragment.add(dm)
          outputs.iterator.flatMap {
            case (key, output) =>
              def prepare[T <: DataModel[T]](buffer: mutable.ArrayBuffer[_]) = {
                buffer.asInstanceOf[mutable.ArrayBuffer[T]]
                  .map(out => ((key, shuffleKey(key, out)), out))
              }
              prepare(output.buffer)
          }
      }
    },
      partitioners = partitioners,
      keyOrderings = orderings,
      preservesPartitioning = true)
  }

  def fragments[U <: DataModel[U]]: (Fragment[T], Map[B, OutputFragment[U]])
}
