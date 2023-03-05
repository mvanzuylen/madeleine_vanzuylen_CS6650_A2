import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

// Make multithreaded
// data safe data structure (hashmap?)

public class Madeleine_van_Zuylen_Consumer1 {
  final static private int NUMTHREADS = 50;
  private final static String QUEUE_NAME = "queue1";
  //private static ConcurrentHashMap<String, ArrayList<ArrayList<String>>> twinderMap = new ConcurrentHashMap();
  private static ConcurrentHashMap<String, ArrayList<Integer>> twinderMapLikesDislikes = new ConcurrentHashMap();
  private static ConcurrentHashMap<String, ArrayList<String>> twinderMap100UMatches = new ConcurrentHashMap();

  synchronized public static void updateConcurrentHashMap(String message) {
    String[] messageArray = message.replace("\"", "").split("[,:{}]");
    String key = messageArray[3];
    String direction = messageArray[1];
    String left = "left";
    String right = "right";

    twinderMapLikesDislikes.compute(key, (k, v) -> {
      ArrayList<Integer> val = v;
      if(val == null) {
        val = new ArrayList<Integer>(Collections.nCopies(2, 0));
      }

      if (direction.equals(left)){
        Integer value = val.get(1);
        value = value + 1;
        val.set(1, value);
      } else if (direction.equals(right)) {
        Integer value = val.get(0);
        value = value + 1;
        val.set(0, value);
      }
      return val;
    });
    twinderMap100UMatches.compute(key, (k, v) -> {
      ArrayList<String> val = v;
      if(val == null)
        val = new ArrayList<>();
      if (direction.equals(right) && val.size() < 100){
        val.add(messageArray[5]);
      }
      return val;
    });
  }

  /*
  public static void declareBindings() throws IOException, TimeoutException {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    //factory.setHost("35.89.252.145");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    channel.queueBind(QUEUE_NAME, "exchange1", "");
    channel.close();
  }
*/
  public static void main(String[] argv) throws Exception {
    CountDownLatch completed = new CountDownLatch(NUMTHREADS);
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("35.89.78.12");
    //factory.setHost("localhost");
    Connection connection = factory.newConnection();
    for (int i = 0; i < NUMTHREADS; i++) {
      Runnable thread = () -> {
        try {
          Channel channel = connection.createChannel();
          channel.queueDeclare(QUEUE_NAME, false, false, false, null);
          //added
          channel.queueBind(QUEUE_NAME, "exchange1", "");

          System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

          DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            updateConcurrentHashMap(message);
            //System.out.println(" [x] Received '" + message + "'");
          };
          channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {});
        } catch (IOException e){
          e.printStackTrace();
        } finally {
          completed.countDown();
        }
      };
      new Thread(thread).start();
    }
    completed.await();
  }
}

