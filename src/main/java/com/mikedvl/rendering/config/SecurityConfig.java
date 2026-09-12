package com.mikedvl.rendering.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final LoginSuccessHandler loginSuccessHandler;

    public SecurityConfig(LoginSuccessHandler loginSuccessHandler) {
        this.loginSuccessHandler = loginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Απενεργοποίηση προστασίας CSRF για τα APIs (Zebra & Dispatch)
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/zebra/**", "/api/dispatch/**"))

                // 2. Κανόνες πρόσβασης στις διαδρομές
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        .requestMatchers("/api/zebra/**", "/api/dispatch/**").permitAll()
                        // ΜΟΝΟ ο ADMIN έχει πρόσβαση στη διαχείριση χρηστών
                        .requestMatchers("/users/**").hasAuthority("ADMIN")
                        .anyRequest().authenticated()
                )

                // 3. Φόρμα Login με ενεργό τον Success Handler για το ιστορικό συνδέσεων
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(loginSuccessHandler)
                        .permitAll()
                )

                // 4. Logout ρύθμιση
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    // 5. ΑΠΑΡΑΙΤΗΤΟ BEAN: Ο κρυπτογράφος κωδικών για το UserService
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}