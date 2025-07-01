package com.hospital.attendance.Config;

import com.hospital.attendance.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;
    private final UserService userService;

    @Value("${VPS_HOST:localhost}")
    private String vpsHost;

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
                        // ========== PUBLIC ENDPOINTS ==========
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ========== CHẤM CÔNG APIs ==========
                        // Chấm công cơ bản - Tất cả roles (bao gồm NGUOITONGHOP_1KP)
                        .requestMatchers("/chamcong/checkin").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP", "NGUOITONGHOP_1KP")
                        .requestMatchers("/chamcong/checkin-bulk").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP", "NGUOITONGHOP_1KP")
                        .requestMatchers("/chamcong/lichsu").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP", "NGUOITONGHOP_1KP")
                        .requestMatchers("/chamcong/trangthai-ngay").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP", "NGUOITONGHOP_1KP")
                        .requestMatchers("/chamcong/chitiet-homnay").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP", "NGUOITONGHOP_1KP")

                        // Chấm công nâng cao - Chỉ ADMIN
                        .requestMatchers("/chamcong/{id}/trangthai").hasAnyRole("ADMIN", "NGUOICHAMCONG")
                        .requestMatchers("/chamcong/update-bulk").hasAnyRole("ADMIN", "NGUOICHAMCONG")
                        .requestMatchers("/chamcong/update-symbol").hasRole("ADMIN")

                        // Tổng hợp - Chỉ NGUOITONGHOP (NGUOITONGHOP_1KP KHÔNG có quyền này)
                        .requestMatchers("/chamcong/tonghopa/**").hasRole("NGUOITONGHOP")

                        // ========== KÝ HIỆU CHẤM CÔNG APIs ==========
                        // Xem - Tất cả roles (bao gồm NGUOITONGHOP_1KP)
                        .requestMatchers(HttpMethod.GET, "/ky-hieu-cham-cong").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP", "NGUOITONGHOP_1KP")
                        .requestMatchers("/ky-hieu-cham-cong/paged").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP", "NGUOITONGHOP_1KP")
                        .requestMatchers(HttpMethod.GET, "/ky-hieu-cham-cong/{id}").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP", "NGUOITONGHOP_1KP")

                        // Quản lý - Chỉ ADMIN
                        .requestMatchers(HttpMethod.POST, "/ky-hieu-cham-cong").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/ky-hieu-cham-cong/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/ky-hieu-cham-cong/{id}").hasRole("ADMIN")

                        // ========== CA LÀM VIỆC APIs ==========
                        // Xem - Tất cả roles (bao gồm NGUOITONGHOP_1KP)
                        .requestMatchers(HttpMethod.GET, "/ca-lam-viec").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP", "NGUOITONGHOP_1KP")
                        .requestMatchers("/ca-lam-viec/paged").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP", "NGUOITONGHOP_1KP")
                        .requestMatchers(HttpMethod.GET, "/ca-lam-viec/{id}").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP", "NGUOITONGHOP_1KP")

                        // Quản lý - Chỉ ADMIN
                        .requestMatchers(HttpMethod.POST, "/ca-lam-viec").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/ca-lam-viec/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/ca-lam-viec/{id}").hasRole("ADMIN")

                        // ========== NHÂN VIÊN & KHOA PHÒNG APIs ==========
                        .requestMatchers("/nhanvien/**").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP", "NGUOITONGHOP_1KP")
                        .requestMatchers("/khoa-phong").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP", "NGUOITONGHOP_1KP")
                        .requestMatchers("/loai-nghi").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP", "NGUOITONGHOP_1KP")

                        // ========== USER MANAGEMENT APIs ==========
                        // Profile cá nhân - Tất cả roles (bao gồm NGUOITONGHOP_1KP)
                        .requestMatchers("/user/current").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP", "NGUOITONGHOP_1KP")
                        .requestMatchers("/user/current/password").hasAnyRole("ADMIN", "NGUOICHAMCONG", "NGUOITONGHOP", "NGUOITONGHOP_1KP")

                        // Quản lý user - Chỉ ADMIN
                        .requestMatchers("/user/**").hasRole("ADMIN")

                        // ========== DEFAULT ==========
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

        // Dynamic CORS origins based on environment
        List<String> allowedOrigins = Arrays.asList(
                "http://localhost:3000",
                "http://localhost",
                "http://" + vpsHost,
                "https://" + vpsHost,
                "http://" + vpsHost + ":3000",
                "https://" + vpsHost + ":3000"
        );

        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}