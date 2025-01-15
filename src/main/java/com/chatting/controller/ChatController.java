package com.chatting.controller;

import com.chatting.model.ChatDto;
import com.chatting.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatRepository chatRepository;

    /**
     * MessageMapping 을 통해 WebSocket 으로 들어오는 메시지를 발신 처리한다.
     * 클라이언트에서는 /pub/chat/message 로 요청,
     * 처리가 완료되면 /sub/chat/room/roomId 로 메시지 전송,
     * 해당 url 을 sub(구독)하고 있는 모든 클라이언트에게 메시지 전달
     */
    @MessageMapping("/chat/enterUser")
    public void enterUser(@Payload ChatDto chat, SimpMessageHeaderAccessor headerAccessor) {

        log.info("enterUser 요청 받음");

        // 채팅방 유저+1
        chatRepository.plusUserCnt(chat.getRoomId());

        // 채팅방에 유저 추가 및 UserUUID 반환
        String userId = chatRepository.addUser(chat.getRoomId(), chat.getSender());

        // 반환 결과를 socket session 에 userId 로 저장
        headerAccessor.getSessionAttributes().put("userId", userId);
        headerAccessor.getSessionAttributes().put("roomId", chat.getRoomId());

        chat.setMessage(chat.getSender() + " 님 입장!!");

        // 도착지점으로 온 객체를 Message 객체로 변환 후, 도착지점을 sub 하고 있는 모든 클라이언트에게 메세지 전달
        messagingTemplate.convertAndSend("/sub/chat/room?roomId=" + chat.getRoomId(), chat);
    }

    // 해당 유저
    @MessageMapping("/chat/sendMessage")
    public void sendMessage(@Payload ChatDto chat) {
        log.info("메세지 발행 요청 = {}", chat.getMessage());

        chat.setMessage(chat.getMessage());
        messagingTemplate.convertAndSend("/sub/chat/room?roomId=" + chat.getRoomId(), chat);
    }

    // 유저 퇴장 시에는 EventListener 을 통해서 유저 퇴장을 확인
    @EventListener
    public void webSocketDisconnectListener(SessionDisconnectEvent event) {
        log.info("DisConnEvent {}", event);

        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // stomp 세션에 있던 uuid 와 roomId 를 확인해서 채팅방 유저 리스트와 room 에서 해당 유저를 삭제
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        String roomId = (String) headerAccessor.getSessionAttributes().get("roomId");

        log.info("headAccessor {}", headerAccessor);

        // 채팅방 유저 -1
        chatRepository.minusUserCnt(roomId);

        // 채팅방 유저 리스트에서 UUID 유저 닉네임 조회 및 리스트에서 유저 삭제
        String userName = chatRepository.getUserName(roomId, userId);
        chatRepository.delUser(roomId, userId);

        if (userName != null) {
            log.info("User Disconnected : " + userName);

            ChatDto chat = ChatDto.builder()
                    .type(ChatDto.MessageType.LEAVE)
                    .sender(userName)
                    .message(userName + " 님 퇴장!!")
                    .build();

            messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, chat);
        }
    }

    // 채팅에 참여한 유저 리스트 반환
    @GetMapping("/chat/userlist")
    @ResponseBody
    public List<String> userList(String roomId) {
        log.info("유저리스트 확인 요청");
        return chatRepository.getUserList(roomId);
    }

    // 채팅에 참여한 유저 닉네임 중복 확인
    @GetMapping("/chat/username/check")
    @ResponseBody
    public String isDuplicatedName(@RequestParam("roomId") String roomId, @RequestParam("userName") String userName) {
        log.info("유저이름 중복체크");
        return chatRepository.isDuplicatedName(roomId, userName);
    }
}
