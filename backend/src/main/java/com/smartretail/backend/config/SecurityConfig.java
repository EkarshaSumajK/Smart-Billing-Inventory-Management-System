package com.smartretail.backend.config;

import com.smartretail.backend.security.CustomOAuth2AuthenticationSuccessHandler;
import com.smartretail.backend.security.JwtAuthenticationFilter;
import com.smartretail.backend.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserService userService;
    private final CustomOAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
            @Lazy UserService userService,
            CustomOAuth2AuthenticationSuccessHandler oAuth2SuccessHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userService = userService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/api/auth/**", "/error", "/static/**", "/", "/index.html", "/favicon.ico",
                                "/login/oauth2/**", "/oauth2/**")
                        .permitAll()
                        .requestMatchers("/api/bills/{billId}/pdf").permitAll()
                        .requestMatchers("/api/bills/suggestions").permitAll() // ✅ ALLOW REC ENGINE

                        // Allow anyone to GET product images
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/products/image/**").permitAll()

                        // This rule now correctly secures the other product endpoints
                        .requestMatchers("/api/products/**", "/api/bills/**").hasAnyRole("MANAGER", "OWNER")

                        // --- ADD THIS LINE ---
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/customers/send-email")
                        .hasAnyRole("MANAGER", "OWNER")

                        .requestMatchers("/api/customers/**").hasAnyRole("MANAGER", "CASHIER", "OWNER")
                        .requestMatchers("/api/reports/**").hasAnyRole("MANAGER", "OWNER")
                        .requestMatchers("/api/users/**").hasRole("OWNER")
                        .anyRequest().authenticated())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                // --- OAUTH2 LOGIN CONFIG ---
                .oauth2Login(oauth2 -> {
                    oauth2.successHandler(oAuth2SuccessHandler);
                });

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("https://smart-billing-inventory-management.vercel.app",
                "http://localhost:3000", "http://localhost:5173", "http://localhost:8080"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept-Language"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}