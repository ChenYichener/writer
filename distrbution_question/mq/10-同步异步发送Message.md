# 同步发送消息

```java
package chenyichen.info.rocketmq.sample.producer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;

@Slf4j
@SuppressWarnings("all")
public class RocketMQProducer {

    private static DefaultMQProducer producer;

    static {

        try {
            producer = new DefaultMQProducer("order_producer_group");
            // 这里是设置NameServer的位置，让它可以拉去路由信息
            // 这样才知道每个Topic数据分散在哪个Broker机器上
            // 然后才可以将数据发送哪个Broker上
            producer.setNamesrvAddr("39.106.108.194:9876");
            // 这里是启动一个Producer
            producer.start();
        } catch (MQClientException e) {
            e.printStackTrace();
        }
    }

    /**
     * 同步发送
     *
     * @param topic
     * @param message
     * @throws Exception
     */
    public static void send(String topic, String message) throws Exception {
        Message msg = new Message(
                topic, // 指定消息发送到哪个Topic中
                "TagA", // 消息的Tag
                message.getBytes(RemotingHelper.DEFAULT_CHARSET) // 消息体
        );
        // 利用Producer发送消息
        SendResult sendResult = producer.send(msg);
        System.out.printf("%s%n", sendResult);
    }

```





# 异步发送消息





# 单向发送消息





# Push消费模式

```java
@Slf4j
public class RocketMQConsumer {

    public static void consumer() throws MQClientException, IOException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("order_consumer_group");
        consumer.setNamesrvAddr("39.106.108.194:9876");
        consumer.subscribe("order_topic", "TagA");
        consumer.registerMessageListener(new MessageListenerConcurrently() {

            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                log.info("接受到了消息【{}】", list);
                for (MessageExt messageExt : list) {
                    String body = new String(messageExt.getBody(), CharsetUtil.UTF_8);
                    log.info("消息体是:【{}】", body);
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        consumer.start();
        System.in.read();
    }
}

```



注意这里的Consumer类名是： DefaultMQPushConsumer 

 从类名中我们可以提取出来一个关键的信息：Push。其实从这里我们就能看出来，当前我们使用的消息消费实际上是Push模式。 

那么什么是Push消费模式呢？

其实很简单，就是Broker会主动把消息发送给你的消费者，你的消费者是被动的接收Broker推送给过来的消息，然后进行处理。

这个就是所谓的Push模式，意思就是Broker主动推送消息给消费者。
 

# Pull消费模式	

