package com.chatting.domain;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    private long memberSeq;

    // OAuth2 공급자
    private String provider;
    private String memberId;

    private String memberName;
    private String memberEmail;
    private String memberNickName;

    private String memberRole;

    private LocalDateTime joinDate;
}
