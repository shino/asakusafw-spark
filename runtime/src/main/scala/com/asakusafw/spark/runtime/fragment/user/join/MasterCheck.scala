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
package com.asakusafw.spark.runtime.fragment
package user.join

import com.asakusafw.runtime.flow.{ ArrayListBuffer, ListBuffer }
import com.asakusafw.runtime.model.DataModel

trait MasterCheck[M <: DataModel[M], T <: DataModel[T]] extends Join[M, T] {

  def missed: Fragment[T]

  def found: Fragment[T]

  override def join(master: M, tx: T): Unit = {
    (if (master != null) found else missed).add(tx) // scalastyle:ignore
  }
}
