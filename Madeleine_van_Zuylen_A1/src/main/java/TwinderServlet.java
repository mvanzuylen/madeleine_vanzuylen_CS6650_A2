import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.ConnectionFactory;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeoutException;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import com.google.gson.Gson;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

@WebServlet(name = "TwinderServlet", value = "/TwinderServlet")
public class TwinderServlet extends HttpServlet {

  private final static String QUEUE_NAME = "queue1";
  private final static String QUEUE_NAME2 = "queue2";

  public Connection connection;
  public RMQChannelPool pool;
  //String HOSTNAME = "localhost";
  String HOSTNAME = "54.185.234.125";
  String USERNAME = "guest";
  String PASSWORD = "guest";
  Integer PORT = 5672;

  @Override
  public void init(ServletConfig config) throws ServletException{
    int MAX_SIZE = 50;
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(HOSTNAME);
    //factory.setVirtualHost(VIRTUALHOST);
    factory.setUsername(USERNAME);
    factory.setPassword(PASSWORD);
    factory.setPort(PORT);
    try {
      connection = factory.newConnection();
      RMQChannelFactory rmqChannelFactory = new RMQChannelFactory(connection);
      pool = new RMQChannelPool(MAX_SIZE, rmqChannelFactory);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (TimeoutException e) {
      e.printStackTrace();
    }

    try {
      declareExchange();
    } catch (TimeoutException | IOException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  @Override
  public void destroy(){

  }

  public void sendMessageToQueue(String message) throws Exception {
    try (
        Channel channel = pool.borrowObject()) {
      channel.queueDeclare(QUEUE_NAME, false, false, false, null);
      channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
      System.out.println(" [x] Sent '" + message + "'");
    }
  }

  public void declareExchange() throws Exception {
    Channel channel = pool.borrowObject();
    channel.queueDeclare(QUEUE_NAME, false, false, false, null);
    channel.queueDeclare(QUEUE_NAME2, false, false, false, null);
    channel.exchangeDeclare("exchange1", BuiltinExchangeType.FANOUT, true);
    channel.queueBind(QUEUE_NAME, "exchange1", "");
    channel.queueBind(QUEUE_NAME2, "exchange1", "");
    pool.returnObject(channel);
    //channel.close();
  }

  public void publishMessageToFanOutExchange(String message) throws Exception {
    Channel channel = pool.borrowObject();
    channel.basicPublish("exchange1", "", null, message.getBytes());
    pool.returnObject(channel);
    //channel.close();
    System.out.println(" [x] Sent to exchange:  '" + message + "'");
  }

  private boolean isUrlValid(String[] urlPath) {
    // TODO: validate the request url path according to the API spec
    // urlPath  = "/1/seasons/2019/day/1/skier/123"
    // urlParts = [, 1, seasons, 2019, day, 1, skier, 123]
    String[] validURLSwipeLeft = {"", "swipe", "left"};
    String[] validURLSwipeRight = {"", "swipe", "right"};

    if (Arrays.equals(urlPath, validURLSwipeLeft) || (Arrays.equals(urlPath, validURLSwipeRight))){
      return true;
    }
    return false;
  }

  private boolean validateSwipeDetails(String swipeDetails)
      throws IOException, ServletException {
    //Collection<Part> parts = req.getParts();
    //Gson gson= new Gson();
    //String partsString = gson.toJson(parts);
    //System.out.println(partsString);

    String[] messageArray = swipeDetails.split("[,:{}]");

    // Check if swipe if 'left' or 'right
    if (!(messageArray[1] == "true" || messageArray[1] == "false")){
      return false;
    }
    // Check user id is between 1 and 5000
    if (!(Integer.valueOf(messageArray[3]) < 5001 && Integer.valueOf(messageArray[3]) > 0)){
      return false;
    }
    // Check swipee id is between 1 and 1000000
    if (!(Integer.valueOf(messageArray[5]) < 1000001 && Integer.valueOf(messageArray[5]) > 0)){
      return false;
    }
    // Check comment is 256 characters
    if (!(messageArray[7].length() == 256)){
      return false;
    }
    return true;
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    System.out.println("here2");

    res.setContentType("application/json");
    String urlPath = req.getPathInfo();

    // check we have a URL!
    if (urlPath == null || urlPath.isEmpty()) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write("missing parameters");
      return;
    }

    String[] urlParts = urlPath.split("/");

    // Check if URL is valid
    if (!isUrlValid(urlParts)) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write("missing parameters");
    } else {

      // Create string from swipe details
      StringBuilder stringBuilder = new StringBuilder();
      BufferedReader reader = req.getReader();

      String line;
      stringBuilder.append("\"swipe\":" + "\"" + urlParts[2] + "\"");
      while ((line = reader.readLine()) != null){
        stringBuilder.append(line);
      }
      String swipeDetails = stringBuilder.toString();

      // validate payload
      if (!validateSwipeDetails(swipeDetails)){
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write("Swipe Details Incorrect");
      }

      try {
        publishMessageToFanOutExchange(swipeDetails);
      } catch (TimeoutException e) {
        e.printStackTrace();
      } catch (Exception e) {
        e.printStackTrace();
      }

      /*
      try {
        sendMessageToQueue(swipeDetails);
      } catch (Exception e) {
        e.printStackTrace();
      }
*/
      res.setStatus(HttpServletResponse.SC_OK);
      res.getWriter().write("It works!!");
    }
  }
}
