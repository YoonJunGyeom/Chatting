package com.chatting.controller;

import com.chatting.config.provider.CustomMemberDetails;
import com.chatting.model.ChatRoomDto;
import com.chatting.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRepository chatRepository;

    // 채팅 리스트 화면
    @GetMapping()
    public String viewRoomList(Model model, @AuthenticationPrincipal CustomMemberDetails memberDetails){
        model.addAttribute("roomList", chatRepository.findAllRoom());

        if (memberDetails != null) {
            model.addAttribute("user", memberDetails.getMember());
        }

        return "roomList";
    }

    // 채팅방 생성
    @PostMapping("/room/create")
    public String createRoom(@ModelAttribute("room") ChatRoomDto params,
                             RedirectAttributes rttr) {

        ChatRoomDto room = chatRepository.createChatRoom(params);
        log.info("CREATE Chat Room = {}", room);
        rttr.addFlashAttribute("roomName", room);
        return "redirect:/chat";
    }

    // 채팅방 입장 화면
    @GetMapping("/room")
    public String viewRoomDetail(@AuthenticationPrincipal CustomMemberDetails memberDetails,
                                 @RequestParam String roomId,Model model) {

        model.addAttribute("room", chatRepository.findRoomById(roomId));
        if (memberDetails != null) {
            model.addAttribute("member", memberDetails.getMember());
        }

        return "roomDetail";
    }

    // 채팅방 비밀번호 확인
    @PostMapping("/confirmPwd/{roomId}")
    @ResponseBody
    public boolean confirmPwd(@PathVariable String roomId, @RequestParam String roomPwd){
        // 넘어온 roomId 와 roomPwd 를 이용해서 비밀번호 찾기
        // 찾아서 입력받은 roomPwd 와 room pwd 와 비교해서 맞으면 true, 아니면  false
        return chatRepository.confirmPwd(roomId, roomPwd);
    }

    // 채팅방 삭제
    @GetMapping("/delRoom/{roomId}")
    public String delChatRoom(@PathVariable String roomId){
        // roomId 기준으로 chatRoomMap 에서 삭제, 해당 채팅룸 안에 있는 사진 삭제
        chatRepository.delChatRoom(roomId);
        return "redirect:/chat";
    }

    // 채팅방 최대인원 확인
    @GetMapping("/chkUserCnt/{roomId}")
    @ResponseBody
    public boolean checkUserCnt(@PathVariable String roomId){
        return chatRepository.checkUserCnt(roomId);
    }
}
