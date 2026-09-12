package com.mikedvl.rendering.config;

import com.mikedvl.rendering.model.User;
import com.mikedvl.rendering.model.UserLoginHistory;
import com.mikedvl.rendering.repository.UserLoginHistoryRepository;
import com.mikedvl.rendering.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final UserLoginHistoryRepository historyRepository;

    public LoginSuccessHandler(UserRepository userRepository, UserLoginHistoryRepository historyRepository) {
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // Βρίσκουμε ποιος χρήστης μόλις συνδέθηκε
        String username = authentication.getName();

        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            UserLoginHistory history = new UserLoginHistory();
            history.setUser(user);
            history.setLoginTime(LocalDateTime.now());
            history.setIpAddress(request.getRemoteAddr());
            history.setUserAgent(request.getHeader("User-Agent"));

            historyRepository.save(history);
        }

        // Ανακατεύθυνση στην αρχική σελίδα μετά από επιτυχημένο Login
        setDefaultTargetUrl("/");
        super.onAuthenticationSuccess(request, response, authentication);
    }
}