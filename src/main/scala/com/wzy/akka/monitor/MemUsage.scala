package com.wzy.akka.monitor

import java.io._

import scala.util.control.Breaks

/**
 * 采集内存使用率
 */
object MemUsage {
  private val INSTANCE = new MemUsage

  def getInstance: MemUsage = INSTANCE
}

class MemUsage {
  /**
   * Purpose:采集内存使用率
   *
   * @return float,内存使用率,小于1.0
   */
  def get: Float = {
    println("开始收集memory使用率")
    var memUsage = 0.0f
    var pro = null
    val r = Runtime.getRuntime
    try {
      val command = "cat /proc/meminfo"
      val pro = r.exec(command)
      val in = new BufferedReader(new InputStreamReader(pro.getInputStream))
      var line: String = in.readLine
      var count: Long = 0
      var totalMem: Long = 0
      var freeMem: Long = 0

      val loop = new Breaks
      loop.breakable {
        while (line != null) {
          println(line)
          val memInfo: Array[String] = line.split("\\s+")
          if (memInfo(0).startsWith("MemTotal")) totalMem = memInfo(1).toLong
          if (memInfo(0).startsWith("MemFree")) freeMem = memInfo(1).toLong
          memUsage = 1 - freeMem.toFloat / totalMem.toFloat
          println("本节点内存使用率为: " + memUsage)
          count += 1
          if (count == 2) loop.break()
          line = in.readLine
        }
      }
      in.close()
      pro.destroy()
    } catch {
      case e: IOException =>
        val sw = new StringWriter
        e.printStackTrace(new PrintWriter(sw))
        println("MemUsage发生InstantiationException. " + e.getMessage)
        println(sw.toString)
    }
    memUsage
  }
}
