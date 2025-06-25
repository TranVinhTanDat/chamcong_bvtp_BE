package com.hospital.attendance.Config;

import com.hospital.attendance.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;
    private final UserService userService;

    @Autowired
    public SecurityConfig(JwtRequestFilter jwtRequestFilter, @Lazy UserService userService) {
        this.jwtRequestFilter = jwtRequestFilter;
        this.userService = userService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(userService).passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/chamcong/checkin").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP")
                        .requestMatchers("/chamcong/{id}/trangthai").hasRole("ADMIN")
                        .requestMatchers("/chamcong/tonghopa/**").hasRole("NGUOITONGHOP")
                        .requestMatchers("/chamcong/lichsu").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP")
                        .requestMatchers("/loai-nghi").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP")
                        .requestMatchers("/nhanvien/**").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP")
                        .requestMatchers("/khoa-phong").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP")
                        .requestMatchers("/user/current").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP")
                        .requestMatchers("/user/current/password").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP")


                        .requestMatchers("/user/**").hasRole("ADMIN")

                        // API ADMIN Chấm công
                        .requestMatchers("/ky-hieu-cham-cong/paged").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP")
                        .requestMatchers("/ky-hieu-cham-cong/{id}").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP")
                        .requestMatchers(HttpMethod.POST, "/ky-hieu-cham-cong").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/ky-hieu-cham-cong/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/ky-hieu-cham-cong/{id}").hasRole("ADMIN")

                        // API Ca làm việc
                        .requestMatchers("/ca-lam-viec/paged").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP")
                        .requestMatchers("/ca-lam-viec/{id}").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP")
                        .requestMatchers(HttpMethod.POST, "/ca-lam-viec").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/ca-lam-viec/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/ca-lam-viec/{id}").hasRole("ADMIN")


                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.getWriter().write("{\"error\": \"Truy cập không được phép\"}");
                        })
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}