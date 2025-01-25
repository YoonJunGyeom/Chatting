'use strict';

document.write("<script\n" +
    "  src=\"https://code.jquery.com/jquery-3.6.1.min.js\"\n" +
    "  integrity=\"sha256-o88AwQnZB+VDvE9tvIXrMQaPlFFSUTR+nldQm1LuPXQ=\"\n" +
    "  crossorigin=\"anonymous\"></script>")

let userNamePage = document.querySelector('#userName-page');
let chatPage = document.querySelector('#chat-page');
let userNameForm = document.querySelector('#userNameForm');
let messageForm = document.querySelector('#messageForm');
let messageInput = document.querySelector('#message');
let messageArea = document.querySelector('#messageArea');
let connectingElement = document.querySelector('.connecting');

let stompClient = null;
let userName = null;

let colors = [
    '#2196F3', '#32c787', '#00BCD4', '#ff5652',
    '#ffc107', '#ff85af', '#FF9800', '#39bbb0'
];

// roomId 파라미터 가져오기
const url = new URL(location.href).searchParams;
const roomId = url.get('roomId');

function connect(event) {
    userName = document.querySelector('#name').value.trim();

    // userName 중복 확인
    isDuplicateName();

    // userNamePage 에 hidden 속성 추가해서 가리고
    // chatPage 를 등장시킴
    userNamePage.classList.add('invisible');
    chatPage.classList.remove('invisible');

    // 연결하고자하는 Socket 의 endPoint
    let socket = new SockJS('/ws-stomp');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, onConnected, onError);


    event.preventDefault();

}

function onConnected() {

    // sub 할 url => /sub/chat/room/roomId 로 구독한다
    console.log("roomId 구독 : " + roomId);
    stompClient.subscribe('/sub/chat/room?roomId=' + roomId, onMessageReceived);

    // 서버에 userName 을 가진 유저가 들어왔다는 것을 알림
    // /pub/chat/enterUser 로 메시지를 보냄
    console.log("유저 입장 json 보냄");
    stompClient.send("/pub/chat/enterUser",
        {},
        JSON.stringify({
            roomId: roomId,
            sender: userName,
            type: 'ENTER'
        })
    )

    connectingElement.classList.add('invisible');

}

// 유저 닉네임 중복 확인
function isDuplicateName() {

    $.ajax({
        type: "GET",
        url: "/chat/username/check",
        data: {
            "userName": userName,
            "roomId": roomId
        },
        success: function (data) {
            console.log("유저 아이디 중복체크 : " + data);

            userName = data;
        }
    })

}

// 유저 리스트 받기
// ajax 로 유저 리스를 받으며 클라이언트가 입장/퇴장 했다는 문구가 나왔을 때마다 실행된다.
function getUserList() {

    const userList = $("#userList");

    $.ajax({
        type: "GET",
        url: "/chat/userlist",
        data: {
            "roomId": roomId
        },
        success: function (data) {
            let users = "";
            for (let i = 0; i < data.length; i++) {
                users += "<li class='dropdown-item'>" + data[i] + "</li>"
            }
            console.log('유저 리스트 받기 : ' + users);
            userList.html(users);
        }
    })
}


function onError(error) {
    connectingElement.textContent = 'Could not connect to WebSocket server. Please refresh this page to try again!';
    connectingElement.style.color = 'red';
}

// 메시지 전송때는 JSON 형식을 메시지를 전달한다.
function sendMessage(event) {
    let messageContent = messageInput.value.trim();

    if (messageContent && stompClient) {
        let chatMessage = {
            "roomId": roomId,
            sender: userName,
            message: messageInput.value,
            type: 'TALK'
        };
        console.log("메세지 발행 : " + JSON.stringify(chatMessage));

        stompClient.send("/pub/chat/sendMessage", {}, JSON.stringify(chatMessage));
        messageInput.value = '';
    }
    event.preventDefault();
}

// 메시지를 받을 때도 마찬가지로 JSON 타입으로 받으며,
// 넘어온 JSON 형식의 메시지를 parse 해서 사용한다.
function onMessageReceived(payload) {

    let chat = JSON.parse(payload.body);

    let messageElement = document.createElement('li');

    if (chat.type === 'ENTER') {  // chatType 이 enter 라면 아래 내용
        messageElement.classList.add('event-message');
        chat.content = chat.sender + chat.message;
        getUserList();

    } else if (chat.type === 'LEAVE') { // chatType 가 leave 라면 아래 내용
        messageElement.classList.add('event-message');
        chat.content = chat.sender + chat.message;
        getUserList();

    } else { // chatType 이 talk 라면 아래 내용용
        messageElement.classList.add('chat-message');

        let avatarElement = document.createElement('i');
        let avatarText = document.createTextNode(chat.sender[0]);
        avatarElement.appendChild(avatarText);
        avatarElement.style['background-color'] = getAvatarColor(chat.sender);

        messageElement.appendChild(avatarElement);

        let userNameElement = document.createElement('span');
        let userNameText = document.createTextNode(chat.sender);
        userNameElement.appendChild(userNameText);
        messageElement.appendChild(userNameElement);
    }

    let textElement = document.createElement('p');
    let messageText = document.createTextNode(chat.message);
    textElement.appendChild(messageText);

    messageElement.appendChild(textElement);

    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;
}


function getAvatarColor(messageSender) {
    let hash = 0;
    for (let i = 0; i < messageSender.length; i++) {
        hash = 31 * hash + messageSender.charCodeAt(i);
    }

    let index = Math.abs(hash % colors.length);
    return colors[index];
}

userNameForm.addEventListener('submit', connect, true)
messageForm.addEventListener('submit', sendMessage, true)