package com.chatting.repository.mappers;

import com.chatting.domain.Member;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface MemberMapper {
    void save(Member member);
    Optional<Member> findById(String memberId);
}
