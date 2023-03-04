import io.swagger.client.ApiException;
import io.swagger.client.api.SwipeApi;
import io.swagger.client.model.SwipeDetails;

public class DataGeneration {

  public static String getRandomNumber(int min, int max) {
    return  Integer.toString((int) (Math.random() * (max + 1 - min)) + min);
  }

  public static String getRandomString() {
    int stringSize = 256;
    String alphaNum = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvxyz";
    StringBuilder sb = new StringBuilder(stringSize);

    for (int i = 0; i < stringSize; i++) {
      int index = (int)(alphaNum.length() * Math.random());
      sb.append(alphaNum.charAt(index));
    }
    return sb.toString();
  }

  public static String getSwipe(int num) {
    if (num == 0) {
      return "left";
    } else if (num == 1){
      return "right";
    } else {
      return "error";
    }
  }

  public static String[] getValues() {
    int left = 0;
    int right = 1;
    int min = 1;
    int maxSwiper = 5000;
    int maxSwipee = 1000000;

    String swipe = getSwipe(Integer.parseInt(getRandomNumber(left, right)));
    String swiper = getRandomNumber(min, maxSwiper);
    String swipee = getRandomNumber(min, maxSwipee);
    String comment =getRandomString();

    String[] values = {swipe, swiper, swipee, comment};
    return values;
  }
}
