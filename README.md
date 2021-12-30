一、SpringBoot+RabbitMQ 实现 RPC 调用  (源码请查看：mq_demo)

说到 RPC（Remote Procedure Call Protocol 远程过程调用协议），小伙伴们脑海里蹦出的估计都是 RESTful API、Dubbo、WebService、Java RMI、CORBA 等。
其实，RabbitMQ 也给我们提供了 RPC 功能，并且使用起来很简单。
今天松哥通过一个简单的案例来和大家分享一下 Spring Boot+RabbitMQ 如何实现一个简单的 RPC 调用。
注意
关于 RabbitMQ 实现 RPC 调用，有的小伙伴可能会有一些误解，心想这还不简单？搞两个消息队列 queue_1 和 queue_2，首先客户端发送消息到 queue_1 上，服务端监听 queue_1 上的消息，收到之后进行处理；处理完成后，服务端发送消息到 queue_2 队列上，然后客户端监听 queue_2 队列上的消息，这样就知道服务端的处理结果了。
这种方式不是不可以，就是有点麻烦！RabbitMQ 中提供了现成的方案可以直接使用，非常方便。接下来我们就一起来学习下。
1. 架构
   先来看一个简单的架构图：
   图片![image](https://user-images.githubusercontent.com/67642821/147717986-34273efe-a906-46f0-b2b7-bcd2aeddf0da.png)

   这张图把问题说的很明白了：
   首先 Client 发送一条消息，和普通的消息相比，这条消息多了两个关键内容：一个是 correlation_id，这个表示这条消息的唯一 id，还有一个内容是 reply_to，这个表示消息回复队列的名字。
   Server 从消息发送队列获取消息并处理相应的业务逻辑，处理完成后，将处理结果发送到 reply_to 指定的回调队列中。
   Client 从回调队列中读取消息，就可以知道消息的执行情况是什么样子了。
   这种情况其实非常适合处理异步调用。
2. 实践
   接下来我们通过一个具体的例子来看看这个怎么玩。
   2.1 客户端开发
   首先我们来创建一个 Spring Boot 工程名为 producer，作为消息生产者，创建时候添加 web 和 rabbitmq 依赖，如下图：
   图片
   项目创建成功之后，首先在 application.properties 中配置 RabbitMQ 的基本信息，如下：
   spring.rabbitmq.host=localhost
   spring.rabbitmq.port=5672
   spring.rabbitmq.username=guest
   spring.rabbitmq.password=guest
   spring.rabbitmq.publisher-confirm-type=correlated
   spring.rabbitmq.publisher-returns=true
   这个配置前面四行都好理解，我就不赘述，后面两行：首先是配置消息确认方式，我们通过 correlated 来确认，只有开启了这个配置，将来的消息中才会带 correlation_id，只有通过 correlation_id 我们才能将发送的消息和返回值之间关联起来。最后一行配置则是开启发送失败退回。
   接下来我们来提供一个配置类，如下：
   /**
* @author ：jerry
* @date ：Created in 2021/12/29 13:11
* @description：
* @version: V1.1
  */
  @Configuration
  public class RabbitConfig {

  public static final String RPC_QUEUE1 = "queue_1";
  public static final String RPC_QUEUE2 = "queue_2";
  public static final String RPC_EXCHANGE = "rpc_exchange";

  /**
    * 设置消息发送RPC队列
      */
      @Bean
      Queue msgQueue() {
      return new Queue(RPC_QUEUE1);
      }

  /**
    * 设置返回队列
      */
      @Bean
      Queue replyQueue() {
      return new Queue(RPC_QUEUE2);
      }

  /**
    * 设置交换机
      */
      @Bean
      TopicExchange exchange() {
      return new TopicExchange(RPC_EXCHANGE);
      }

  /**
    * 请求队列和交换器绑定
      */
      @Bean
      Binding msgBinding() {
      return BindingBuilder.bind(msgQueue()).to(exchange()).with(RPC_QUEUE1);
      }

  /**
    * 返回队列和交换器绑定
      */
      @Bean
      Binding replyBinding() {
      return BindingBuilder.bind(replyQueue()).to(exchange()).with(RPC_QUEUE2);
      }


    /**
     * 使用 RabbitTemplate发送和接收消息
     * 并设置回调队列地址
     */
    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setReplyAddress(RPC_QUEUE2);
        template.setReplyTimeout(6000);
        return template;
    }


    /**
     * 给返回队列设置监听器
     */
    @Bean
    SimpleMessageListenerContainer replyContainer(ConnectionFactory connectionFactory) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(RPC_QUEUE2);
        container.setMessageListener(rabbitTemplate(connectionFactory));
        return container;
    }
}
这个配置类中我们分别配置了消息发送队列 msgQueue 和消息返回队列 replyQueue，然后将这两个队列和消息交换机进行绑定。这个都是 RabbitMQ 的常规操作，没啥好说的。
在 Spring Boot 中我们负责消息发送的工具是 RabbitTemplate，默认情况下，系统自动提供了该工具，但是这里我们需要对该工具重新进行定制，主要是添加消息发送的返回队列，最后我们还需要给返回队列设置一个监听器。
好啦，接下来我们就可以开始具体的消息发送了：
/**
* @author ：jerry
* @date ：Created in 2021/12/29 13:11
* @description：
* @version: V1.1
  */
  @RestController
  public class RpcClientController {

  private static final Logger logger = LoggerFactory.getLogger(RpcClientController.class);

  @Autowired
  private RabbitTemplate rabbitTemplate;

  @GetMapping("/send")
  public String send(String message) {
  // 创建消息对象
  Message newMessage = MessageBuilder.withBody(message.getBytes()).build();

       logger.info("client send：{}", newMessage);

       //客户端发送消息
       Message result = rabbitTemplate.sendAndReceive(RabbitConfig.RPC_EXCHANGE, RabbitConfig.RPC_QUEUE1, newMessage);

       String response = "";
       if (result != null) {
           // 获取已发送的消息的 correlationId
           String correlationId = newMessage.getMessageProperties().getCorrelationId();
           logger.info("correlationId:{}", correlationId);

           // 获取响应头信息
           HashMap<String, Object> headers = (HashMap<String, Object>) result.getMessageProperties().getHeaders();

           // 获取 server 返回的消息 id
           String msgId = (String) headers.get("spring_returned_message_correlation");

           if (msgId.equals(correlationId)) {
               response = new String(result.getBody());
               logger.info("client receive：{}", response);
           }
       }
       return response;
  }
  }
  这块的代码其实也都是一些常规代码，我挑几个关键的节点说下：
  消息发送调用 sendAndReceive 方法，该方法自带返回值，返回值就是服务端返回的消息。
  服务端返回的消息中，头信息中包含了 spring_returned_message_correlation 字段，这个就是消息发送时候的 correlation_id，通过消息发送时候的 correlation_id 以及返回消息头中的 spring_returned_message_correlation 字段值，我们就可以将返回的消息内容和发送的消息绑定到一起，确认出这个返回的内容就是针对这个发送的消息的。
  这就是整个客户端的开发，其实最最核心的就是 sendAndReceive 方法的调用。调用虽然简单，但是准备工作还是要做足够。例如如果我们没有在 application.properties 中配置 correlated，发送的消息中就没有 correlation_id，这样就无法将返回的消息内容和发送的消息内容关联起来。
  2.2 服务端开发
  再来看看服务端的开发。
  首先创建一个名为 consumer 的 Spring Boot 项目，创建项目添加的依赖和客户端开发创建的依赖是一致的，不再赘述。
  然后配置 application.properties 配置文件，该文件的配置也和客户端中的配置一致，不再赘述。
  接下来提供一个 RabbitMQ 的配置类，这个配置类就比较简单，单纯的配置一下消息队列并将之和消息交换机绑定起来，如下：
  /**
* @author ：jerry
* @date ：Created in 2021/12/29 13:11
* @description：
* @version: V1.1
  */
  @Configuration
  public class RabbitConfig {

  public static final String RPC_QUEUE1 = "queue_1";
  public static final String RPC_QUEUE2 = "queue_2";
  public static final String RPC_EXCHANGE = "rpc_exchange";

  /**
    * 配置消息发送队列
      */
      @Bean
      Queue msgQueue() {
      return new Queue(RPC_QUEUE1);
      }

  /**
    * 设置返回队列
      */
      @Bean
      Queue replyQueue() {
      return new Queue(RPC_QUEUE2);
      }

  /**
    * 设置交换机
      */
      @Bean
      TopicExchange exchange() {
      return new TopicExchange(RPC_EXCHANGE);
      }

  /**
    * 请求队列和交换器绑定
      */
      @Bean
      Binding msgBinding() {
      return BindingBuilder.bind(msgQueue()).to(exchange()).with(RPC_QUEUE1);
      }

  /**
    * 返回队列和交换器绑定
      */
      @Bean
      Binding replyBinding() {
      return BindingBuilder.bind(replyQueue()).to(exchange()).with(RPC_QUEUE2);
      }
      }
      最后我们再来看下消息的消费：
      @Component
      public class RpcServerController {
      private static final Logger logger = LoggerFactory.getLogger(RpcServerController.class);
      @Autowired
      private RabbitTemplate rabbitTemplate;

  @RabbitListener(queues = RabbitConfig.RPC_QUEUE1)
  public void process(Message msg) {
  logger.info("server receive : {}",msg.toString());
  Message response = MessageBuilder.withBody(("i'm receive:"+new String(msg.getBody())).getBytes()).build();
  CorrelationData correlationData = new CorrelationData(msg.getMessageProperties().getCorrelationId());
  rabbitTemplate.sendAndReceive(RabbitConfig.RPC_EXCHANGE, RabbitConfig.RPC_QUEUE2, response, correlationData);
  }
  }
  这里的逻辑就比较简单了：
  服务端首先收到消息并打印出来。
  服务端提取出原消息中的 correlation_id。
  服务端调用 sendAndReceive 方法，将消息发送给 RPC_QUEUE2 队列，同时带上 correlation_id 参数。
  服务端的消息发出后，客户端将收到服务端返回的结果。
  OK，大功告成。
  2.3 测试
  接下来我们进行一个简单测试。
  首先启动 RabbitMQ。
  接下来分别启动 producer 和 consumer，然后在 postman 中调用 producer 的接口进行测试，如下：
  图片
  可以看到，已经收到了服务端的返回信息。
  来看看 producer 的运行日志：
  图片
  可以看到，消息发送出去后，同时也收到了 consumer 返回的信息。
  图片
  可以看到，consumer 也收到了客户端发来的消息。
3. 小结
   好啦，一个小小的案例，带小伙伴们体验一把 RabbitMQ 实现 RPC 调用。


二、RabbitMQ之消息回调和手动接收消息  (源码请查看：mq_demo2)
1、什么是消息回调

消息回调，其实就是消息确认(生产者推送消息成功，消费者接收消息成功)
2、为什么要进行消息确认
经常会听到丢消息的字眼, 对于程序来说，发送者没法确认是否发送成功，消费者处理失败也无法反馈，没有消息确认机制，就会出现消息莫名其妙的没了，也不知道什么情况

3、消息发送的两种回调
ConfirmCallBack
流程：由生产者 -------> 交换机（Exchange）

ReturnCallBack:
流程：由交换机（Exchange）--------> 队列（Queue）
注意：想触发ReturnCallBack回调必须开启spring.rabbitmq.template.mandatory
