package com.ryu.studyhelper.config;

import com.ryu.studyhelper.auth.GoogleOAuth2UserService;
import com.ryu.studyhelper.auth.OAuth2SuccessHandler;
import com.ryu.studyhelper.config.jwt.JwtAccessDeniedHandler;
import com.ryu.studyhelper.config.jwt.JwtAuthenticationEntryPoint;
import com.ryu.studyhelper.config.jwt.filter.JwtAuthenticationFilter;
import com.ryu.studyhelper.config.jwt.util.JwtUtil;
import com.ryu.studyhelper.config.security.PrincipalDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtUtil jwtUtil;
    private final GoogleOAuth2UserService googleOAuth2UserService;
    private final OAuth2SuccessHandler successHandler;
    private final PrincipalDetailsService principalDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // TODO: 리프레시토큰 CSRF 방어 고려

        // CSRF 비활성화 (JWT 사용으로 불필요)
        http.csrf(AbstractHttpConfigurer::disable);

        // Stateless 세션 정책 (JWT 사용)
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

        // 요청 권한 설정
        http.authorizeHttpRequests(auth -> auth
                // 정적 리소스
                .requestMatchers("/favicon.ico", "/error").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/assets/**", "/html/**").permitAll()

                // API 문서
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**").permitAll()

                // OAuth2 & 인증
                .requestMatchers("/oauth2/**").permitAll()
                .requestMatchers("/api/auth/refresh").permitAll()

                // 헬스체크 (인증 불필요)
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

//                // 공개 API (필요시 사용)
//                .requestMatchers("/api/public/**").permitAll()

                // Swagger 테스트용 - 모든 API 개방
//                .requestMatchers("/api/**").permitAll()

                .anyRequest().authenticated()
        );

        // HTTP Basic 인증 비활성화
        http.httpBasic(AbstractHttpConfigurer::disable);

        // CORS 설정 (WebConfig의 설정 사용)
        http.cors(Customizer.withDefaults());

        // 예외 처리
        http.exceptionHandling(exception -> exception
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                .accessDeniedHandler(new JwtAccessDeniedHandler())
        );

        // JWT 인증 필터 추가
        http.addFilterBefore(
                new JwtAuthenticationFilter(jwtUtil, principalDetailsService), 
                UsernamePasswordAuthenticationFilter.class
        );

        // OAuth2 로그인 설정
        http.oauth2Login(oauth -> oauth
                .userInfoEndpoint(u -> u.userService(googleOAuth2UserService))
                .successHandler(successHandler)
        );

        return http.build();
    }
}