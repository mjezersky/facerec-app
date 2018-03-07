
package facerec;


import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class MQLink {
    
    private static MQLink currentLink = null;
    
    private Connection connection = null;
    private Channel channel = null;
    
    public static void makeLink() {
        if (currentLink != null) {
            currentLink.close();
        }
        currentLink = new MQLink();
    }
    
    public static MQLink getLink() { return currentLink; };
    
    public void connect(String host) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        connection = factory.newConnection();
        channel = connection.createChannel();
        connection.addShutdownListener(new ShutdownListener() {
            public void shutdownCompleted(ShutdownSignalException cause)
            {
                shutdownListener(cause);
            }
        });
    }
    
    
    
    public void declareQueue(String queueName) throws IOException {
        //channel.queueDelete(queueName);
        channel.queueDeclare(queueName, false, false, false, null);
    }
    
    public void declareExchange(String exchangeName) throws IOException {
        channel.exchangeDeclare(exchangeName, "fanout");
    }

    public void close() {
        try { channel.close(); }
        catch (IOException | TimeoutException ex) { }
        try { connection.close(); }
        catch (IOException ex) { }
    }
    
    public void broadcast(String exchangeName, String message) throws IOException {
        channel.basicPublish(exchangeName, "", null, message.getBytes());
        System.out.println(" [x] Sent '" + message + "'");
    }
    
    public void broadcast(String exchangeName, byte[] message) throws IOException {
        channel.basicPublish(exchangeName, "", null, message);
        System.out.println(" [x] Sent '" + message + "'");
    }
    
    public void publish(String queueName, String message) throws IOException {
        channel.basicPublish("", queueName, null, message.getBytes());
        System.out.println(" [x] Sent '" + message + "'");
    }
    
    public void publish(String queueName, byte[] message) throws IOException {
        channel.basicPublish("", queueName, null, message);
        System.out.print(queueName);
        System.out.println(": Sent bytes");
    }
    
    
    public void registerDispatcher(Dispatcher dispatcher) throws IOException {
    Consumer consumer = new DefaultConsumer(channel) {
        @Override
        public void handleDelivery(String consumerTag, Envelope envelope,
                                   AMQP.BasicProperties properties, byte[] body)
            throws IOException {
          String message = new String(body, "UTF-8");
          //System.out.println("Got MSG, calling dispatcher");
          //System.out.println(message);
          dispatcher.processMessage(message);
        }
      };
      channel.basicConsume(Dispatcher.QUEUE_NAME, true, consumer);
    }
    
    
    private void shutdownListener(ShutdownSignalException cause) {
        if (cause.isHardError()) {
            System.err.println("Error: unexpected channel shutdown.");
            cause.printStackTrace();
        }
    }
    
    public static void test() {
        String QUEUE_NAME = "test";
        try {
            MQLink link = new MQLink();
            link.connect("localhost");
            link.declareQueue(QUEUE_NAME);
            link.publish(QUEUE_NAME, "Hello!");
            link.publish(QUEUE_NAME, "1");
            link.publish(QUEUE_NAME, "2");
            link.publish(QUEUE_NAME, "3");
            link.close();
            
        }
        catch(IOException | TimeoutException ex) {
            System.err.println("MQLink error");
        }
    }
    
}
