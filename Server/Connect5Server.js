/* Connect5Server Class
   This class is a Node.js implementation of server
   architecture. It handles the internal game logic
   and is used to handle requests from players based 
   on the input provided and the type of the request.
*/

//Can be adjusted to make the game board larger.
var width = 9;
var height = 5;

var http = require('http'); 
var port = 3000; 

var currentPlayer = 5;
var moveCounter = 0;

var globalResults = 4;

var piecesInPlay = new Array(width);
var playersInGame = new Array();

for (var i = 0; i < piecesInPlay.length; i++) {	
    piecesInPlay[i] = new Array();
} 

/* Name: requestHandler method
   Description: This handles all requests to the server and
   processes them before returning an output. Each kind of
   request is detailed in the relevant section.
*/
const requestHandler = (request, response) => {
  let body = '';
  var postData = new Array();
  
  //On receiving data from a client the server stores it for
  //later use. It is specifically used for post requests.
  request.on('data', chunk => {
	if (request.method == 'POST') {
        body += chunk.toString();
		var splitString = body.split("&");
		for (var i = 0; i < splitString.length;i++)
		{
			var paramData = splitString[i].split("=");
			postData[paramData[0]] = paramData[1];
		}	
		console.log("[POST REQUEST] RECEIVED: " + body);
	}
	else
	{
		console.log("[GET REQUEST] RECEIVED");
	}
  });

  //On finishing the process of a request to the server, this
  //section handles the return of data to the client and processing
  //of relevant information for POST requests. 
  request.on('end', () => {
	if (request.method == 'POST') {
		//This deals with the registering of new players. Only two are
		//allowed to join and the server reacts accordingly to the provided input.
		if (postData["query"] == "register")
		{
			var responseCode = 3;
			if (playersInGame.length < 2)
			{
				if (postData["name"] == undefined || playersInGame[0] == postData["name"])
				{
					responseCode = 2;
				}
				else
				{
					if (postData["name"].length === 0)
					{
						postData["name"] = "John";
						if (playersInGame[0] == postData["name"])
						{
							postData["name"] = "Doe";
						}
					}
					playersInGame.push(postData["name"]);
					if (playersInGame.length == 2)
					{
						currentPlayer = 0;
					}
					
					responseCode = 1;

				}
			}
			console.log("[POST REQUEST]     SENT: responseCode=" + responseCode + ",playersInGame=" + playersInGame.length +
				",name=" + postData["name"] + ",boardWidth=" + width + ",boardHeight=" + height + "\n");
			response.end("responseCode=" + responseCode + ",playersInGame=" + playersInGame.length + ",name=" + postData["name"] + 
				",boardWidth=" + width + ",boardHeight=" + height + "\n");
		}
		//This section handles when a new move is posted by the player. This method
		//is where all major game events are handled. After confirming the players
		//turn and move are legal, it adds the new move to the board and confirms the
		//win state. This processed information is then provided to the client.
		else if (postData["query"] == "newMove")
		{
			if (playersInGame.length >= 2)
			{
				var player = postData["name"];
				var newRow = parseInt(postData["row"]);
				var result = "4";
				if (player != playersInGame[currentPlayer]) {
					result = "-1";
				}
				else if (piecesInPlay[newRow] == undefined) {
					result = "-3";
				}
				else if (piecesInPlay[newRow].length >= height) {
					result = "-2";
				}
				else {
					piecesInPlay[newRow].push(currentPlayer);
					moveCounter++;
					if (checkWinCondition(newRow)) {
						result = currentPlayer.toString();
						globalResults = result;
					}
					else {
						currentPlayer++;
						if (currentPlayer > 1) {
							currentPlayer = 0;
						}
						if (moveCounter >= width*height) {
							result = "3";
							globalResults = result;
						}
					}
				}	
				var boardState = "";
				for (var i = 0; i < piecesInPlay.length; i++) {
					boardState +=  piecesInPlay[i].join("") + "-";
				}	
				console.log("[POST REQUEST]     SENT: responseCode=5" + ",winState=" + result + ",currentPlayer=" + playersInGame[currentPlayer] + 
					",opponent=" + playersInGame[1-currentPlayer] + ",boardState="  + boardState + ",width=" + width + ",height=" + height + "\n");	
				response.end("responseCode=5" + ",winState=" + result + ",currentPlayer=" + playersInGame[currentPlayer] + 
					",opponent=" + playersInGame[1-currentPlayer] + ",boardState="  + boardState + ",width=" + width + ",height=" + height + "\n");	
			}
			else
			{
				console.log("[POST REQUEST]     SENT: responseCode=4\n");
				response.end("responseCode=4\n");
			}				
		}
		else if (postData["query"] == "quit" && (postData["name"] == playersInGame[0] || postData["name"] == playersInGame[1]))
		{
			globalResults = 2;
			console.log("[POST REQUEST]     SENT: responseCode=5" + ",winState=2" + ",currentPlayer=" + playersInGame[currentPlayer] + 
				",opponent=" + playersInGame[1-currentPlayer] + ",boardState="  + boardState + ",width=" + width + ",height=" + height + "\n");
			response.end("responseCode=5" + ",winState=2" + ",currentPlayer=" + playersInGame[currentPlayer] + 
				",opponent=" + playersInGame[1-currentPlayer] + ",boardState="  + boardState + ",width=" + width + ",height=" + height + "\n");
		}
		else {
			console.log("SENT: responseCode=6");
			response.end("responseCode=6");
		}
	}
	//The sole GET request is used to allow the update of information in the client.
	//It basically returns the current state of the game including if it has been won.
	else {
		console.log("[GET REQUEST] RECEIVED:");
		var boardState = "";
		for (var i = 0; i < piecesInPlay.length; i++) {
			boardState +=  piecesInPlay[i].join("") + "-";
		}		
		console.log("[GET REQUEST]     SENT: responseCode=5" + ",winState=" + globalResults + ",currentPlayer=" + playersInGame[currentPlayer] + 
			",opponent=" + playersInGame[1-currentPlayer] + ",boardState="  + boardState + ",width=" + width + ",height=" + height + "\n");	
		response.end("responseCode=5" + ",winState=" + globalResults + ",currentPlayer=" + playersInGame[currentPlayer] + 
			",opponent=" + playersInGame[1-currentPlayer] + ",boardState="  + boardState + ",width=" + width + ",height=" + height + "\n");	
	}
  });
}

