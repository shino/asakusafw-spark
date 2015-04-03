package com.asakusafw.spark.runtime.driver

import java.io.{ DataInput, DataOutput }

import scala.annotation.tailrec

import org.apache.hadoop.io.{ BooleanWritable, Writable }

import com.asakusafw.runtime.value.ValueOption

class ShuffleKey protected (
    val grouping: Seq[ValueOption[_]],
    val ordering: Seq[ValueOption[_]]) extends Writable with Equals {

  override def write(out: DataOutput): Unit = {
    grouping.foreach { value =>
      value.write(out)
    }
    ordering.foreach { value =>
      value.write(out)
    }
  }

  override def readFields(in: DataInput): Unit = {
    grouping.foreach { value =>
      value.readFields(in)
    }
    ordering.foreach { value =>
      value.readFields(in)
    }
  }

  override def hashCode: Int = (grouping ++ ordering).hashCode

  override def equals(obj: Any): Boolean = {
    obj match {
      case that: ShuffleKey =>
        (that canEqual this) && (this.grouping == that.grouping) && (this.ordering == that.ordering)
      case _ => false
    }
  }

  override def canEqual(obj: Any): Boolean = {
    obj.isInstanceOf[ShuffleKey]
  }

  def dropOrdering: ShuffleKey = new ShuffleKey(grouping, Seq.empty)
}

object ShuffleKey {

  @tailrec
  private[this] def compare0(xs: Seq[ValueOption[_]], ys: Seq[ValueOption[_]], ascs: Seq[Boolean]): Int = {
    if (xs.isEmpty) {
      0
    } else {
      val cmp = if (ascs.head) {
        xs.head.compareTo(ys.head)
      } else {
        ys.head.compareTo(xs.head)
      }
      if (cmp == 0) {
        compare0(xs.tail, ys.tail, ascs.tail)
      } else {
        cmp
      }
    }
  }

  object GroupingOrdering extends Ordering[ShuffleKey] {

    override def compare(x: ShuffleKey, y: ShuffleKey): Int = {
      assert(x.grouping.size == y.grouping.size)
      assert(x.grouping.zip(y.grouping).forall { case (x, y) => x.getClass == y.getClass })

      compare0(x.grouping, y.grouping, Seq.fill(x.grouping.size)(true))
    }
  }

  class SortOrdering(directions: Seq[Boolean]) extends Ordering[ShuffleKey] {

    override def compare(x: ShuffleKey, y: ShuffleKey): Int = {
      assert(x.grouping.size == y.grouping.size)
      assert(x.grouping.zip(y.grouping).forall { case (x, y) => x.getClass == y.getClass })
      assert(x.ordering.size == y.ordering.size)
      assert(x.ordering.zip(y.ordering).forall { case (x, y) => x.getClass == y.getClass })
      assert(directions.size == x.ordering.size)

      val cmp = GroupingOrdering.compare(x, y)
      if (cmp == 0) {
        compare0(x.ordering, y.ordering, directions)
      } else {
        cmp
      }
    }
  }
}
