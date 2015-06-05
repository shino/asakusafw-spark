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
package com.asakusafw.spark.gradle.plugins.internal

/**
 * An extension object for the Asakusa on Spark features.
 * This is only for internal use.
 */
class AsakusaSparkBaseExtension {

    /**
     * The compiler project version.
     */
    String compilerProjectVersion

    /**
     * The Spark project version.
     */
    String sparkProjectVersion

    /**
     * The custom Spark artifact notation (nullable).
     */
    String customSparkArtifact

    /**
     * Excluding modules for compiler.
     */
    List<Object> excludeModules = []
}