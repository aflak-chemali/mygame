import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class MasterMind {

  private static final int GUESS_TIMER_SECONDS = 30;
  private static final int GAME_DURATION_MINUTES = 2;

  public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in);

    // Ask the user if they know the game rules
    System.out.println("Do you know the rules of Mastermind? (yes/no)");
    String knowRules = scanner.nextLine().toLowerCase();

    if (knowRules.equals("no")) {
      displayGameRules();
    }

    // Ask the user to choose the opponent
    System.out.println("Choose your opponent:\n1. Computer\n2. Another user");
    int opponentChoice = scanner.nextInt();
    scanner.nextLine(); // Consume the newline character

    int[] secretCode;
    if (opponentChoice == 1) {
      secretCode = fetchSecretCode(); // Fetching secret code from API
    } else if (opponentChoice == 2) {
      System.out.println("Player 1, enter your secret code (4 numbers between 0 and 7 separated by spaces):");
      secretCode = getUserInputCode(scanner);
    } else {
      System.out.println("Invalid choice. Exiting the game.");
      return;
    }

    // Ask the user to choose the game level
    System.out.println("Choose a game level:\n1. With Timer\n2. Without Timer");
    int gameLevel = scanner.nextInt();
    scanner.nextLine(); // Consume the newline character

    int attempts = 10;
    boolean guessed = false;
    List<String> history = new ArrayList<>();

    Timer overallTimer = null;
    int remainingGameTime = GAME_DURATION_MINUTES * 60;
    if (gameLevel == 1) {
      // Set up a timer for the overall game duration
      overallTimer = new Timer();
      TimerTask overallTask = new TimerTask() {
        public void run() {
          System.out.println("\nGame over! You've exceeded the time limit.");
          scanner.close();
          System.exit(0);
        }
      };
      overallTimer.schedule(overallTask, remainingGameTime * 1000, 1000);
    }

    while (attempts > 0 && !guessed) {
      final Timer guessTimer;  // Declare guessTimer as final
      int remainingGuessTime = GUESS_TIMER_SECONDS;

      if (gameLevel == 1) {
        // Set up a timer to limit the time for each guess
        guessTimer = new Timer();
        TimerTask guessTask = new TimerTask() {
          public void run() {
            System.out.println("\nTime's up! Defaulting to guess: 0 0 0 0");
            guessTimer.cancel();
          }
        };
        guessTimer.schedule(guessTask, remainingGuessTime * 1000, 1000);
      } else {
        guessTimer = null;
      }

      System.out.println("Enter your guess (4 numbers between 0 and 7 separated by spaces):");
      String guessInput = scanner.nextLine();

      if (gameLevel == 1 && guessTimer != null) {
        guessTimer.cancel(); // Cancel the guess timer as the user has entered their guess
      }

      int[] guess;
      if (guessInput.trim().isEmpty()) {
        // If the user didn't input a guess, default to "0 0 0 0"
        guess = new int[]{0, 0, 0, 0};
        System.out.println("No guess provided. Defaulting to 0 0 0 0.");
      } else {
        guess = parseUserInputCode(guessInput, scanner);
      }

      String feedback = provideFeedback(secretCode, guess);
      history.add(Arrays.toString(guess) + " - " + feedback);
      System.out.println(feedback);

      if (feedback.equals("4 correct numbers and positions")) {
        guessed = true;
        System.out.println("Congratulations! You've guessed the code!");
      } else {
        attempts--;
        System.out.println("Attempts left: " + attempts);
      }

      if (gameLevel == 1) {
        remainingGameTime -= GUESS_TIMER_SECONDS; // Deduct guess time from remaining game time
      }
    }

    if (!guessed) {
      System.out.println("Sorry, you've run out of attempts. The secret code was: " + Arrays.toString(secretCode));
    }

    if (gameLevel == 1) {
      assert overallTimer != null;
      overallTimer.cancel(); // Cancel the overall game timer
    }

    System.out.println("Game Over. Here's your history:");
    for (String entry : history) {
      System.out.println(entry);
    }
  }
  // Method to fetch the secret code from Random Number Generator API
  public static int[] fetchSecretCode() {
    try {
      URL url = new URL("https://www.random.org/integers/?num=4&min=0&max=7&col=1&base=10&format=plain&rnd=new");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      int responseCode = connection.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        int[] secretCode = new int[4];
        int index = 0;
        while ((line = reader.readLine()) != null && index < 4) {
          secretCode[index] = Integer.parseInt(line.trim());
          index++;
        }
        reader.close();
        return secretCode;
      } else {
        System.out.println("Failed to get response from the API. Response code: " + responseCode);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new int[]{0, 0, 0, 0}; // Default code if fetching fails
  }

  // Method to get the secret code from the user
  public static int[] getUserInputCode(Scanner scanner) {
    int[] code = new int[4];
    String userInput = scanner.nextLine();
    return parseUserInputCode(userInput, scanner);
  }

  // Method to parse the user's input code
  public static int[] parseUserInputCode(String userInput, Scanner scanner) {
    String[] inputArray = userInput.split(" ");
    if (inputArray.length != 4) {
      System.out.println("Invalid input. Please enter 4 numbers.");
      return parseUserInputCode(scanner.nextLine(), scanner); // Retry input
    }

    int[] code = new int[4];
    try {
      for (int i = 0; i < 4; i++) {
        code[i] = Integer.parseInt(inputArray[i]);
        if (code[i] < 0 || code[i] > 7) {
          throw new NumberFormatException();
        }
      }
    } catch (NumberFormatException e) {
      System.out.println("Invalid input. Please enter numbers between 0 and 7.");
      return parseUserInputCode(scanner.nextLine(), scanner); // Retry input
    }

    return code;
  }

  // Method to provide feedback
  public static String provideFeedback(int[] secretCode, int[] guess) {
    int correctPositions = 0;
    int correctNumbers = 0;
    for (int i = 0; i < 4; i++) {
      if (guess[i] == secretCode[i]) {
        correctPositions++;
      } else {
        for (int j = 0; j < 4; j++) {
          if (guess[i] == secretCode[j]) {
            correctNumbers++;
            break;
          }
        }
      }
    }
    if (correctPositions == 4) {
      return "4 correct numbers and positions";
    } else {
      return correctNumbers + " correct number(s) but in the wrong position, and " +
          correctPositions + " correct number(s) and position(s)";
    }
  }
  // Method to display the game rules
  public static void displayGameRules() {
    System.out.println("\nMastermind Game Rules:");
    System.out.println("1. The secret code is a 4-digit combination of numbers between 0 and 7.");
    System.out.println("2. You have 10 attempts to guess the code.");
    System.out.println("3. After each guess, you will receive feedback on the correctness of your numbers and positions.");
    System.out.println("4. Feedback Format: 'X correct numbers and positions' or 'Y correct number(s) but in the wrong position, and Z correct number(s) and position(s)'.");
    System.out.println("5. The game has a time limit of 2 minutes. If you don't make a guess within 30 seconds, it defaults to '0 0 0 0'.");
    System.out.println("6. Good luck!\n");
  }

}