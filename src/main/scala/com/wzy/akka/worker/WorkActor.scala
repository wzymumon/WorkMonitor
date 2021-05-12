package com.wzy.akka.worker

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, Props}
import com.wzy.akka.common.{HeartBeat, RegisterWorkerInfo, RegisteredWorkerInfo, SendHeartBeat}
import com.typesafe.config.ConfigFactory
import com.wzy.akka.monitor.{CpuUsage, MemUsage}

import scala.concurrent.duration._

class WorkerActor(workerName: String, serverHost: String, serverPort: Int, masterName: String) extends Actor {
  //定义一个MasterActorRef
  var masterActorProxy: ActorSelection = _

  //定义Worker的编号
  var id: String = workerName

  //在Actor中有一个方法preStart方法，它会在Actor运行前执行
  //在Akka开发中，通常将初始化的工作，放在preStart方法中
  override def preStart(): Unit = {
    this.masterActorProxy = context.actorSelection(s"akka.tcp://Master@${serverHost}:${serverPort}/user/${masterName}")
    println("this.masterActorProxy=" + this.masterActorProxy)
  }

  override def receive: Receive = {
    case "start" => {
      println("Worker客户端启动运行 向Master发生注册信息")
      val CpuCores: Int = Runtime.getRuntime.availableProcessors()
      val ram = MemUsage.getInstance.getMaxMemory
      masterActorProxy ! RegisterWorkerInfo(id, CpuCores, ram)
    }
    case SendHeartBeat => {
      println("WorkedId= " + id + " 向Master发送心跳")
      val cpuUsage: Float = CpuUsage.getInstance.get
      val memUsage: Float = MemUsage.getInstance.get
      masterActorProxy ! HeartBeat(id, cpuUsage, memUsage)
    }
    case RegisteredWorkerInfo => {
      println(s"收到 master 回复消息 workerId= $id 注册成功")
      // 当注册成功后，定义一个计时器，每隔一段时间，发生SendHeartBeat给自己
      import context.dispatcher
      //说明
      //1.schedule 创建一个定时器
      //2.0 millis, 延时多久才执行, 0 表示不延时，立即执行
      //3. 3000 millis 表示每隔多长时间执行 3 秒
      //4. self 给自己发送 消息
      //5. SendHeartBeat 消息
      context.system.scheduler.schedule(0 millis, 3000 millis, self, SendHeartBeat)
    }

  }
}

object WorkerActorApp {

  def main(args: Array[String]): Unit = {
    if (args.length != 6) {
      println("请输入参数 host port WorkerActor的名字 serverHost serverPort MasterActor的名字")
    }

    val host = args(0)
    val port = args(1)
    val workerName = args(2)

    val serverHost = args(3)
    val serverPort = args(4)

    val masterName = args(5)

    // 创建 WorkerActor 的 Actor 和 ActorRef
    val config = ConfigFactory.parseString(
      s"""
         |akka.actor.provider="akka.remote.RemoteActorRefProvider"
         |akka.remote.netty.tcp.hostname=$host
         |akka.remote.netty.tcp.port=$port
      """.stripMargin)

    //创建ActorSystem
    val workerActorSystem = ActorSystem("Worker", config)
    //创建WorkerActor的Actor和ActorRef
    val workerActorRef: ActorRef = workerActorSystem.actorOf(Props(new WorkerActor(workerName, serverHost, serverPort.toInt, masterName)), s"${workerName}")
    //启动客户端
    workerActorRef ! "start"
  }
}