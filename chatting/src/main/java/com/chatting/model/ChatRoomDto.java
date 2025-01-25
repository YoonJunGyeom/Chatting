package com.chatting.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class ChatRoomDto {

    private String roomId;
    private String roomName;
    private int userCount;
    private int maxUserCnt;

    private String roomPwd;
    private boolean secretChk;

    private Map<String, String> userMap;


}
