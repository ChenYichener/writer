# 架构图

![](./images/34-部署RocketMQ集群架构图.jpg)

NameServer是核心的路由服务，所以可以给8核16G的较高配置的机器，但是他一般就是承载Broker注册和心跳、系统的路由表拉取等请求，负载其实很低，因此不需要特别高的机器配置，部署三台也可以实现高可用的效果了。

Broker是最负载最高的，未来要承载高并发写入和海量数据存储，所以可以16核心32G给它



# 一台机器，快速部署RocketMQ集群

## 前置准备

1)  Java的环境

2）Maven的环境

3)  Git的环境

## 步骤

在一台机器上执行了下面的命令来构建Dledger：

```shell
git clone https://github.com/openmessaging/openmessaging-storage-dledger.git

cd openmessaging-storage-dledger

mvn clean install -DskipTests
```

执行了下面的命令来构建RocketMQ：

```shell
git clone https://github.com/apache/rocketmq.git

cd rocketmq

git checkout -b store_with_dledger origin/store_with_dledger

mvn -Prelease-all -DskipTests clean install -U
```

进入一个目录：

```shel
cd distribution/target/apache-rocketmq
```

在这个目录中，需要编辑三个文件，一个是bin/runserver.sh，一个是bin/runbroker.sh，另外一个是bin/tools.sh

在里面找到如下三行，然后将第二行和第三行都删了，同时将第一行的值修改为你自己的JDK的主目录

```she
[ ! -e "$JAVA_HOME/bin/java" ] && JAVA_HOME=$HOME/jdk/java

[ ! -e "$JAVA_HOME/bin/java" ] && JAVA_HOME=/usr/java

[ ! -e "$JAVA_HOME/bin/java" ] && error_exit "Please set the JAVA_HOME variable in your environment, We need java(x64)!"
```

**注**：如果要查看你的JDK装哪儿了，可以用命令：/usr/libexec/java_home -V，修改为你的Java主目录即可

执行下面的命令进行快速RocketMQ集群启动：

```shell
sh bin/dledger/fast-try.sh start
```

这个命令会在当前这台机器上启动一个NameServer和三个Broker，三个Broker其中一个是Master，另外两个是Slave，瞬间就可以组成一个最小可用的RocketMQ集群。

接着使用下面的命令检查一下RocketMQ集群的状态：

```shell
sh bin/mqadmin clusterList -n 127.0.0.1:9876
```

此时你需要等待一会儿，这个命令执行的过程会有点缓慢，大概可能几秒到几十秒过后，你会看到三行记录，说是一个RaftCluster，Broker名称叫做RaftNode00，然后BID是0、1、2，也有可能是0、1、3

这就说明的RocketMQ集群启动成功了，BID为0的就是Master，BID大于0的就都是Slave，其实在这里也可以叫做Leader和Follower

接着就可以尝试一下Slave是如何自动切换为Master的了。

此时我们可以用命令（lsof -i:30921）找出来占用30921端口的进程PID，接着就用kill -9的命令给他杀了，比如我这里占用30921端口的进程PID是4344，那么就执行命令：kill -9 4344

接着等待个10s，再次执行命令查看集群状态：

```shee
sh bin/mqadmin clusterList -n 127.0.0.1:9876
```



此时就会发现作为Leader的BID为0的节点，变成另外一个Broker了，这就是说Slave切换为Master了。



# 正式部署三台NameServe集群

其实RocketMQ集群部署并不难，主要就是在几台机器上做好相应的配置，然后执行一些命令启动NameServer和Broker就可以了。

首先是在三台NameServer的机器上，就按照上面的步骤安装好Java，构建好Dledger和RocketMQ，然后编辑对应的文件，设置好JAVA_HOME就可以了。

此时可以执行如下的一行命令就可以启动NameServer：

```shell
nohup sh mqnamesrv &
```

这个NameServer监听的接口默认就是9876，所以如果你在三台机器上都启动了NameServer，那么他们的端口都是9876，此时我们就成功的启动了三个NameServer了



# 正式部署一组Borker集群

接着需要启动一个Master Broker和两个Slave Broker，这个启动也很简单，分别在上述三台为Broker准备的高配置机器上，安装好Java，构建好Dledger和RocketMQ，然后编辑好对应的文件。

接着就可以执行如下的命令：

```powershell
nohup sh bin/mqbroker -c conf/dledger/broker-n0.conf &
```

这里要给大家说一下，第一个Broker的配置文件是broker-n0.conf，第二个broker的配置文件可以是broker-n1.conf，第三个broker的配置文件可以是broker-n2.conf。

对于这个配置文件里的东西要给大家说明一下，自己要做对应的修改。



用**broker-n0.conf**举例子，然后在每个配置项上加入注释：

