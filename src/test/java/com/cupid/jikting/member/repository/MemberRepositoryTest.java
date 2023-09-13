package com.cupid.jikting.member.repository;

import com.cupid.jikting.member.entity.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class MemberRepositoryTest {

    private static final String USERNAME = "username123";
    private static final String PASSWORD = "Password123!";
    private static final String NAME = "홍길동";
    private static final String PHONE = "전화번호";
    private static final String WRONG_PHONE = "잘못된 전화번호";

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void 회원_삭제_성공() {
        // given
        Member member = Member.builder()
                .username(USERNAME)
                .password(PASSWORD)
                .name(NAME)
                .phone(PHONE)
                .build();
        Member savedMember = memberRepository.save(member);
        // when
        memberRepository.delete(member);
        // then
        assertThat(memberRepository.findById(savedMember.getId())).isEmpty();
    }

    @Test
    void 전화번호로_회원_조회_성공() {
        // given
        Member member = Member.builder()
                .username(USERNAME)
                .password(PASSWORD)
                .name(NAME)
                .phone(PHONE)
                .build();
        memberRepository.save(member);
        // when & then
        assertDoesNotThrow(() -> memberRepository.findByPhone(PHONE));
    }
}
