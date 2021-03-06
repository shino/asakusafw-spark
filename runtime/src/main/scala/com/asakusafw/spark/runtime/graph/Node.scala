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

import org.apache.spark.backdoor._
import org.apache.spark.util.backdoor._

trait Node extends Serializable {

  implicit def jobContext: JobContext

  def label: String

  def withCallSite[A](rc: RoundContext)(block: => A): A = {
    jobContext.sparkContext.setCallSite(
      CallSite(rc.roundId.map(r => s"${label}: [${r}]").getOrElse(label), rc.toString))
    try {
      block
    } finally {
      jobContext.sparkContext.clearCallSite()
    }
  }
}
