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
package com.asakusafw.spark.runtime
package graph

import scala.concurrent.Future
import scala.reflect.ClassTag

import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.NullWritable
import org.apache.hadoop.mapreduce.{ Job => MRJob }
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.spark.rdd.RDD

import com.asakusafw.bridge.stage.StageInfo
import com.asakusafw.runtime.stage.input.TemporaryInputFormat
import com.asakusafw.spark.runtime.JobContext.InputCounter
import com.asakusafw.spark.runtime.rdd.BranchKey

abstract class TemporaryInput[V: ClassTag](
  @transient val broadcasts: Map[BroadcastId, Broadcast[_]])(
    implicit jobContext: JobContext)
  extends NewHadoopInput[TemporaryInputFormat[V], NullWritable, V] {
  self: CacheStrategy[RoundContext, Map[BranchKey, Future[() => RDD[_]]]] =>

  override def counter: InputCounter = InputCounter.External

  protected def paths: Set[String]

  override protected def newJob(rc: RoundContext): MRJob = {
    val job = MRJob.getInstance(rc.hadoopConf.value)
    val stageInfo = StageInfo.deserialize(job.getConfiguration.get(StageInfo.KEY_NAME))
    FileInputFormat.setInputPaths(job, paths.map { path =>
      new Path(stageInfo.resolveUserVariables(path))
    }.toSeq: _*)
    job
  }
}
