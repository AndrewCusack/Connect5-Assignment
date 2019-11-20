import java.io.*;
import java.net.*; 
import java.util.*;

/* Connect5Client Class
   This class handles client functionality for the game. It
   works via http to handle requsts to the server for updates
   on gamestate. It is intended to only act as the user interface
   for the project.
*/
public class Connect5Client
{ 
	boolean myTurn = false;
	boolean gameOver = false;
	String name = "";
	
	/* Name: main method
	   Input: String[]
	   Output: void
	   Description: The main method in this project gives the player
	   an opportunity to log their credentials into the game and give 
	   them access to the game if it is available.
    */
	public static void main(String[] args) { 
		System.out.println("--------------------------------------");
		System.out.println("|         Welcome to Connect5         |");
		System.out.println("--------------------------------------\n");
		System.out.print("Enter your username: ");
		Scanner input = new Scanner(System.in);
		String name = input.nextLine();
		
		String[][] requestParams = {{"query", "register"},{"name", name}};
		Map<String, String> response = httpPost(requestParams);

		int responseCode = parseResponseCode(response.get("responseCode"));
		if (responseCode == 1) {
			name = response.get("name");
			if (Integer.parseInt(response.get("playersInGame")) == 1) {
				System.out.println("You are registered, welcome " + name + "!\nPlease wait for your opponent to join.");
			}
			else {
				System.out.println("You are registered, welcome " + name + "!\nPlease wait for your opponent to start.");
			}
			new Connect5Client().gameStart(name);
		}
		else if (responseCode == 3) {
			System.out.println("Sorry, you can't join because the game is full.");
		}
		else if (responseCode == 2) {
			System.out.println("Duplicate or invalid name detected, please use a different name.");
		}
		else
		{
			System.out.println("Server did not validate the request, please try again!");
		}
	} 
	
	/* Name: gameStart method
	   Input: String
	   Output: void
	   Description: This method controls the core gameplay loop. It features a
	   thread which handles refresh requests and allows the user to submit requests
	   to post new moves to the server and interpert their response.
    */
	private void gameStart(String name) {
		this.name = name;
		
		Thread t1 = new Thread( new Runnable() {
			@Override
			public void run() {
				try {
					while (!gameOver) {
						if (!myTurn) {
							Map<String, String> response = httpGet();
							if (response.get("currentPlayer").equals(name)) {
								printBoardFromResponse(response);
								System.out.print(name + ", please enter row [0-" + (Integer.parseInt(response.get("width"))-1) + "]: "); 
								myTurn = true;
							}
							switch (Integer.parseInt(response.get("winState"))) {
								case 0: case 1: 
									printBoardFromResponse(response); 
									System.out.println("Unfortunately, " + response.get("opponent") + " has won! Better luck next time. Press enter to quit."); 
									gameOver = true; 
								break;
								case 2: 
									System.out.println(response.get("currentPlayer") + " has quit the game, terminating the session. Press enter to quit."); 
									gameOver = true;
								break;
								case 3: 
									printBoardFromResponse(response); 
									System.out.println("The game is a draw! Press enter to quit."); 
									gameOver = true; 
								break;
								default: ;break;
							}
						}
						Thread.sleep(4000);
					}
				}
				catch (Exception e)
				{ System.out.println("Error in thread: " + e.toString()); }
			}
		});
		t1.start();
			
		Scanner input = new Scanner(System.in);
		String sentMessage = "";
		
		while (!gameOver) {
			sentMessage = input.nextLine();
			if (sentMessage.equals("exit")) {
				String[][] requestParams = {{"query", "quit"},{"name", name}};
				Map<String, String> response = httpPost(requestParams);
				gameOver = true;
			}
			else if (!gameOver){
				String[][] requestParams = {{"query", "newMove"},{"name", name},{"row", sentMessage}};
				Map<String, String> response = httpPost(requestParams);
	
				int responseCode = parseResponseCode(response.get("responseCode"));
				if (responseCode == 5) {
					switch (Integer.parseInt(response.get("winState"))) {
						case -3: 
							System.out.println("Invalid input detected, please only enter numbers within the scope of the board."); 
							System.out.print("Please enter row [0-" + (Integer.parseInt(response.get("width"))-1) + "]: ");
						break;
						case -2: 
							System.out.println("Invalid move made, please try a different one."); 
							System.out.print("Please enter row [0-" + (Integer.parseInt(response.get("width"))-1) + "]: ");
						break;
						case -1: 
							System.out.println("It is not your turn. Please wait for " + response.get("currentPlayer") + " to take their turn.") ;
						break;
						case 0: case 1: 
							printBoardFromResponse(response); 
							System.out.println("You have won the game congratulations! Press enter to quit."); 
							gameOver = true;
							input.nextLine();
						break;
						case 3: 
							printBoardFromResponse(response); 
							System.out.println("The game is a draw! Press enter to quit."); 
							gameOver = true;
							input.nextLine();
						break;
						default: 
							printBoardFromResponse(response); 
							System.out.println("Board has been refreshed. Please wait for " + response.get("currentPlayer") + "..."); 
							myTurn = false;
						break;
					}
				}
				else if (responseCode == 4)
				{
					System.out.println("Please wait for an opponent to join!");
				}
				else
				{
					System.out.println("Server did not validate the request, please try again!");
				}
			}
		}			
	}
	
