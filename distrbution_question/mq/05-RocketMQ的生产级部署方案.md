# NameServer集群化部署，保证高可用

首先第一步，我们要让NameServer集群化部署，建议可以部署在三台机器上，这样可以充分保证NameServer作为路由中心的可用性，哪怕是挂掉两台机器，只要有一个NameServer还在运行，就能保证MQ系统的稳定性。

![](./images/26-生产部署方案-NameServer.jpg)

NameServer的设计是采用的Peer-to-Peer的模式来做的，也就是可以集群化部署，但是里面任何一台机器都是独立运行的，跟其他的机器没有任何通信。



每台NameServer实际上都会有完整的集群路由信息，包括所有的Broker节点信息，我们的数据信息，等等。所以只要任何一台NameServer存活下来，就可以保证MQ系统正常运行，不会出现故障。



因此NameServer的集群化部署是必须的第一步





# 基于Dledger的Broker主从架构技术

其次，就是要考虑我们的Broker集群应该如何部署，采用什么方式来部署。



如果采用RocketMQ 4.5以前的那种普通的Master-Slave架构来部署，能在一定程度上保证数据不丢失，也能保证一定的可用性。



但是那种方式的缺陷是很明显的，最大的问题就是当Master Broker挂了之后，没办法让Slave Broker自动切换为新的Master Broker，需要手工做一些运维操作，修改配置以及重启机器才行，这个非常麻烦。

在手工运维的期间，可能就会导致系统的不可用。



所以既然现在RocketMQ 4.5之后已经基于Dledger技术实现了可以自动让Slave切换为Master的功能，那么我们肯定是选择基于Dledger的主备自动切换的功能来进行生产架构的部署。



而且Dledger技术是要求至少得是一个Master带两个Slave，这样有三个Broke组成一个Group，也就是作为一个分组来运行。一旦Master宕机，他就可以从剩余的两个Slave中选举出来一个新的Master对外提供服务。

![](./images/27-生产部署方案-Broker.jpg)

**在上图可以看到，每个Broker（不论是Master和Slave）都会把自己注册到所有的NameServer上去。**



**注：图中没有画出Slave Broker注册到NameServer。**



然后Master Broker还会把数据同步给两个Slave Broker，保证一份数据在不同机器上有多份副本。



# Broker是如何跟NameServer通信的



之前我们的时候就说过，这个Broker会每隔30秒发送心跳到所有的NameServer上去，然后每个NameServer都会每隔10s检查一次有没有哪个Broker超过120s没发送心跳的，如果有，就认为那个Broker已经宕机了，从路由信息里要摘除这个Broker。



**首先，Broker跟NameServer之间的通信是基于什么协议来进行的？**



HTTP协议？RPC调用？还是TCP长连接？首先我们要搞明白这个。



在RocketMQ的实现中，采用的是**TCP长连接**进行通信。



也就是说，Broker会跟每个NameServer都建立一个TCP长连接，然后定时通过TCP长连接发送心跳请求过去

![](./images/28-NameServer和Broker基于长连接通信.jpg)



所以各个NameServer就是通过跟Broker建立好的长连接不断收到心跳包，然后定时检查Broker有没有120s都没发送心跳包，来判定集群里各个Broker到底挂掉了没有。



# 使用MQ的系统要多机器集群化部署









# MQ的核心数据模型，Topic是什么？

