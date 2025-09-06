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
        // CSRF 설정
        http.csrf(AbstractHttpConfigurer::disable);  //CSRF 보호를 비활성화

        // 기본 설정인 Session 방식은 사용하지 않고 JWT 방식을 사용하기 위한 설정
        http.sessionManagement((sessionManagement) ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );


        // 요청 권한 설정: 특정 URL 패턴에 대한 접근 권한을 설정
        http.authorizeHttpRequests((authorizeHttpRequests) ->
                authorizeHttpRequests
                        .requestMatchers("/oauth2/authorization/**").permitAll() // 회원가입 요청 모두 접근 허가
                        .requestMatchers("/login/oauth2/code/**").permitAll() // 회원가입 요청 모두 접근 허가
                        .requestMatchers("/user/signup").permitAll() // 회원가입 요청 모두 접근 허가
                        .requestMatchers("/user/signin").permitAll() // 로그인 요청 모두 접근 허가
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**").permitAll()
                        .anyRequest().authenticated() // 그 외 모든 요청 인증처리
        );

        // HTTP Basic 인증을 비활성화하여 JWT 인증을 사용하도록 설정
        http.httpBasic(AbstractHttpConfigurer::disable);

        http.exceptionHandling(
                (httpSecurityExceptionHandlingConfigurer) ->
                        httpSecurityExceptionHandlingConfigurer
                                // 인증 실패
                                .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                                // 인가 실패
                                .accessDeniedHandler(new JwtAccessDeniedHandler()));

        http.addFilterBefore(
                new JwtAuthenticationFilter(jwtUtil, principalDetailsService), UsernamePasswordAuthenticationFilter.class
			);



        // 필터 관리: JWT 인증 필터와 권한 부여 필터를 필터 체인에 추가
//        http.addFilterBefore(jwtAuthorizationFilter(), JwtAuthenticationFilter.class);
//        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);



        // OAuth2 로그인 설정
        http.oauth2Login(oauth -> oauth
                .userInfoEndpoint(u -> u.userService(googleOAuth2UserService))
                .successHandler(successHandler)
        );


        return http.build();
    }


}