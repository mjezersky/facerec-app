
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

/**
 * Class to create, manage and close RabbitMQ connections.
 * @author Matous Jezersky
 */
public class MQLink {
    
    private static MQLink currentLink = null;
    
    private Connection connection = null;
    private Channel channel = null;
    
    /**
     * Creates a single static connection.
     */
    public static void makeLink() {
        if (currentLink != null) {
            currentLink.close();
        }
        currentLink = new MQLink();
    }
    
    /**
     * Returns the current static connection.
     * @return current static connection
     */
    public static MQLink getLink() { return currentLink; };
    
    public void connect() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(FacerecConfig.RABBIT_MQ_USERNAME);
        factory.setPassword(FacerecConfig.RABBIT_MQ_PASSWORD);
        factory.setConnectionTimeout(FacerecConfig.RABBIT_MQ_TIMEOUT);
        factory.setHost(FacerecConfig.RABBIT_MQ_SERVER_IP);
        factory.setPort(FacerecConfig.RABBIT_MQ_SERVER_PORT);
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
    
    /**
     * Returns whether the current channel is open.
     * @return true if current channel is open, false otherwise
     */
    public boolean isChannelOpen() {
        if (channel == null) { return false; }
        return channel.isOpen();
    }
    
    /**
     * Returns whether the current connection is open.
     * @return true if current connection is open, false otherwise
     */
    public boolean isConnectionOpen() {
        if (connection == null) { return false; }
        return connection.isOpen();
    }
    
    /**
     * Declares a RabbitMQ queue.
     * @param queueName queue name
     * @throws IOException
     */
    public void declareQueue(String queueName) throws IOException {
        //channel.queueDelete(queueName);
        channel.queueDeclare(queueName, false, false, false, null);
    }
    
    /**
     * Declares a RabbitMQ exchange.
     * @param exchangeName exchange name
     * @throws IOException
     */
    public void declareExchange(String exchangeName) throws IOException {
        channel.exchangeDeclare(exchangeName, "fanout");
    }

    /**
     * Closes everything safely, even if something crashed.
     */
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
    
    /**
     * Broadcasts a message.
     * @param exchangeName exchange to broadcast in
     * @param message message to broadcast
     * @throws IOException
     */
    public void broadcast(String exchangeName, String message) throws IOException {
        channel.basicPublish(exchangeName, "", null, message.getBytes());
    }
    
    /**
     * Broadcasts a message.
     * @param exchangeName exchange to broadcast in
     * @param message message to broadcast
     * @throws IOException
     */
    public void broadcast(String exchangeName, byte[] message) throws IOException {
        channel.basicPublish(exchangeName, "", null, message);
    }
    
    /**
     * Publishes a message to a queue.
     * @param queueName queue to publish in
     * @param message message to publish
     * @throws IOException
     */
    public void publish(String queueName, String message) throws IOException {
        channel.basicPublish("", queueName, null, message.getBytes());
    }
    
    /**
     * Publishes a message to a queue.
     * @param queueName queue to publish in
     * @param message message to publish
     * @throws IOException
     */
    public void publish(String queueName, byte[] message) throws IOException {
        channel.basicPublish("", queueName, null, message);
    }
    
    /**
     * Assigns a dispatcher for message handling.
     * @param dispatcher dispatcher for message handling
     * @throws IOException
     */
    public void registerDispatcher(Dispatcher dispatcher) throws IOException {
    Consumer consumer = new DefaultConsumer(channel) {
        @Override
        public void handleDelivery(String consumerTag, Envelope envelope,
                                   AMQP.BasicProperties properties, byte[] body)
            throws IOException {
          String message = new String(body, "UTF-8");
          dispatcher.processMessage(message);
        }
      };
      channel.basicConsume("feedback-"+FacerecConfig.WORKER_GROUP_NAME, true, consumer);
    }
    
    // channel shutdown listener
    private void shutdownListener(ShutdownSignalException cause) {
        // only for debugging purposes
        // cause.printStackTrace();
    }
    
}
