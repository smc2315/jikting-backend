package com.cupid.jikting.team.service;

import com.cupid.jikting.common.entity.Personality;
import com.cupid.jikting.common.error.ApplicationError;
import com.cupid.jikting.common.error.BadRequestException;
import com.cupid.jikting.common.error.NotFoundException;
import com.cupid.jikting.common.error.WrongAccessException;
import com.cupid.jikting.common.repository.PersonalityRepository;
import com.cupid.jikting.common.util.TeamNameGenerator;
import com.cupid.jikting.member.entity.MemberProfile;
import com.cupid.jikting.member.repository.MemberProfileRepository;
import com.cupid.jikting.team.dto.TeamRegisterRequest;
import com.cupid.jikting.team.dto.TeamResponse;
import com.cupid.jikting.team.dto.TeamUpdateRequest;
import com.cupid.jikting.team.entity.Team;
import com.cupid.jikting.team.entity.TeamMember;
import com.cupid.jikting.team.entity.TeamPersonality;
import com.cupid.jikting.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Transactional
@Service
public class TeamService {

    private static final boolean LEADER = true;
    private static final String TEAM_INVITE_URL = "https://jikting.com/invite?team=";

    private final TeamRepository teamRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final PersonalityRepository personalityRepository;

    public void register(Long memberProfileId, TeamRegisterRequest teamRegisterRequest) {
        MemberProfile memberProfile = getMemberProfileById(memberProfileId);
        validateCertified(memberProfile);
        if (memberProfile.isInTeam()) {
            throw new BadRequestException(ApplicationError.ALREADY_IN_TEAM);
        }
        Team team = Team.builder()
                .name(getRandomTeamName())
                .description(teamRegisterRequest.getDescription())
                .memberCount(teamRegisterRequest.getMemberCount())
                .gender(memberProfile.getGender())
                .build();
        team.addTeamPersonalities(toTeamPersonalities(toPersonalities(teamRegisterRequest.getKeywords()), team));
        TeamMember.of(LEADER, team, memberProfile);
        teamRepository.save(team);
    }

    public void attend(Long teamId, Long memberProfileId) {
        MemberProfile memberProfile = getMemberProfileById(memberProfileId);
        TeamMember.of(!LEADER, getTeamById(teamId), memberProfile);
        memberProfileRepository.save(memberProfile);
    }

    @Transactional(readOnly = true)
    public TeamResponse get(Long memberProfileId) {
        Team team = getMemberProfileById(memberProfileId).getTeam();
        return TeamResponse.of(team, TEAM_INVITE_URL + team.getId());
    }

    public void update(Long teamId, TeamUpdateRequest teamUpdateRequest) {
        Team team = getTeamById(teamId);
        team.update(teamUpdateRequest.getDescription(), toTeamPersonalities(toPersonalities(teamUpdateRequest.getKeywords()), team));
        teamRepository.save(team);
    }

    public void delete(Long teamId) {
        teamRepository.delete(getTeamById(teamId));
    }

    public void deleteMember(Long teamId, Long memberProfileId) {
    }

    private MemberProfile getMemberProfileById(Long memberProfileId) {
        return memberProfileRepository.findById(memberProfileId)
                .orElseThrow(() -> new NotFoundException(ApplicationError.MEMBER_NOT_FOUND));
    }

    private void validateCertified(MemberProfile memberProfile) {
        if (memberProfile.getMember().isNotCertifiedCompany()) {
            throw new WrongAccessException(ApplicationError.UNCERTIFIED_MEMBER);
        }
    }

    private String getRandomTeamName() {
        String randomTeamName = TeamNameGenerator.generate();
        while (teamRepository.existsByName(randomTeamName)) {
            randomTeamName = TeamNameGenerator.generate();
        }
        return randomTeamName;
    }

    private List<TeamPersonality> toTeamPersonalities(List<Personality> personalities, Team team) {
        return personalities.stream()
                .map(personality -> toTeamPersonality(personality, team))
                .collect(Collectors.toList());
    }

    private TeamPersonality toTeamPersonality(Personality personality, Team team) {
        return TeamPersonality.builder()
                .team(team)
                .personality(personality)
                .build();
    }

    private List<Personality> toPersonalities(List<String> keywords) {
        return keywords.stream()
                .map(this::getPersonalityByKeyword)
                .collect(Collectors.toList());
    }

    private Personality getPersonalityByKeyword(String keyword) {
        return personalityRepository.findByKeyword(keyword)
                .orElseThrow(() -> new NotFoundException(ApplicationError.PERSONALITY_NOT_FOUND));
    }

    private Team getTeamById(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException(ApplicationError.TEAM_NOT_FOUND));
    }
}
