
package facerec;


import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AlreadyClosedException;
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
    
    public void connect(String host, int port) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        connection = factory.newConnection();
        channel = connection.createChannel();
        connection.addShutdownListener(new ShutdownListener() {
            @Override
            public void shutdownCompleted(ShutdownSignalException cause)
            {
                shutdownListener(cause);
            }
        });
    }
    
    public boolean isChannelOpen() {
        if (channel == null) { return false; }
        return channel.isOpen();
    }
    
    public boolean isConnectionOpen() {
        if (connection == null) { return false; }
        return connection.isOpen();
    }
    
    public void declareQueue(String queueName) throws IOException {
        //channel.queueDelete(queueName);
        channel.queueDeclare(queueName, false, false, false, null);
    }
    
    public void declareExchange(String exchangeName) throws IOException {
        channel.exchangeDeclare(exchangeName, "fanout");
    }

    public void close() {
        try {channel.abort(); }
        catch (AlreadyClosedException|NullPointerException|IOException ex) {}
        try { channel.queuePurge("feedback-"+FacerecConfig.WORKER_GROUP_NAME); }
        catch (AlreadyClosedException|NullPointerException|IOException ex) {}
        try { channel.close(); }
        catch (AlreadyClosedException|NullPointerException|IOException | TimeoutException ex) {}
        try { connection.abort(); }
        catch (AlreadyClosedException|NullPointerException ex) {}
        try { connection.close(); }
        catch (AlreadyClosedException|NullPointerException|IOException ex) {}
        
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
      channel.basicConsume("feedback-"+FacerecConfig.WORKER_GROUP_NAME, true, consumer);
    }
    
    
    private void shutdownListener(ShutdownSignalException cause) {
        System.out.println("Channel shutdown.");
        if (cause.isHardError()) {
            System.err.println("Error: unexpected channel shutdown.");
            cause.printStackTrace();
        }
    }
    
}
