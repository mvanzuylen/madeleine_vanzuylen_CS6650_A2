import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SwipeApi;
import io.swagger.client.model.SwipeDetails;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MultithreadedClient {

  final static private int NUMTHREADS = 100;
  private static int NUMPOSTS = 5000; //500000
  private static String url = "http://34.214.111.249:8080/Madeleine_van_Zuylen_A1_war/twinder";
  //private static String url = "http://localhost:8080/Madeleine_van_Zuylen_A1_war_exploded/twinder/";
  private static AtomicInteger numSuccessfulRequests = new AtomicInteger(0);
  private static AtomicInteger numUnSuccessfulRequests = new AtomicInteger(0);
  private static String postCsv = "posts.csv";
  private static List<String[]> postData = new ArrayList<>();
  private static List<Long> latecyData = new ArrayList<>();
  private static List<Integer> endTimeData = new ArrayList<>();

  public static String convertToCSV(String[] data) {
    return Stream.of(data)
        .collect(Collectors.joining(","));
  }

  synchronized public static void addPostData(long threadStartTime, String post, long latency, int responseCode) {
    File csvOutputFile = new File(postCsv);
    postData.add(new String[]
        {Long.toString(threadStartTime), post, Long.toString(latency), Integer.toString(responseCode) });
    latecyData.add(latency);
    endTimeData.add((int) Math.floor((threadStartTime + latency)/1000));
  }

  public static void writeToCsv(String csvFile) throws IOException {
    File csvOutputFile = new File(postCsv);
    try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
      postData.stream()
          .map(MultithreadedClient::convertToCSV)
          .forEach(pw::println);
    }
  }

 // Calculates the 99th percentile of latencies:
  public static long percentile99(List<Long> latencies) {
    int index = (int) Math.ceil(99 / 100.0 * latencies.size());
    return latencies.get(index-1);
  }

  // Calculate metrics from Task 3:
  public static void calculateMetrics(String csvFile, long totalTime){
    Collections.sort(latecyData);
    int sum = latecyData.stream().mapToInt(Long::intValue).sum();
    System.out.println("");
    System.out.println("Task 3: ");
    System.out.println("Mean Response Time (ms): " + (double) sum/latecyData.size());
    System.out.println("Median Response Time (ms): " + Long.toString(latecyData.get(latecyData.size()/2)));
    System.out.println("Throughput (requests/sec): " + (double) latecyData.size()/totalTime * 1000);
    System.out.println("p99 Response Time (ms): " + percentile99(latecyData));
    System.out.println("Max Response Time (ms): " + Long.toString(latecyData.get(latecyData.size() - 1)));
    System.out.println("Min Response Time (ms): " + Long.toString(latecyData.get(0)));
  }

  // Creates arraylist with number of seconds (index of array) and counts of the number of finished threads (value)
  public static List<Integer> generatePlotArray(List<Integer> endTimeData){
    Collections.sort(endTimeData);
    int numSeconds = endTimeData.get(endTimeData.size()-1) - endTimeData.get(0);
    List<Integer> plotArray = new ArrayList<Integer>(Collections.nCopies( numSeconds + 1, 0));

    for (int i = 0; i < endTimeData.size(); i++){
      int value =  endTimeData.get(i);
      value = value - endTimeData.get(0);
      int oldValue = plotArray.get(value);
      plotArray.set(value, oldValue + 1);
    }
    return plotArray;
  }

  public static void main(String[] args) throws InterruptedException, IOException {


    CountDownLatch completed = new CountDownLatch(NUMTHREADS);

    long startTime = System.currentTimeMillis();

      for (int i = 0; i < NUMTHREADS; i++) {

        Runnable thread = () -> {
          SwipeApi swipeApi = new SwipeApi();
          ApiClient apiClient = swipeApi.getApiClient();
          apiClient.setBasePath(url);
          swipeApi.setApiClient(apiClient);

        try {
          // For loop for number of posts/threads
          for (int j = 0; j < NUMPOSTS/NUMTHREADS; j++) {
            // Set swipe details
            String[] values = DataGeneration.getValues();
            SwipeDetails swipeDetails = new SwipeDetails();
            swipeDetails.setSwiper(values[1]);
            swipeDetails.setSwipee(values[2]);
            swipeDetails.setComment(values[3]);
            ApiResponse<Void> response;

            long threadStartTime = System.currentTimeMillis();
            int responseCode = 0;
            // Try 5x (if there is a failure)
            for (int z = 0; z < 5; z++) {
              response = swipeApi.swipeWithHttpInfo(swipeDetails, values[0]);
              // if response code is 200 and break this for loop and if not, keep trying
              responseCode = response.getStatusCode();
              if (responseCode == 200) {
                numSuccessfulRequests.incrementAndGet();
                break;
              } else {
                numUnSuccessfulRequests.incrementAndGet();
              }
            }
            long threadEndTime = System.currentTimeMillis();
            long latency = threadEndTime - threadStartTime;
            addPostData(threadStartTime, "POST", latency, responseCode);
          }
        } catch (ApiException e) {
          e.printStackTrace();
        } finally {
          completed.countDown();
        }
      };
      new Thread(thread).start();
  }
    completed.await();

    long endTime = System.currentTimeMillis();
    int requests = numSuccessfulRequests.get() + numUnSuccessfulRequests.get();
    long numRequests = requests;
    long totalTime = endTime - startTime;
    System.out.println("Task 2: ");
    System.out.println("Number of Requests:" + " " + Integer.toString(requests));
    System.out.println("Number of Successful Requests:" + " " + Integer.toString(numSuccessfulRequests.get()));
    System.out.println("Number of Unsuccessful Requests:" + " " + Integer.toString(numUnSuccessfulRequests.get()));
    System.out.println("Total Time:" + " " + String.valueOf(totalTime) + " " + "Milliseconds");
    System.out.println("Throughput:" + " " + ((double) numRequests/(endTime - startTime))*1000 + " " + "Number of requests/Second");
    writeToCsv(postCsv);
    calculateMetrics(postCsv, totalTime);
    System.out.println("Array to generate plot: " + generatePlotArray(endTimeData));
  }
}
