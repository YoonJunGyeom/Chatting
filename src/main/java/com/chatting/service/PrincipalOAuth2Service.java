package com.chatting.service;

import com.chatting.config.oauth.GoogleUserInfo;
import com.chatting.config.oauth.NaverUserInfo;
import com.chatting.config.oauth.OAuth2UserInfo;
import com.chatting.config.provider.CustomMemberDetails;
import com.chatting.domain.Member;
import com.chatting.domain.Role;
import com.chatting.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PrincipalOAuth2Service extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();  // 공급자 요청 확인

        OAuth2UserInfo oAuth2UserInfo;
        switch (provider) {
            case "google":
                oAuth2UserInfo = new GoogleUserInfo(oAuth2User.getAttributes());
                break;

            case "naver":
                oAuth2UserInfo = new NaverUserInfo((Map) oAuth2User.getAttributes().get("response"));
                break;

            default:
                throw new IllegalArgumentException("OAuth2Service 인증 오류 : " + provider);
        }

        Member member = memberRepository.findById(oAuth2UserInfo.getMemberId()).orElse(null);
        if (member == null) {
            member = Member.builder()
                    .provider(provider)
                    .memberId(oAuth2UserInfo.getMemberId())
                    .memberEmail(oAuth2UserInfo.getEmail())
                    .memberName(oAuth2UserInfo.getName())
                    .memberNickName(oAuth2UserInfo.getMemberNickName())
                    .memberRole(Role.USER.getAuth())
                    .build();
            memberRepository.save(member);
        }

        return new CustomMemberDetails(member, oAuth2User.getAttributes());
    }
}
