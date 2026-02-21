let stompClient = null;
let currentRoomId = null;
let playerName = null;
let mySymbol = null;
let isMyTurn = false;
let timerInterval = null;

const screens = ['register-screen', 'menu-screen', 'lobby-screen', 'game-screen'];

function showScreen(screenId) {
    screens.forEach(id => {
        document.getElementById(id).classList.remove('active');
        document.getElementById(id).style.display = 'none';
    });
    document.getElementById(screenId).classList.add('active');
    document.getElementById(screenId).style.display = 'block';
}

function registerUser() {
    const nameInput = document.getElementById('display-name');
    if (!nameInput.value) return alert("Please enter a name");
    playerName = nameInput.value;

    fetch('/api/users/register', {
        method: 'POST',
        body: JSON.stringify(playerName),
        headers: { 'Content-Type': 'application/json' }
    }).then(res => res.json())
        .then(user => {
            document.getElementById('welcome-text').innerText = `Welcome, ${user.displayName}!`;
            showScreen('menu-screen');
            connectWS();
        });
}

function connectWS() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe('/user/topic/room', function (message) {
            const gameMessage = JSON.parse(message.body);
            if (gameMessage.type === 'START') { // Handle immediate start if matched
                subscribeToRoom(gameMessage.roomId);
                startGame(gameMessage.content);
            } else {
                handleRoomCreated(gameMessage.content);
            }
        });
        stompClient.subscribe('/user/topic/errors', function (message) {
            alert(JSON.parse(message.body).content);
        });
    });
}

function startSolo() {
    if (!stompClient) return;
    stompClient.send("/app/game.create", {}, JSON.stringify({
        sender: playerName,
        content: "AI"
    }));
}

function startWithFriend() {
    if (!stompClient) return;
    stompClient.send("/app/game.create", {}, JSON.stringify({
        sender: playerName,
        content: "FRIEND"
    }));
}

function quickMatch() {
    if (!stompClient) return;
    stompClient.send("/app/game.quickmatch", {}, JSON.stringify({
        sender: playerName
    }));
}

function joinRoomByCode() {
    const code = document.getElementById('join-room-id').value;
    if (!code) return alert("Enter code");
    subscribeToRoom(code);
    stompClient.send("/app/game.join", {}, JSON.stringify({
        roomId: code,
        sender: playerName
    }));
}

function handleRoomCreated(room) {
    currentRoomId = room.id;
    mySymbol = "X";
    subscribeToRoom(room.id); // Subscribe first
    if (room.mode === 'AI') {
        startGame(room);
    } else {
        document.getElementById('room-code').innerText = room.id;
        showScreen('lobby-screen');
    }
}

function subscribeToRoom(roomId) {
    stompClient.subscribe(`/topic/room/${roomId}`, function (message) {
        const gameMessage = JSON.parse(message.body);
        if (gameMessage.type === 'START' || gameMessage.type === 'UPDATE') {
            startGame(gameMessage.content);
        } else if (gameMessage.type === 'REACTION') {
            showReaction(gameMessage.content);
        } else if (gameMessage.type === 'REMATCH_REQUESTED') {
            if (gameMessage.sender !== playerName) {
                alert(`${gameMessage.sender} wants a rematch!`);
            }
        }
    });
}

function startGame(room) {
    currentRoomId = room.id;
    if (!mySymbol) mySymbol = "O"; // If we didn't create the room, we are O
    showScreen('game-screen');
    updateBoard(room);
}

function updateBoard(room) {
    const cells = document.querySelectorAll('.cell');
    room.board.forEach((val, i) => {
        cells[i].innerText = val || "";
        cells[i].className = 'cell' + (val ? ' ' + val.toLowerCase() : '');
    });

    const currentPlayer = room.players[room.turnIndex];
    isMyTurn = currentPlayer.name === playerName;

    document.getElementById('current-turn-status').innerText = isMyTurn ? "Your Turn" : `${currentPlayer.name}'s Turn`;
    document.getElementById('player-names').innerText = `${room.players[0].name} (X) vs ${room.players[1] ? room.players[1].name : 'AI'} (O)`;

    if (room.status === 'finished') {
        showWinModal(room.winner);
    } else {
        startTimer();
    }
}

function makeMove(index) {
    if (!isMyTurn) return;
    stompClient.send("/app/game.move", {}, JSON.stringify({
        roomId: currentRoomId,
        sender: playerName,
        content: index
    }));
}

function sendReaction(emoji) {
    stompClient.send("/app/game.reaction", {}, JSON.stringify({
        roomId: currentRoomId,
        sender: playerName,
        type: 'REACTION',
        content: emoji
    }));
}

function showReaction(emoji) {
    const div = document.getElementById('floating-reaction');
    div.innerText = emoji;
    setTimeout(() => div.innerText = "", 2000);
}

function startTimer() {
    clearInterval(timerInterval);
    let timeLeft = 30;
    const timerDiv = document.getElementById('turn-timer');
    timerDiv.innerText = `Time: ${timeLeft}s`;

    timerInterval = setInterval(() => {
        timeLeft--;
        timerDiv.innerText = `Time: ${timeLeft}s`;
        if (timeLeft <= 0) {
            clearInterval(timerInterval);
            // Handle timeout if needed
        }
    }, 1000);
}

function showWinModal(winner) {
    document.getElementById('win-modal').style.display = 'flex';
    document.getElementById('win-text').innerText = winner === 'draw' ? "It's a Draw!" : `${winner} Wins!`;
    clearInterval(timerInterval);
}

function requestRematch() {
    document.getElementById('win-modal').style.display = 'none';
    const statusDiv = document.getElementById('current-turn-status');
    statusDiv.innerText = "Waiting for opponent to join rematch...";
    stompClient.send("/app/game.rematch", {}, JSON.stringify({
        roomId: currentRoomId,
        sender: playerName
    }));
}

function backToMenu() {
    document.getElementById('win-modal').style.display = 'none';
    showScreen('menu-screen');
    // Ideally disconnect from room sub
}