```shell
# 这个是集群的名称，你整个broker集群都可以用这个名称

brokerClusterName = RaftCluster


# 这是Broker的名称，比如你有一个Master和两个Slave，那么他们的Broker名称必须是一样的，因为他们三个是一个分组，如果你有另外一组Master和两个Slave，你可以给他们起个别的名字，比如说RaftNode01

brokerName=RaftNode00


# 这个就是你的Broker监听的端口号，如果每台机器上就部署一个Broker，可以考虑就用这个端口号，不用修改

listenPort=30911


# 这里是配置NameServer的地址，如果你有很多个NameServer的话，可以在这里写入多个NameServer的地址

namesrvAddr=127.0.0.1:9876


# 下面两个目录是存放Broker数据的地方，你可以换成别的目录，类似于是/usr/local/rocketmq/node00之类的

storePathRootDir=/tmp/rmqstore/node00

storePathCommitLog=/tmp/rmqstore/node00/commitlog



# 这个是非常关键的一个配置，就是是否启用DLeger技术，这个必须是true

enableDLegerCommitLog=true


# 这个一般建议和Broker名字保持一致，一个Master加两个Slave会组成一个Group

dLegerGroup=RaftNode00


# 这个很关键，对于每一组Broker，你得保证他们的这个配置是一样的，在这里要写出来一个组里有哪几个Broker，比如在这里假设有三台机器部署了Broker，要让他们作为一个组，那么在这里就得写入他们三个的ip地址和监听的端口号

dLegerPeers=n0-127.0.0.1:40911;n1-127.0.0.1:40912;n2-127.0.0.1:40913


# 这个是代表了一个Broker在组里的id，一般就是n0、n1、n2之类的，这个你得跟上面的dLegerPeers中的n0、n1、n2相匹配

dLegerSelfId=n0


# 这个是发送消息的线程数量，一般建议你配置成跟你的CPU核数一样，比如我们的机器假设是24核的，那么这里就修改成24核

sendMessageThreadPoolNums=24
```

其实最关键的是，你的Broker是分为多组的，每一组是三个Broker，一个Master和两个Slave。



对每一组Broker，他们的Broker名称、Group名称都是一样的，然后你得给他们配置好一样的dLegerPeers（里面是组内三台Broker的地址）



然后他们得配置好对应的NameServer的地址，最后还有就是每个Broker有自己的ID，在组内是唯一的就可以了，比如说不同的组里都有一个ID为n0的broker，这个是可以的。



所以按照这个思路就可以轻松的配置好一组Broker，在三台机器上分别用命令启动Broker即可。启动完成过后，可以跟NameServer进行通信，检查Broker集群的状态，就是如下命令：

```shell
sh bin/mqadmin clusterList -n 127.0.0.1:9876
```



# 编写代码进行压测

接着就可以编写最基本的生产者和消费者代码，准备执行压测了。

可以新建两个工程，一个是生产者，一个是消费者，两个工程都需要加入下面的依赖：

```xml
<dependency>
	<groupId>org.apache.rocketmq</groupId>
	<artifactId>rocketmq-client</artifactId>
	<version>4.5.1</version>
</dependency>
```

然后生产者的示例代码如下:

简单来说，就是只要一运行，就立马不停的在while死循环里去发送消息，根据需要可以设置为多个线程

```java
public class SyncProducer {
    public static void main(String[] args) throws Exception {
        
        // create producer 
        final DefaultMQProducer producer = new  DefaultMQProducer("test_producer");
        // Specify name server addresses.
        producer.setNamesrvAddr("localhost:9876");
        //Launch the instance.
        producer.start();

        // 死循环不停的发送消息，可以设置为多线程启动
        // 然后使用producer循环的发送消息
        for (int i = 0; i < 10; i++) {
            new  Thread(() -> {

                while (true) {
                    //Create a message instance, specifying topic, tag and message body.
                    Message msg = new Message("TopicTest" /*Topic */, "TagA" /* Tag */, ("Test").getBytes(RemotingHelper.DEFAULT_CHARSET) /* Message body */);

                    //Call send message to deliver message to one of brokers.
                    SendResult sendResult = producer.send(msg);
                }
            }).start();
        }
   

        // 程序卡在这里不让程序停止
        while (true) {
            // pass
        }

        //Shut down once the producer instance is not longer in use.
        producer.shutdown();
    }
}
```

消费者代码示例

```java
public class Consumer {

    public static void main(String[] args) throws InterruptedException, MQClientException {

        // Instantiate with specified consumer group name.
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("test_consumer_group");
         
        // Specify name server addresses.
        consumer.setNamesrvAddr("localhost:9876");
        
        // Subscribe one more more topics to consume.
        consumer.subscribe("TopicTest", "*");
        
        // Register callback to execute on arrival of messages fetched from brokers.
        consumer.registerMessageListener(new MessageListenerConcurrently() {

            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                ConsumeConcurrentlyContext context) {
                System.out.printf("%s Receive New Messages: %s %n", Thread.currentThread().getName(), msgs);
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        //Launch the consumer instance.
        consumer.start();

        System.out.printf("Consumer Started.%n");
    }
}
```







