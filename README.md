# SpringBoot WebSocket Chatting Project
---
## 1. 사용기술
- Java 11
- Spring Boot
- Spring Security
- AJAX
- jquery
- WebSocket & SocketJS
- Stomp
- MariaDB / MyBatis
---
# 2. 구동 화면
|소셜 로그인|방 생성 페이지|목록 페이지|
|:---:|:---:|:---:|
|<img width="300" height="400" alt="스크린샷 2023-05-25 오후 9 45 45" src="https://github.com/na1011/ChatForYou/assets/144922969/470af7ef-4ccc-492e-bb93-430fcb358798">|<img width="300" height="400" alt="스크린샷 2023-05-25 오후 9 52 31" src="https://github.com/na1011/ChatForYou/assets/144922969/efaea4af-858d-495f-a061-cfca14a371b7">|<img width="300" height="400" alt="스크린샷 2023-05-25 오후 9 47 24" src="https://github.com/na1011/ChatForYou/assets/144922969/1f3d7d9c-4794-4205-a653-42af2c4ce8ac">|
|방 설정 수정|채팅 화면|비밀번호 오류/인원 초과 시 입장제한|
|<img width="300" height="400" alt="스크린샷 2023-05-25 오후 9 47 31" src="https://github.com/na1011/ChatForYou/assets/144922969/54277b52-7e0d-4bb4-811f-ee3dfd86bea0">|<img width="300" height="400" alt="스크린샷 2023-05-25 오후 9 47 48" src="https://github.com/na1011/ChatForYou/assets/144922969/ecf7564d-1286-47cf-a380-42043e7c959d">|<img width="300" height="400" alt="스크린샷 2023-05-25 오후 9 50 05" src="https://github.com/na1011/ChatForYou/assets/144922969/00f733ea-1b26-4b17-b362-d3584c1cde4d">|
---
# 3. 핵심 코드
### 1) 유저 입장 시 메세지 발행 로직
```
function onConnected() {
    // 채팅방을 sub(구독)하는 코드
    stompClient.subscribe('/sub/chat/room?roomId=' + roomId, onMessageReceived);

    // /chat/enterUser 로 메시지를 pub(발행)
    stompClient.send("/pub/chat/enterUser",
        {},
        JSON.stringify({
            roomId: roomId,
            sender: userName,
            type: 'ENTER'
        })
    )
}

// MessageMapping 을 통해 WebSocket 으로 들어오는 메시지를 발신 처리
@MessageMapping("/chat/enterUser")
    public void enterUser(@Payload ChatDto chat, SimpMessageHeaderAccessor headerAccessor) {
        // 채팅방에 유저 추가 및 UserUUID 반환
        String userId = chatRepository.addUser(chat.getRoomId(), chat.getSender());

        // 반환 결과를 socket session 에 userId 로 저장
        headerAccessor.getSessionAttributes().put("userId", userId);
        headerAccessor.getSessionAttributes().put("roomId", chat.getRoomId());

        chat.setMessage(chat.getSender() + " 님 입장!!");

        // 도착지점으로 온 객체를 Message 객체로 변환 후, 도착지점을 sub 하고 있는 모든 클라이언트에게 메세지 전달
        messagingTemplate.convertAndSend("/sub/chat/room?roomId=" + chat.getRoomId(), chat);
    }
```

### 2) 채팅 메시지 수신 로직
```
function onMessageReceived(payload) {
    // JSON 형식으로 넘어온 메시지를 parse 해서 사용
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

    } else { // chatType 이 talk 라면 아래 내용
        messageElement.classList.add('chat-message');
        let userNameElement = document.createElement('span');
        let userNameText = document.createTextNode(chat.sender);
        userNameElement.appendChild(userNameText);
        messageElement.appendChild(userNameElement);
    }

    let textElement = document.createElement('p');
    let messageText = document.createTextNode(chat.message);
    textElement.appendChild(messageText);

    messageElement.appendChild(textElement);

    // 각각의 태그로 삽입된 messageElement 를 채팅창 영역에 삽입
    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;
}
```

---
# 4. 주요 기능 요약

## 기본 기능

### 1. 채팅방 관리
- **채팅방 생성**: 사용자의 채팅 방 생성 기능
- **중복 검사**: 이미 존재하는 채팅방 이름에 대한 중복 검사를 실행
- **닉네임 선택**: 중복된 닉네임이 있을 경우, 자동으로 임의의 숫자를 추가하여 유니크하게 조정
- **채팅방 입장 및 퇴장 관리**: 사용자의 채팅방 입장 및 퇴장 기능


### 2. 채팅 기능
- **메시지 전송/수신**: RestAPI를 기반으로 한 메시지 송수신 기능을 제공
- **유저 리스트 및 참여 인원 확인**: 채팅방 내 유저 리스트와 현재 참여 인원 수를 확인 가능


## 추가 기능

### 1. 보안 및 관리
- **채팅방 암호화**: 채팅방 생성 시 암호를 설정하여 채팅방 입장 보안 강화
- **채팅방 삭제 및 인원 설정**: 필요에 따라 채팅방을 삭제하거나 최대 인원 수를 설정 가능


## 2. 회원 기능
- **OAuth2 소셜 로그인**: 구글 및 네이버 소셜 로그인 연동
