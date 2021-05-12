# Akka 网络编程-模仿Spark Master-Worker 进程通讯项目

## workerActor 
部署在worker节点上，负责获取work节点的性能监控数据

部署方式：`nohup java -jar tmp/data/WorkerActor.jar {worker.ip} 10001 WorkerActor-01 {master.ip} 10000 MasterActor >log.log &`

## masterActor 
部署在master节点上，负责汇总所有worker节点的性能

部署方式：`nohup java -jar root/jars/wzy/MasterActor.jar {master} 10000 MasterActor > log.log &`