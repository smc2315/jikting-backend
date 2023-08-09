package com.cupid.jikting.jwt.filter;

import com.cupid.jikting.common.error.ApplicationError;
import com.cupid.jikting.common.error.BadRequestException;
import com.cupid.jikting.common.error.InvalidJwtException;
import com.cupid.jikting.common.error.JwtException;
import com.cupid.jikting.common.util.PasswordGenerator;
import com.cupid.jikting.jwt.service.JwtService;
import com.cupid.jikting.member.entity.Member;
import com.cupid.jikting.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationProcessingFilter extends OncePerRequestFilter {

    private static final String LOGIN_URI = "/login";
    private static final String OAUTH_LOGIN_URL = "/oauth";

    private final JwtService jwtService;
    private final MemberRepository memberRepository;
    private final GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (request.getRequestURI().contains(LOGIN_URI) || request.getRequestURI().contains(OAUTH_LOGIN_URL)) {
            filterChain.doFilter(request, response);
            return;
        }
        String refreshToken = jwtService.extractRefreshToken(request)
                .filter(jwtService::isTokenValid)
                .orElse(null);
        if (refreshToken != null) {
            checkRefreshTokenAndReIssueAccessToken(response, refreshToken);
            return;
        }
        saveAccessTokenAuthentication(request, response, filterChain);
    }

    public void checkRefreshTokenAndReIssueAccessToken(HttpServletResponse response, String refreshToken) {
        Member member = memberRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new InvalidJwtException(ApplicationError.INVALID_TOKEN));
        String reIssuedRefreshToken = reIssueRefreshToken(member);
        jwtService.sendAccessAndRefreshToken(response, jwtService.createAccessToken(member.getMemberProfileId()), reIssuedRefreshToken);
    }

    public void saveAccessTokenAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        log.info("saveAccessTokenAuthentication() 호출");
        Member member = memberRepository.findById(jwtService.extractMemberProfileId(request))
                .orElseThrow(() -> new JwtException(ApplicationError.UNAUTHORIZED_MEMBER));
        saveAuthentication(member);
        filterChain.doFilter(request, response);
    }

    public void saveAuthentication(Member member) {
        String password = member.getPassword();
        if (password == null && member.getSocialType() != null) {
            password = PasswordGenerator.generate();
        }
        if (password == null) {
            throw new BadRequestException(ApplicationError.BAD_MEMBER);
        }
        UserDetails userDetails = User.builder()
                .username(member.getId().toString())
                .password(password)
                .roles(member.getRole().name())
                .build();
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                userDetails, null, authoritiesMapper.mapAuthorities(userDetails.getAuthorities()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String reIssueRefreshToken(Member member) {
        String reIssuedRefreshToken = jwtService.createRefreshToken();
        member.updateRefreshToken(reIssuedRefreshToken);
        memberRepository.saveAndFlush(member);
        return reIssuedRefreshToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().equals(LOGIN_URI);
    }
}
