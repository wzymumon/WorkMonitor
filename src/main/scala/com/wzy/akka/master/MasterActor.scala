package com.wzy.akka.master

import akka.actor.{Actor, ActorSystem, Props}
import com.wzy.akka.common.{HeartBeat, RegisterWorkerInfo, RegisteredWorkerInfo, RemoveTimeOutWorker, StartTimeOutWorker, WorkerInfo}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.collection.mutable

/**
 * 将所有worker节点的性能指标统计到对应节点上
 */
class MasterActor extends Actor {
  // 定义一个 mutable.HashMap 属性，用于管理Worker
  val workers: mutable.Map[String, WorkerInfo] = mutable.HashMap[String, WorkerInfo]()

  override def receive: Receive = {
    case "start" => {
      println("Master服务器启动了...")
      // Master 启动定时任务，定时检测注册的 Worker 有哪些没有更新心跳，已经超时的 Worker，将其从 HashMap 中删除掉。
      self ! StartTimeOutWorker
    }

    // 接收到Worker 客户端注册的信息，保存进 HashMap
    case RegisterWorkerInfo(id, cpu, ram) => {
      if (!workers.contains(id)) {
        // 创建 WorkerInfo
        val workerInfo: WorkerInfo = new WorkerInfo(id, cpu, ram)
        // 加入到 HashMap
        workers += (id -> workerInfo)
        println("workerInfo" + workerInfo.toString)
        // 回复客户端注册成功
        sender() ! RegisteredWorkerInfo
      }
    }

    // 开启定时器，每隔一定时间检测是否有 Worker 的心跳超时
    case StartTimeOutWorker => {
      println("开启了定时检测Worker心跳的任务")
      import context.dispatcher // 使用调度器时候必须导入dispatcher
      context.system.scheduler.schedule(0 millis, 9000 millis, self, RemoveTimeOutWorker)
    }

    // 判断哪些 Worker 心跳超时（nowTime - lastHeartBeatTime），对已经超时的 Worker，将其从 HashMap 中删除掉。
    case RemoveTimeOutWorker => {
      // 首先获取所有 Workers 的所有 WorkerInfo
      val workerInfos = workers.values
      val nowTime = System.currentTimeMillis()
      // 过滤出所有超时的 workerInfo 并删除即可
      workerInfos.filter(workerInfo => (nowTime - workerInfo.lastHeartBeatTime) > 6000)
        .foreach(workerInfo => workers.remove(workerInfo.id))
      println("当前有 " + workers.size + " 个Worker存活")
    }

    case HeartBeat(id, cpuUsage, memUsage) => {
      // 更新对应的 Worker 的心跳时间
      val workerInfo = workers(id)
      workerInfo.lastHeartBeatTime = System.currentTimeMillis()
      workerInfo.lastCpuUsage = cpuUsage
      workerInfo.lastMemUsage = memUsage
      println("Master更新了" + id + " 的心跳时间 ")
    }
  }
}

object MasterActorApp {
  def main(args: Array[String]): Unit = {
    if (args.length != 3) {
      println("请输入参数  host  port  MasterActor的名字")
      sys.exit()
    }
    val host = args(0) //  服务端ip地址
    val port = args(1) //  端口
    val masterName = args(2) //  MasterActor的名字
    // 创建 config 对象，指定协议类型、监听的ip和端口
    val config = ConfigFactory.parseString(
      s"""
         |akka.actor.provider="akka.remote.RemoteActorRefProvider"
         |akka.remote.netty.tcp.hostname=$host
         |akka.remote.netty.tcp.port=$port
      """.stripMargin)
    // 先创建 ActorSystem
    val masterActorSystem = ActorSystem("Master", config)
    // 再创建 Master 的 Actor 和 ActorRef
    val masterActorRef = masterActorSystem.actorOf(Props[MasterActor], s"${masterName}")

    // 启动 Master
    masterActorRef ! "start"
  }
}