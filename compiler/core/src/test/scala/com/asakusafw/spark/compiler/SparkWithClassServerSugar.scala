package com.asakusafw.spark.compiler

import org.scalatest.BeforeAndAfterEach
import org.scalatest.Suite

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext

trait SparkWithClassServerSugar extends BeforeAndAfterEach { self: Suite =>

  var cl: ClassLoader = _

  var classServer: ClassServer = _

  var sc: SparkContext = _

  override def beforeEach() {
    try {
      super.beforeEach()
    } finally {
      cl = Thread.currentThread().getContextClassLoader()

      val conf = new SparkConf
      conf.setMaster("local[*]")
      conf.setAppName(getClass.getName)
      conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      conf.set("spark.kryo.registrator", kryoRegistrator)

      classServer = new ClassServer(cl, conf)
      val uri = classServer.start()
      Thread.currentThread().setContextClassLoader(classServer.classLoader)

      conf.set("spark.repl.class.uri", uri)
      sc = new SparkContext(conf)
    }
  }

  def kryoRegistrator: String = "com.asakusafw.spark.runtime.serializer.KryoRegistrator"

  override def afterEach() {
    try {
      Thread.currentThread().setContextClassLoader(cl)
      sc.stop
      sc = null
      classServer.stop
      classServer = null
    } finally {
      super.afterEach
    }
  }
}