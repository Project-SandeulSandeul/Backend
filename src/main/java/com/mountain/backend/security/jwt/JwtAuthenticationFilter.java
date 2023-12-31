package com.mountain.backend.security.jwt;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mountain.backend.user.entity.User;
import com.mountain.backend.user.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;
	private final UserRepository userRepository;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

		// JWT 토큰을 해석하여 추출
		String access_token = jwtUtil.resolveToken(request, JwtUtil.ACCESS_KEY);
		String refresh_token = jwtUtil.resolveToken(request, JwtUtil.REFRESH_KEY);

		// 토큰이 존재하면 유효성 검사를 수행하고, 유효하지 않은 경우 예외 처리
		if(access_token == null){
			filterChain.doFilter(request, response);
		} else {
			if (jwtUtil.validateToken(access_token)) {
				setAuthentication(jwtUtil.getUserInfoFromToken(access_token));
			} else if (refresh_token != null && jwtUtil.refreshTokenValid(refresh_token)) {
				//Refresh토큰으로 유저명 가져오기
				String memberId = jwtUtil.getUserInfoFromToken(refresh_token);
				//유저명으로 유저 정보 가져오기
				User user = userRepository.findByEmail(memberId).get();
				//새로운 ACCESS TOKEN 발급
				String newAccessToken = jwtUtil.createToken(memberId, "Access");
				//Header에 ACCESS TOKEN 추가
				jwtUtil.setHeaderAccessToken(response, newAccessToken);
				setAuthentication(memberId);
			} else if (refresh_token == null) {
				jwtExceptionHandler(response, "AccessToken Expired.", HttpStatus.BAD_REQUEST.value());
				return;
			} else {
				jwtExceptionHandler(response, "RefreshToken Expired.", HttpStatus.BAD_REQUEST.value());
				return;
			}
			// 다음 필터로 요청과 응답을 전달하여 필터 체인 계속 실행
			filterChain.doFilter(request, response);
		}
	}


	public void setAuthentication(String userId) {

		SecurityContext context = SecurityContextHolder.createEmptyContext();
		Authentication authentication = jwtUtil.createAuthentication(userId);
		context.setAuthentication(authentication);

		SecurityContextHolder.setContext(context);

	}


	public void jwtExceptionHandler(HttpServletResponse response, String msg, int statusCode) {

		response.setStatus(statusCode);
		response.setContentType("application/json");
		try {
			String json = new ObjectMapper().writeValueAsString(new SecurityExceptionDto(statusCode, msg));
			response.getWriter().write(json);
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

}