/* Name: checkWinCondition method
   Description: This is used to do a check based on the last move the player has made.
   Since the game logically should only be won based on the current move checks are
   only performed on the pieces surrounding the last placed piece in all directions
   except for above the placed piece due to its impossibility within the games 
   constraints. Options are eliminated as soon as the current line does not match and
   the result is returned to finish the process.
*/
function checkWinCondition(locationX) {	
	var locationY = piecesInPlay[locationX].length - 1;
	var checkList = [[1, 0, 0], [-1, 0, 0], [1, 1, 1], [-1, -1, 1], [-1, 1, 2], [1, -1, 2], [0, -1, 3]]
	var countDir = [0, 0, 0, 0];
	var range = 0;
	
	while (checkList.length > 0 && ++range < 5) {
		for (var i = checkList.length-1; i >= 0; i--) {
			var dir = checkList[i];
			var x = dir[0] * range + locationX;
			var y = dir[1] * range + locationY;
				
			if (piecesInPlay[x] == undefined || piecesInPlay[x][y] != currentPlayer) {
				checkList.splice(i, 1);
			}
			else if (++countDir[dir[2]] >= 4) {
				return true;
			}
		}
	}

	return false;
}

/* Name: server
   Description: This is the set up for the server on the current device. Reports if there
   are issues during this process.
*/
const server = http.createServer(requestHandler)
server.listen(port, (err) => {
  if (err) {
    return console.log('Failed to start server: ', err)
  }

  console.log(`server is listening on ${port}`)
})
