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
package com.asakusafw.spark.extensions.iterativebatch.runtime
package flow

import scala.concurrent.{ ExecutionContext, Future }

import org.apache.spark.{ Partitioner, SparkContext }
import org.apache.spark.rdd.RDD

import com.asakusafw.spark.runtime.Props
import com.asakusafw.spark.runtime.driver.{ Branching, BroadcastId, ShuffleKey }
import com.asakusafw.spark.runtime.rdd._

abstract class CoGroup(
  @transient prevs: Seq[(Seq[(Source, BranchKey)], Option[SortOrdering])],
  @transient grouping: GroupOrdering,
  @transient part: Partitioner)(
    @transient val broadcasts: Map[BroadcastId, Broadcast])(
      @transient implicit val sc: SparkContext)
  extends Source
  with UsingBroadcasts
  with Branching[Seq[Iterator[_]]] {

  @transient
  private val fragmentBufferSize =
    sc.getConf.getInt(Props.FragmentBufferSize, Props.DefaultFragmentBufferSize)

  @transient
  override val dependencies: Set[Node] = prevs.flatMap(_._1.map(_._1)).toSet ++ broadcasts.values

  override def compute(
    rc: RoundContext)(implicit ec: ExecutionContext): Map[BranchKey, Future[RDD[_]]] = {

    val future =
      Future.sequence(
        prevs.map {
          case (targets, sort) =>
            val rdds = targets.map {
              case (source, branchKey) =>
                source.getOrCompute(rc).apply(branchKey).map(_.asInstanceOf[RDD[(ShuffleKey, _)]])
            }
            Future.sequence(rdds).map((_, sort))
        }).zip(zipBroadcasts(rc)).map {
          case (prevs, broadcasts) =>

            sc.clearCallSite()
            sc.setCallSite(label)

            val cogrouped = sc.smcogroup[ShuffleKey](
              prevs.map {
                case (rdds, sort) =>
                  (sc.confluent[ShuffleKey, Any](rdds, part, sort), sort)
              },
              part,
              grouping)

            branch(
              cogrouped.asInstanceOf[RDD[(_, Seq[Iterator[_]])]], broadcasts, rc.hadoopConf)(
                fragmentBufferSize)
        }

    branchKeys.map(key => key -> future.map(_(key))).toMap
  }
}
