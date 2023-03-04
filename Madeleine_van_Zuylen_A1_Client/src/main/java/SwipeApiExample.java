import io.swagger.client.*;
import io.swagger.client.auth.*;
import io.swagger.client.model.*;
import io.swagger.client.api.SwipeApi;

import java.io.File;
import java.util.*;

public class SwipeApiExample {

  public static void main(String[] args) {

    SwipeApi apiInstance = new SwipeApi();
    SwipeDetails body = new SwipeDetails(); // SwipeDetails | response details
    String leftorright = "leftorright_example"; // String | Ilike or dislike user
    try {
      apiInstance.swipe(body, leftorright);
    } catch (ApiException e) {
      System.err.println("Exception when calling SwipeApi#swipe");
      e.printStackTrace();
    }
  }
}