	/* Name: printBoardFromResponse method
	   Input: Map<String, String>
	   Output: void
	   Description: This method prints out the status of the board recieved from
	   the main server. This is also used to refresh the player view as well. 
    */	
	private static void printBoardFromResponse(Map<String, String> response) {
		String printString = "";
		String [] result = response.get("boardState").split("-", -1);
		int width = Integer.parseInt(response.get("width")), height = Integer.parseInt(response.get("height"));
		for (int j = height - 1; j >= 0; j--) {
			for (int i = 0; i < width; i++) {
				if (j >= result[i].length()) {
					printString += "[ ]";
				}
				else if (result[i].substring(j, j+1).equals("0")) {
					printString += "[O]";
				}
				else {
					printString += "[X]";
				}
			}
			printString += "\n";
		}
		System.out.println(printString);
	}

	/* Name: httpGet method
	   Input: void
	   Output: Map<String, String>
	   Description: This method is used to perform the HTTP requests that GET data from 
	   the server. It subimts and returns the response that was gotten from the request.
    */
	private static Map<String, String> httpGet() {
		Map<String, String> response = new HashMap<String, String>(); 
		try {
			URL url = new URL("http://localhost:3000");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			try (BufferedReader in = new BufferedReader(
				new InputStreamReader(connection.getInputStream()))) {
				String line;
				while ((line = in.readLine()) != null) {
					String[] initialResponse = line.split(",");
					for (int i = 0; i < initialResponse.length; i++) {
						String[] parsedResponse = initialResponse[i].split("=");
						response.put(parsedResponse[0], parsedResponse[1]);
					}
				}
			} 
			finally {
				connection.disconnect();
			}
		}
		catch (Exception e) {
			System.out.println("Error during request: " + e.toString());
			response.put("responseCode", "7"); 
		}
		if (response.get("responseCode") == null)
		{
			response.put("responseCode", "7");
		}
		return response;
	}
	
	/* Name: httpPost method
	   Input: String[][]
	   Output: Map<String, String>
	   Description: This method is used to perform the HTTP requests that POST to 
	   the server. It prepares the parameters for the query and returns the response
	   that was gotten from the request.
    */	
	private static Map<String, String> httpPost(String[][] parameters) {
		Map<String, String> response = new HashMap<String, String>(); 
		try {
			URL url = new URL("http://localhost:3000");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");

			Map<String, String> params = new HashMap<String, String>(); 
			for (int i = 0; i < parameters.length; i++) {
				params.put(parameters[i][0], parameters[i][1]);
			}

			StringBuilder postData = new StringBuilder();
			for (Map.Entry<String, String> param : params.entrySet()) {
				if (postData.length() != 0) {
					postData.append('&');
				}
				postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
				postData.append('=');
				postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
			}
			byte[] postDataBytes = postData.toString().getBytes("UTF-8");
			connection.setDoOutput(true);
			try (DataOutputStream writer = new DataOutputStream(connection.getOutputStream())) {
				writer.write(postDataBytes);
				writer.flush();

				try (BufferedReader in = new BufferedReader(
						new InputStreamReader(connection.getInputStream()))) {
					String line;
					while ((line = in.readLine()) != null) {
						String[] initialResponse = line.split(",");
						for (int i = 0; i < initialResponse.length; i++) {
							String[] parsedResponse = initialResponse[i].split("=");
							response.put(parsedResponse[0], parsedResponse[1]);
						}
					}
				}
			}
			finally {
				connection.disconnect();
			}
		}
		catch (Exception e) {
			System.out.println("Error during request: " + e.toString());
			response.put("responseCode", "7"); 
		}
		if (response.get("responseCode") == null)
		{
			response.put("responseCode", "7");
		}
		return response;
	}
	
	/* Name: parseResponseCode method
	   Input: String
	   Output: int
	   Description: This method is used to ensure a valid response code was recieved
	   from the server.
    */		
	private static int parseResponseCode(String responseCode) {
		int value;
		try {
			value = Integer.parseInt(responseCode);
		}
		catch (Exception e) {
			value = 7;
		}
		return value;
	}
}